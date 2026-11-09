package com.cumulocity.agent.packaging.microservice.buildx;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Temporary docker CLI configuration carrying explicitly configured registry credentials.
 * <p>
 * BuildKit asks the docker CLI for registry credentials when it pulls images referenced by {@code FROM}. The CLI has no
 * option for credentials, it reads them from {@code config.json} in its configuration directory. To pass credentials
 * configured in the plugin without touching the user's configuration, a copy of the user's {@code config.json} with an
 * additional {@code auths} entry is written to a private temporary directory, which is passed with
 * {@code docker --config} and deleted on {@link #close()}.
 */
@Slf4j(topic = "BuildxClient")
public final class CredentialsConfig implements AutoCloseable {

    public static final String DOCKER_CONFIG = "DOCKER_CONFIG";
    public static final String DOCKER_HOST = "DOCKER_HOST";
    public static final String DOCKER_HUB = "https://index.docker.io/v1/";

    private static final Set<String> DOCKER_HUB_ALIASES = new HashSet<>(Arrays.asList(
            "docker.io", "index.docker.io", "registry-1.docker.io"));

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final Path directory;
    private final String dockerHost;

    private CredentialsConfig(Path directory, String dockerHost) {
        this.directory = directory;
        this.dockerHost = dockerHost;
    }

    /**
     * @param registryHost      normalized registry host (see {@link #registryHost(String, String)})
     * @param user              registry user
     * @param password          registry password
     * @param userDockerConfig  the user's docker configuration directory
     * @param dockerHost        daemon address of the user's active docker context; the temporary configuration has no
     *                          contexts, so the address is passed with {@code docker --host}; may be {@code null}
     */
    public static CredentialsConfig create(String registryHost, String user, String password,
                                           File userDockerConfig, String dockerHost) throws IOException {
        Path directory = createPrivateDirectory();
        try {
            ObjectNode config = readConfig(new File(userDockerConfig, "config.json"));

            // credsStore would take precedence over auths for every registry
            if (config.has("credsStore")) {
                log.debug("Ignoring credsStore '{}' while explicit registry credentials are used", config.get("credsStore"));
                config.remove("credsStore");
            }
            // a credential helper for the same registry would take precedence over the configured credentials
            JsonNode credHelpers = config.get("credHelpers");
            if (credHelpers instanceof ObjectNode) {
                ((ObjectNode) credHelpers).remove(registryHost);
            }
            // contexts are stored next to config.json and are not copied; the daemon address is passed with --host
            config.remove("currentContext");

            ObjectNode auths = config.has("auths") && config.get("auths") instanceof ObjectNode
                    ? (ObjectNode) config.get("auths") : config.putObject("auths");
            String auth = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
            auths.putObject(registryHost).put("auth", auth);

            ArrayNode pluginDirs = config.has("cliPluginsExtraDirs") && config.get("cliPluginsExtraDirs") instanceof ArrayNode
                    ? (ArrayNode) config.get("cliPluginsExtraDirs") : config.putArray("cliPluginsExtraDirs");
            pluginDirs.add(new File(userDockerConfig, "cli-plugins").getAbsolutePath());

            Path configFile = directory.resolve("config.json");
            Files.createFile(configFile);
            setPermissions(configFile, "rw-------");
            Files.write(configFile, MAPPER.writeValueAsBytes(config));

            log.info("Using configured credentials for registry {}", registryHost);
            return new CredentialsConfig(directory, dockerHost);
        } catch (IOException | RuntimeException e) {
            FileUtils.deleteQuietly(directory.toFile());
            throw e;
        }
    }

    /**
     * @return the temporary docker configuration directory, for {@code docker --config}
     */
    public File configDir() {
        return directory.toFile();
    }

    /**
     * @return the daemon address for {@code docker --host}, may be {@code null}
     */
    public String dockerHost() {
        return dockerHost;
    }

    @Override
    public void close() {
        FileUtils.deleteQuietly(directory.toFile());
    }

    /**
     * Returns the key under which docker stores credentials of the registry: the registry host (with port), or the
     * Docker Hub address. The registry is taken from {@code registryUrl} when set, otherwise from {@code image}.
     * Examples: {@code registry.stage.c8y.io/stack/} → {@code registry.stage.c8y.io},
     * {@code https://host:5000/x} → {@code host:5000}, {@code alpine:3} → {@code https://index.docker.io/v1/}.
     */
    public static String registryHost(String registryUrl, String image) {
        if (!isBlank(registryUrl)) {
            String host = registryUrl.trim().replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");
            int slash = host.indexOf('/');
            if (slash >= 0) {
                host = host.substring(0, slash);
            }
            return host.isEmpty() || DOCKER_HUB_ALIASES.contains(host) ? DOCKER_HUB : host;
        }
        if (!isBlank(image)) {
            int slash = image.indexOf('/');
            if (slash > 0) {
                String first = image.substring(0, slash);
                if (first.contains(".") || first.contains(":") || "localhost".equals(first)) {
                    return DOCKER_HUB_ALIASES.contains(first) ? DOCKER_HUB : first;
                }
            }
        }
        return DOCKER_HUB;
    }

    /**
     * The user's docker configuration directory: {@code $DOCKER_CONFIG} or {@code ~/.docker}.
     */
    public static File userDockerConfigDir(Map<String, String> processEnvironment) {
        String configured = processEnvironment.get(DOCKER_CONFIG);
        if (!isBlank(configured)) {
            return new File(configured);
        }
        return new File(System.getProperty("user.home"), ".docker");
    }

    private static ObjectNode readConfig(File file) throws IOException {
        if (file.isFile()) {
            JsonNode node = MAPPER.readTree(file);
            if (node instanceof ObjectNode) {
                return (ObjectNode) node;
            }
        }
        return MAPPER.createObjectNode();
    }

    private static Path createPrivateDirectory() throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return Files.createTempDirectory("c8y-docker-config-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        }
        return Files.createTempDirectory("c8y-docker-config-");
    }

    private static void setPermissions(Path file, String permissions) throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(permissions));
        }
    }
}

package com.cumulocity.agent.packaging.microservice.buildx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CredentialsConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    File userDockerConfig;

    @ParameterizedTest
    @CsvSource(value = {
            // registryUrl, image, expected
            "registry.stage.c8y.io/stack/, alpine:3, registry.stage.c8y.io",
            "https://host:5000/x, alpine:3, host:5000",
            "http://localhost:5000, alpine:3, localhost:5000",
            "docker.io, alpine:3, https://index.docker.io/v1/",
            "NULL, test.registry/image:1, test.registry",
            "NULL, localhost/image:1, localhost",
            "NULL, host:5000/image:1, host:5000",
            "NULL, library/alpine:3, https://index.docker.io/v1/",
            "NULL, alpine:3, https://index.docker.io/v1/",
            "NULL, NULL, https://index.docker.io/v1/",
    }, nullValues = "NULL")
    void shouldNormalizeRegistryHost(String registryUrl, String image, String expected) {
        assertEquals(expected, CredentialsConfig.registryHost(registryUrl, image));
    }

    @Test
    void shouldWriteCredentialsWithoutModifyingUserConfig() throws Exception {
        String userConfig = "{"
                + "\"auths\":{\"other.registry\":{\"auth\":\"b3RoZXI6b3RoZXI=\"}},"
                + "\"credsStore\":\"desktop\","
                + "\"credHelpers\":{\"registry.stage.c8y.io\":\"ecr-login\",\"gcr.io\":\"gcloud\"},"
                + "\"currentContext\":\"desktop-linux\","
                + "\"proxies\":{\"default\":{\"httpProxy\":\"http://proxy:3128\"}}"
                + "}";
        Path userConfigFile = userDockerConfig.toPath().resolve("config.json");
        Files.writeString(userConfigFile, userConfig);

        Path directory;
        try (CredentialsConfig config = CredentialsConfig.create("registry.stage.c8y.io", "user", "pa:ss",
                userDockerConfig, "unix:///home/user/.docker/desktop/docker.sock")) {
            directory = config.configDir().toPath();
            assertEquals("unix:///home/user/.docker/desktop/docker.sock", config.dockerHost());

            Path configFile = directory.resolve("config.json");
            if (Files.getFileStore(configFile).supportsFileAttributeView("posix")) {
                assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(configFile)));
                assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(directory)));
            }

            JsonNode written = mapper.readTree(configFile.toFile());
            String expectedAuth = Base64.getEncoder().encodeToString("user:pa:ss".getBytes(StandardCharsets.UTF_8));
            assertEquals(expectedAuth, written.get("auths").get("registry.stage.c8y.io").get("auth").asText());
            assertEquals("b3RoZXI6b3RoZXI=", written.get("auths").get("other.registry").get("auth").asText());
            assertFalse(written.has("credsStore"));
            assertFalse(written.has("currentContext"));
            assertFalse(written.get("credHelpers").has("registry.stage.c8y.io"));
            assertEquals("gcloud", written.get("credHelpers").get("gcr.io").asText());
            assertEquals("http://proxy:3128", written.get("proxies").get("default").get("httpProxy").asText());
            assertEquals(new File(userDockerConfig, "cli-plugins").getAbsolutePath(),
                    written.get("cliPluginsExtraDirs").get(0).asText());
        }

        assertFalse(Files.exists(directory), "temporary directory is deleted on close");
        assertEquals(userConfig, Files.readString(userConfigFile), "user config is not modified");
    }

    @Test
    void shouldWorkWithoutUserConfig() throws Exception {
        try (CredentialsConfig config = CredentialsConfig.create("host:5000", "user", "pass", new File(userDockerConfig, "missing"), null)) {
            JsonNode written = mapper.readTree(new File(config.configDir(), "config.json"));
            assertTrue(written.get("auths").has("host:5000"));
            assertNull(config.dockerHost());
        }
    }

    @Test
    void shouldResolveUserDockerConfigDir() {
        assertEquals(new File("/custom/docker"), CredentialsConfig.userDockerConfigDir(Map.of("DOCKER_CONFIG", "/custom/docker")));
        assertEquals(new File(System.getProperty("user.home"), ".docker"), CredentialsConfig.userDockerConfigDir(Map.of()));
    }
}

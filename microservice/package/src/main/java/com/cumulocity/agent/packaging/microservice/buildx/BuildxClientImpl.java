package com.cumulocity.agent.packaging.microservice.buildx;

import com.cumulocity.agent.packaging.TargetPlatform;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;

import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j(topic = "BuildxClient")
@Component(role = BuildxClient.class)
public class BuildxClientImpl implements BuildxClient {

    static final int QUERY_TIMEOUT_SECONDS = 120;

    private static final int OUTPUT_TAIL_LINES = 20;

    private final CommandExecutor executor;

    /**
     * Builders created by this plugin, keyed by the isolated buildx config directory.
     */
    private final Map<String, CreatedBuilder> createdBuilders = new ConcurrentHashMap<>();

    public BuildxClientImpl() {
        this(new ProcessCommandExecutor());
    }

    @VisibleForTesting
    BuildxClientImpl(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public Optional<String> version(DockerCliContext context) {
        CommandExecutor.Result result = query(context, "buildx", "version");
        if (!result.isSuccess()) {
            log.debug("docker buildx version failed: {}", tail(result.getOutput()));
            return Optional.empty();
        }
        return Optional.of(result.getOutput().trim());
    }

    @Override
    public String selectBuilder(DockerCliContext context, List<TargetPlatform> platforms) throws MojoExecutionException {
        if (usesContainerdImageStore(context)) {
            log.info("Docker daemon uses the containerd image store, using the '{}' builder", DEFAULT_BUILDER);
            return DEFAULT_BUILDER;
        }

        CreatedBuilder existing = createdBuilders.get(key(context));
        if (existing != null) {
            log.info("Reusing builder {}", existing.name);
            return existing.name;
        }

        removeOrphanedBuilders(context);
        String name = context.getBuilderNamePrefix() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Creating temporary buildx builder {} (driver docker-container)", name);
        // host network: the builder reaches registries and proxies the same way as the docker daemon (e.g. localhost:5000)
        CommandExecutor.Result result = query(context, "buildx", "create", "--name", name, "--driver", "docker-container",
                "--driver-opt", "network=host");
        if (!result.isSuccess()) {
            throw new MojoExecutionException("Cannot create buildx builder " + name + ": " + tail(result.getOutput()));
        }
        CreatedBuilder created = new CreatedBuilder(name, key(context), cleanupContext(context));
        createdBuilders.put(key(context), created);
        registerShutdownHook(created);
        return name;
    }

    @Override
    public Set<String> platforms(DockerCliContext context, String builder) throws MojoExecutionException {
        CommandExecutor.Result result = command(context, context.getTimeoutSeconds(), false, null, "buildx", "inspect", builder, "--bootstrap");
        if (!result.isSuccess()) {
            throw new MojoExecutionException("Cannot inspect buildx builder " + builder + ": " + tail(result.getOutput()));
        }
        return parsePlatforms(result.getOutput());
    }

    @Override
    public void build(DockerCliContext context, BuildRequest request) throws MojoExecutionException {
        List<String> args = new ArrayList<>(Arrays.asList("buildx", "build",
                "--builder", request.getBuilder(),
                "--platform", TargetPlatform.toPlatformList(request.getPlatforms()),
                "--provenance=false"));
        if (request.isNoCache()) {
            args.addAll(Arrays.asList("--no-cache", "--pull"));
        }
        new TreeMap<>(request.getBuildArgs()).forEach((key, value) -> args.addAll(Arrays.asList("--build-arg", key + "=" + value)));
        new TreeMap<>(request.getBuildContexts()).forEach((key, value) -> args.addAll(Arrays.asList("--build-context", key + "=" + value)));
        request.getTags().forEach(tag -> args.addAll(Arrays.asList("--tag", tag)));
        if (request.getNetwork() != null && !request.getNetwork().isEmpty()) {
            args.addAll(Arrays.asList("--network", request.getNetwork()));
        }
        if (request.isLoad()) {
            args.add("--load");
        } else if (request.getOutput() != null) {
            args.addAll(Arrays.asList("--output", request.getOutput()));
        }
        args.add(request.getContext().getAbsolutePath());

        log.info("Building image for {} with builder {}", request.getPlatforms(), request.getBuilder());
        CommandExecutor.Result result = command(context, context.getTimeoutSeconds(), true, request.getStdout(), args.toArray(new String[0]));
        if (!result.isSuccess()) {
            throw new MojoExecutionException("Image build failed (exit code " + result.getExitCode() + "): " + tail(result.getOutput()));
        }
    }

    @Override
    public void removeImages(DockerCliContext context, Collection<String> images) {
        if (images.isEmpty()) {
            return;
        }
        List<String> args = new ArrayList<>(Arrays.asList("rmi", "--force"));
        args.addAll(images);
        CommandExecutor.Result result = query(context, args.toArray(new String[0]));
        if (result.isSuccess()) {
            log.info("Removed images {}", images);
        } else {
            log.warn("Failed to remove images {}: {}", images, tail(result.getOutput()));
        }
    }

    @Override
    public Optional<String> resolveDockerHost(DockerCliContext context) {
        CommandExecutor.Result result = query(context, "context", "inspect", "--format", "{{.Endpoints.docker.Host}}");
        String host = result.getOutput() == null ? "" : result.getOutput().trim();
        if (!result.isSuccess() || host.isEmpty()) {
            log.debug("Cannot resolve docker host of the active context: {}", host);
            return Optional.empty();
        }
        return Optional.of(host);
    }

    /**
     * Removes all builders created by this instance. Called on JVM shutdown.
     */
    public void removeCreatedBuilders() {
        for (CreatedBuilder builder : new ArrayList<>(createdBuilders.values())) {
            removeBuilder(builder);
        }
    }

    private void removeBuilder(CreatedBuilder builder) {
        if (createdBuilders.remove(builder.key, builder)) {
            log.info("Removing temporary buildx builder {}", builder.name);
            CommandExecutor.Result result = query(builder.context, "buildx", "rm", "--force", builder.name);
            if (!result.isSuccess()) {
                log.warn("Failed to remove buildx builder {}: {}", builder.name, tail(result.getOutput()));
            }
        }
    }

    @VisibleForTesting
    void registerShutdownHook(CreatedBuilder builder) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> removeBuilder(builder), "buildx-builder-cleanup"));
    }

    private boolean usesContainerdImageStore(DockerCliContext context) {
        CommandExecutor.Result result = query(context, "info", "--format", "{{json .DriverStatus}}");
        return result.isSuccess() && result.getOutput().contains("io.containerd.snapshotter");
    }

    /**
     * Removes builder containers left behind by interrupted builds (e.g. killed Maven process).
     */
    private void removeOrphanedBuilders(DockerCliContext context) {
        CommandExecutor.Result result = query(context, "ps", "--all",
                "--filter", "name=buildx_buildkit_" + context.getBuilderNamePrefix() + "-", "--format", "{{.Names}}");
        if (!result.isSuccess()) {
            return;
        }
        List<String> containers = Arrays.stream(result.getOutput().split("\\R"))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        if (!containers.isEmpty()) {
            log.info("Removing orphaned buildx builder containers {}", containers);
            List<String> args = new ArrayList<>(Arrays.asList("rm", "--force", "--volumes"));
            args.addAll(containers);
            query(context, args.toArray(new String[0]));
        }
    }

    private CommandExecutor.Result query(DockerCliContext context, String... args) {
        return command(context, QUERY_TIMEOUT_SECONDS, false, null, args);
    }

    private CommandExecutor.Result command(DockerCliContext context, int timeoutSeconds, boolean logOutput, OutputStream stdout, String... args) {
        List<String> command = new ArrayList<>();
        command.add(context.getDockerCli());
        // global options of the docker CLI, the buildx plugin uses them as well
        if (context.getDockerConfigDir() != null) {
            command.addAll(Arrays.asList("--config", context.getDockerConfigDir().getAbsolutePath()));
        }
        if (context.getDockerHost() != null) {
            command.addAll(Arrays.asList("--host", context.getDockerHost()));
        }
        command.addAll(Arrays.asList(args));
        log.debug("Executing {}", command);
        // buildx has no option for its state directory
        Map<String, String> environment = Map.of("BUILDX_CONFIG", context.getBuildxConfigDir().getAbsolutePath());
        return executor.execute(command, environment, timeoutSeconds, logOutput, stdout);
    }

    @VisibleForTesting
    static Set<String> parsePlatforms(String inspectOutput) {
        Set<String> platforms = new LinkedHashSet<>();
        for (String line : inspectOutput.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Platforms:")) {
                for (String platform : trimmed.substring("Platforms:".length()).split(",")) {
                    String value = platform.trim().replace("*", "").trim();
                    if (!value.isEmpty()) {
                        platforms.add(value);
                    }
                }
            }
        }
        return platforms;
    }

    private static String key(DockerCliContext context) {
        return context.getBuildxConfigDir().getAbsolutePath();
    }

    private static String tail(String output) {
        if (output == null) {
            return "";
        }
        List<String> lines = Arrays.asList(output.trim().split("\\R"));
        return String.join("\n", lines.subList(Math.max(0, lines.size() - OUTPUT_TAIL_LINES), lines.size()));
    }

    /**
     * The temporary docker configuration of a mojo execution is deleted before JVM shutdown, so builders are removed
     * with the user's regular docker configuration (BUILDX_CONFIG keeps pointing to the isolated buildx state).
     */
    private static DockerCliContext cleanupContext(DockerCliContext context) {
        return context.toBuilder().dockerConfigDir(null).build();
    }

    static final class CreatedBuilder {
        final String name;
        final String key;
        final DockerCliContext context;

        CreatedBuilder(String name, String key, DockerCliContext context) {
            this.name = name;
            this.key = key;
            this.context = context;
        }
    }
}

package com.cumulocity.agent.packaging.microservice.buildx;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.cumulocity.agent.packaging.TargetPlatform.AMD64;
import static com.cumulocity.agent.packaging.TargetPlatform.ARM64;
import static org.junit.jupiter.api.Assertions.*;

class BuildxClientImplTest {

    private static final String INSPECT_OUTPUT = "Name:          default\n"
            + "Driver:        docker\n"
            + "Nodes:\n"
            + "Name:             default\n"
            + "Status:           running\n"
            + "BuildKit version: v0.15.0\n"
            + "Platforms:        linux/amd64*, linux/amd64/v2, linux/amd64/v3, linux/386\n";

    @TempDir
    File buildDir;

    FakeExecutor executor;
    BuildxClientImpl client;
    DockerCliContext context;

    @BeforeEach
    void setUp() {
        executor = new FakeExecutor();
        client = new BuildxClientImpl(executor) {
            @Override
            void registerShutdownHook(CreatedBuilder builder) {
                // no JVM hooks in tests
            }
        };
        context = DockerCliContext.builder()
                .buildxConfigDir(new File(buildDir, "buildx"))
                .builderNamePrefix("c8y-service")
                .timeoutSeconds(42)
                .build();
    }

    @Test
    void shouldReturnBuildxVersion() {
        executor.respond("docker buildx version", 0, "github.com/docker/buildx v0.36.1 1d8dde89b8aba914e05e45366770736fea1fd690\n");

        assertEquals(Optional.of("github.com/docker/buildx v0.36.1 1d8dde89b8aba914e05e45366770736fea1fd690"), client.version(context));

        Invocation invocation = executor.invocations.get(0);
        assertEquals(Map.of("BUILDX_CONFIG", new File(buildDir, "buildx").getAbsolutePath()), invocation.environment);
    }

    @Test
    void shouldReturnNoVersionWhenBuildxIsMissing() {
        executor.respond("docker buildx version", 1, "docker: 'buildx' is not a docker command.");

        assertEquals(Optional.empty(), client.version(context));
    }

    @Test
    void shouldPassCredentialsAsGlobalOptions() {
        client.version(context.toBuilder().dockerConfigDir(new File("/tmp/docker-config")).dockerHost("unix:///run/docker.sock").build());

        assertEquals("docker --config " + new File("/tmp/docker-config").getAbsolutePath() + " --host unix:///run/docker.sock buildx version",
                executor.commands().get(0));
    }

    @Test
    void shouldUseDefaultBuilderWithContainerdImageStore() throws MojoExecutionException {
        executor.respond("docker info", 0, "[[\"driver-type\",\"io.containerd.snapshotter.v1\"]]");

        assertEquals("default", client.selectBuilder(context, List.of(AMD64, ARM64)));
        assertEquals("default", client.selectBuilder(context, List.of(ARM64)));
        assertTrue(executor.commands().stream().noneMatch(c -> c.startsWith("docker buildx create")));
    }

    @Test
    void shouldCreateTemporaryBuilderOnceAndRemoveIt() throws MojoExecutionException {
        String withConfig = "docker --config " + new File("/tmp/docker-config").getAbsolutePath();
        executor.respond(withConfig + " info", 0, "[[\"Backing Filesystem\",\"extfs\"]]");
        executor.respond(withConfig + " ps", 0, "buildx_buildkit_c8y-service-deadbeef0\n");

        String builder = client.selectBuilder(context.toBuilder().dockerConfigDir(new File("/tmp/docker-config")).build(), List.of(AMD64));
        executor.respond("docker info", 0, "[[\"Backing Filesystem\",\"extfs\"]]");
        String reused = client.selectBuilder(context, List.of(AMD64, ARM64));

        assertTrue(builder.startsWith("c8y-service-"), builder);
        assertEquals(builder, reused);
        assertTrue(executor.commands().contains(withConfig + " rm --force --volumes buildx_buildkit_c8y-service-deadbeef0"),
                "orphaned builder container removed: " + executor.commands());
        assertEquals(1, executor.commands().stream().filter(c -> c.equals(withConfig + " buildx create --name " + builder + " --driver docker-container --driver-opt network=host")).count());

        client.removeCreatedBuilders();
        client.removeCreatedBuilders();

        Invocation remove = executor.invocations.stream().filter(i -> i.command().startsWith("docker buildx rm")).findFirst().orElseThrow();
        assertEquals("docker buildx rm --force " + builder, remove.command(), "temporary docker config is not used for cleanup");
        assertEquals(new File(buildDir, "buildx").getAbsolutePath(), remove.environment.get("BUILDX_CONFIG"));
        assertEquals(1, executor.commands().stream().filter(c -> c.startsWith("docker buildx rm")).count());
    }

    @Test
    void shouldReturnPlatformsOfBuilder() throws MojoExecutionException {
        executor.respond("docker buildx inspect", 0, INSPECT_OUTPUT);

        assertEquals(new LinkedHashSet<>(List.of("linux/amd64", "linux/amd64/v2", "linux/amd64/v3", "linux/386")),
                client.platforms(context, "default"));
        assertEquals("docker buildx inspect default --bootstrap", executor.commands().get(0));
    }

    @Test
    void shouldFailWhenBuilderCannotBeInspected() {
        executor.respond("docker buildx inspect", 1, "no builder found");

        assertThrows(MojoExecutionException.class, () -> client.platforms(context, "default"));
    }

    @Test
    void shouldParsePlatforms() {
        assertEquals(new LinkedHashSet<>(List.of("linux/amd64", "linux/arm64", "linux/arm/v7")),
                BuildxClientImpl.parsePlatforms("Platforms: linux/amd64*, linux/arm64\nPlatforms: linux/arm/v7, linux/arm64"));
        assertTrue(new HashSet<>(BuildxClientImpl.parsePlatforms("Platforms: linux/arm64/v8")).stream()
                .anyMatch(p -> p.startsWith(ARM64.platform() + "/")));
    }

    @Test
    void shouldBuildSinglePlatformAndLoadIntoDaemon() throws MojoExecutionException {
        client.build(context, BuildRequest.builder()
                .builder("default")
                .context(new File("/work"))
                .platform(AMD64)
                .buildArg("HTTP_PROXY", "http://proxy:3128")
                .tag("service:1.0")
                .tag("service:latest")
                .network("custom-network")
                .load(true)
                .build());

        Invocation build = executor.invocations.get(0);
        assertEquals("docker buildx build --builder default --platform linux/amd64 --provenance=false "
                + "--build-arg HTTP_PROXY=http://proxy:3128 --tag service:1.0 --tag service:latest "
                + "--network custom-network --load " + new File("/work").getAbsolutePath(), build.command());
        assertEquals(42, build.timeoutSeconds);
        assertTrue(build.logOutput);
    }

    @Test
    void shouldBuildMultiPlatformToOciArchive() throws MojoExecutionException {
        client.build(context, BuildRequest.builder()
                .builder("c8y-service-1")
                .context(new File("/work"))
                .platform(AMD64)
                .platform(ARM64)
                .output("type=oci,dest=/out/image.tar,name=service:1.0")
                .build());

        assertEquals("docker buildx build --builder c8y-service-1 --platform linux/amd64,linux/arm64 --provenance=false "
                + "--output type=oci,dest=/out/image.tar,name=service:1.0 " + new File("/work").getAbsolutePath(), executor.commands().get(0));
    }

    @Test
    void shouldPassNoCacheAndBuildContexts() throws MojoExecutionException {
        client.build(context, BuildRequest.builder()
                .builder("c8y-service-1")
                .context(new File("/check"))
                .platform(AMD64)
                .platform(ARM64)
                .buildContext("microservice", "oci-layout:///out/image@sha256:1234")
                .noCache(true)
                .output("type=local,dest=/out/java-version")
                .build());

        assertEquals("docker buildx build --builder c8y-service-1 --platform linux/amd64,linux/arm64 --provenance=false "
                + "--no-cache --pull "
                + "--build-context microservice=oci-layout:///out/image@sha256:1234 "
                + "--output type=local,dest=/out/java-version " + new File("/check").getAbsolutePath(), executor.commands().get(0));
    }

    @Test
    void shouldWriteStdoutToStream() throws MojoExecutionException {
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        executor.respond("docker buildx build", 0, "#1 exporting", "image-tar");

        client.build(context, BuildRequest.builder()
                .builder("c8y-service-1")
                .context(new File("/work"))
                .platform(AMD64)
                .output("type=docker,name=service:1.0,dest=-")
                .stdout(image)
                .build());

        assertEquals("image-tar", image.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldReportBuildFailure() {
        executor.respond("docker buildx build", 1, "#5 ERROR: failed to solve: exec format error");

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> client.build(context, BuildRequest.builder()
                .builder("default").context(new File("/work")).platform(AMD64).load(true).build()));
        assertTrue(exception.getMessage().contains("exec format error"), exception.getMessage());
    }

    @Test
    void shouldRemoveImages() {
        client.removeImages(context, List.of("service:1.0", "service:latest"));

        assertEquals("docker rmi --force service:1.0 service:latest", executor.commands().get(0));
    }

    @Test
    void shouldResolveDockerHost() {
        executor.respond("docker context inspect", 0, "unix:///var/run/docker.sock\n");
        assertEquals(Optional.of("unix:///var/run/docker.sock"), client.resolveDockerHost(context));

        executor.respond("docker context inspect", 1, "context not found");
        assertEquals(Optional.empty(), client.resolveDockerHost(context));
    }

    @Test
    void shouldUseConfiguredDockerCli() {
        executor.respond("/opt/docker buildx version", 0, "github.com/docker/buildx v0.12.0");

        client.version(context.toBuilder().dockerCli("/opt/docker").build());

        assertEquals("/opt/docker buildx version", executor.commands().get(0));
    }

    static class Invocation {
        final List<String> command;
        final Map<String, String> environment;
        final int timeoutSeconds;
        final boolean logOutput;

        Invocation(List<String> command, Map<String, String> environment, int timeoutSeconds, boolean logOutput) {
            this.command = command;
            this.environment = environment;
            this.timeoutSeconds = timeoutSeconds;
            this.logOutput = logOutput;
        }

        String command() {
            return String.join(" ", command);
        }
    }

    static class FakeExecutor implements CommandExecutor {
        final List<Invocation> invocations = new ArrayList<>();
        final Map<String, Result> responses = new LinkedHashMap<>();
        final Map<String, String> stdouts = new HashMap<>();

        void respond(String commandPrefix, int exitCode, String output) {
            responses.put(commandPrefix, new Result(exitCode, output));
        }

        void respond(String commandPrefix, int exitCode, String output, String stdout) {
            respond(commandPrefix, exitCode, output);
            stdouts.put(commandPrefix, stdout);
        }

        List<String> commands() {
            List<String> commands = new ArrayList<>();
            invocations.forEach(i -> commands.add(i.command()));
            return commands;
        }

        @Override
        public Result execute(List<String> command, Map<String, String> environment, int timeoutSeconds, boolean logOutput, OutputStream stdout) {
            Invocation invocation = new Invocation(command, environment, timeoutSeconds, logOutput);
            invocations.add(invocation);
            stdouts.forEach((prefix, content) -> {
                if (stdout != null && invocation.command().startsWith(prefix)) {
                    try {
                        stdout.write(content.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
            return responses.entrySet().stream()
                    .filter(e -> invocation.command().startsWith(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(new Result(0, ""));
        }
    }
}

package com.cumulocity.agent.packaging.image;

import com.cumulocity.agent.packaging.DockerImage;
import com.cumulocity.agent.packaging.TargetPlatform;
import com.cumulocity.agent.packaging.microservice.buildx.BuildRequest;
import com.cumulocity.agent.packaging.microservice.buildx.BuildxClient;
import com.cumulocity.agent.packaging.microservice.buildx.CredentialsConfig;
import com.cumulocity.agent.packaging.microservice.buildx.DockerCliContext;
import com.cumulocity.agent.packaging.microservice.buildx.JavaVersionCheck;
import com.cumulocity.agent.packaging.microservice.buildx.OciLayoutImage;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Builds images with {@code docker buildx}, one image for all platforms (an OCI image index for several platforms).
 * <p>
 * The image is built once into an OCI layout in {@code target/buildx-image}. For several platforms the Java version of
 * all platforms is checked. Then the image is exported from the layout into the zip, and {@code push} pushes the same
 * layout to the registry, so the zip and the registry get the same image.
 */
@Slf4j
public class BuildxImageBuilder implements ImageBuilder {

    static final int[] MIN_BUILDX_VERSION = {0, 10};
    static final Set<String> MULTI_PLATFORM_NETWORKS = Set.of("default", "host", "none");
    static final String IMAGE_DIR = "buildx-image";

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\bv?(\\d+)\\.(\\d+)(?:\\.\\d+)?");

    /**
     * Prepares the docker build context when {@code push} runs without the {@code package} goal.
     */
    @FunctionalInterface
    public interface BuildContextPreparer {
        void prepare() throws MojoExecutionException;
    }

    private final BuildxClient client;
    private final ImageBuildSettings settings;
    private final BuildContextPreparer buildContext;

    public BuildxImageBuilder(BuildxClient client, ImageBuildSettings settings, BuildContextPreparer buildContext) {
        this.client = client;
        this.settings = settings;
        this.buildContext = buildContext;
    }

    @Override
    public void build(List<TargetPlatform> platforms, ImagePackager packager) throws MojoExecutionException {
        run(platforms, (context, builder) -> {
            OciLayoutImage image = buildImage(context, builder, platforms);
            if (packager != null) {
                String name = settings.getImageName() + ":" + settings.getVersion();
                packager.write(new PackagedImage(platforms, null, outputStream -> image.writeTo(name, outputStream)));
            } else if (platforms.size() == 1 || BuildxClient.DEFAULT_BUILDER.equals(builder)) {
                // the default builder is used only with the containerd image store, which keeps multi-platform images
                image.load(settings.tags());
            } else {
                log.info("The multi-platform image is not loaded into the docker daemon, its image store keeps only "
                        + "single-platform images. Turn on the containerd image store to load it.");
            }
        });
    }

    @Override
    public void push(List<TargetPlatform> platforms, DockerImage target) throws MojoExecutionException {
        List<String> tags = List.of(target.withTag(settings.getVersion()).toString(), target.withTag("latest").toString());
        log.info("Publishing image {} for platforms {}", tags, platforms);
        run(platforms, (context, builder) -> {
            File imageDir = imageDir();
            // an image with different Java versions is not reused when the mismatch fails the build
            Optional<OciLayoutImage> packaged = OciLayoutImage.existing(client, context, builder, platforms, imageDir,
                    settings.isFailOnJavaVersionMismatch());
            OciLayoutImage image;
            if (packaged.isPresent()) {
                log.info("Pushing the image built by the package goal from {}", imageDir);
                image = packaged.get();
            } else {
                buildContext.prepare();
                image = buildImage(context, builder, platforms);
            }
            image.push(tags);
            if (BuildxClient.DEFAULT_BUILDER.equals(builder)) {
                // the docker driver stores the pushed image in the daemon as well
                client.removeImages(context, tags);
            }
        });
    }

    @FunctionalInterface
    private interface BuildxAction {
        void run(DockerCliContext context, String builder) throws MojoExecutionException;
    }

    /**
     * Runs the checks, prepares credentials and the builder, then runs the action.
     */
    private void run(List<TargetPlatform> platforms, BuildxAction action) throws MojoExecutionException {
        DockerCliContext context = dockerCliContext();
        verify(context, platforms);
        try (CredentialsConfig credentials = createCredentialsConfig(context)) {
            if (credentials != null) {
                context = context.toBuilder().dockerConfigDir(credentials.configDir()).dockerHost(credentials.dockerHost()).build();
            }
            String builder = client.selectBuilder(context, platforms);
            verifyEmulation(context, builder, platforms);
            action.run(context, builder);
        }
    }

    /**
     * Checks before the build: buildx is available and new enough, and the network works for the platforms.
     */
    @VisibleForTesting
    void verify(DockerCliContext context, List<TargetPlatform> platforms) throws MojoExecutionException {
        String version = client.version(context).orElseThrow(() -> new MojoExecutionException(
                "docker buildx is required to build microservice images, but '" + context.getDockerCli() + " buildx version' failed. "
                        + "Install Docker Engine 23+ with the docker-buildx-plugin package or Docker Desktop."));
        int[] parsed = parseVersion(version);
        if (parsed != null && compare(parsed, MIN_BUILDX_VERSION) < 0) {
            throw new MojoExecutionException(String.format("docker buildx %d.%d or newer is required, found %d.%d",
                    MIN_BUILDX_VERSION[0], MIN_BUILDX_VERSION[1], parsed[0], parsed[1]));
        }
        log.info("Using {}", version);

        String network = settings.getNetwork();
        if (platforms.size() > 1 && isNotBlank(network) && !MULTI_PLATFORM_NETWORKS.contains(network)) {
            throw new MojoExecutionException("Network '" + network + "' is not supported for multi-platform builds. "
                    + "docker buildx supports only: " + MULTI_PLATFORM_NETWORKS);
        }
    }

    /**
     * Warns when the builder does not list one of the supported platforms, e.g. arm64 without QEMU emulation on an
     * amd64 host. Only a warning: a {@code docker-container} builder can emulate platforms it does not list. Other
     * platforms are passed to docker as given; docker reports them during the build.
     */
    @VisibleForTesting
    void verifyEmulation(DockerCliContext context, String builder, List<TargetPlatform> platforms) throws MojoExecutionException {
        Set<String> supported = client.platforms(context, builder);
        List<TargetPlatform> missing = platforms.stream()
                .filter(TargetPlatform::isSupported)
                .filter(platform -> supported.stream().noneMatch(s -> s.equals(platform.platform()) || s.startsWith(platform.platform() + "/")))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            String architectures = missing.stream().map(TargetPlatform::architecture).collect(Collectors.joining(","));
            log.warn("The buildx builder {} does not list {}, it supports {}. The build may fail with 'exec format error'. "
                    + "Install QEMU emulation: docker run --privileged --rm tonistiigi/binfmt --install {}", builder, missing, supported, architectures);
        }
    }

    /**
     * Builds the image into the OCI layout and, for several platforms, checks the Java version of all platforms.
     */
    private OciLayoutImage buildImage(DockerCliContext context, String builder, List<TargetPlatform> platforms) throws MojoExecutionException {
        BuildRequest request = BuildRequest.builder()
                .builder(builder)
                .context(settings.getDockerWorkDir())
                .platforms(platforms)
                .buildArgs(settings.getBuildArgs())
                .network(settings.getNetwork())
                .noCache(settings.isNoCache())
                .build();
        OciLayoutImage image = OciLayoutImage.build(client, context, request, imageDir());
        boolean sameJavaVersion = platforms.size() == 1
                || new JavaVersionCheck(settings.isFailOnJavaVersionMismatch(), settings.isNoCache()).check(image);
        // only a checked image is reused by the push goal
        image.complete(sameJavaVersion);
        return image;
    }

    private File imageDir() {
        return new File(settings.getBuildDir(), IMAGE_DIR);
    }

    private DockerCliContext dockerCliContext() {
        return DockerCliContext.builder()
                .dockerCli(settings.getDockerCli())
                .buildxConfigDir(new File(settings.getBuildDir(), "buildx"))
                .builderNamePrefix(settings.getBuilderNamePrefix())
                .timeoutSeconds(settings.getTimeoutSeconds())
                .build();
    }

    /**
     * Temporary docker configuration with the configured pull credentials, or {@code null} when no credentials are
     * configured (then the user's {@code docker login} is used).
     */
    private CredentialsConfig createCredentialsConfig(DockerCliContext context) throws MojoExecutionException {
        String registryHost = CredentialsConfig.registryHost(settings.getPullRegistryUrl(), settings.getBaseImage());
        if (!(isNotBlank(settings.getPullRegistryUser()) && isNotBlank(settings.getPullRegistryPass()))) {
            // anonymous pulls from Docker Hub are normal, the docker login hint is useful only for other registries
            if (!CredentialsConfig.DOCKER_HUB.equals(registryHost)) {
                log.info("Using docker CLI credentials (docker login) for {}. To define credentials, configure both registryUser and registryPass.", registryHost);
            }
            return null;
        }
        // DOCKER_HOST and DOCKER_CONFIG come from the user's environment. The temporary configuration has no docker
        // contexts, so the daemon address of the active context is passed explicitly with --host.
        Map<String, String> processEnvironment = System.getenv();
        String dockerHost = processEnvironment.get(CredentialsConfig.DOCKER_HOST);
        if (dockerHost == null) {
            dockerHost = client.resolveDockerHost(context).orElse(null);
        }
        try {
            return CredentialsConfig.create(registryHost, settings.getPullRegistryUser(), settings.getPullRegistryPass(),
                    CredentialsConfig.userDockerConfigDir(processEnvironment), dockerHost);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot prepare docker configuration with registry credentials", e);
        }
    }

    @VisibleForTesting
    static int[] parseVersion(String versionOutput) {
        Matcher matcher = VERSION_PATTERN.matcher(versionOutput);
        if (matcher.find()) {
            return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
        }
        return null;
    }

    private static int compare(int[] a, int[] b) {
        return a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
    }
}

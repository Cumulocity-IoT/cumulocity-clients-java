package com.cumulocity.agent.packaging.image;

import com.cumulocity.agent.packaging.DockerImage;
import com.cumulocity.agent.packaging.TargetPlatform;
import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthConfigurations;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Builds images through the docker daemon API (docker-java), one image and one zip per platform.
 * <p>
 * The zip of {@code linux/amd64} has the default name; with several platforms the zips of the other platforms get the
 * platform in their name. {@code push} pushes the image built last.
 */
@Slf4j
public class DaemonImageBuilder implements ImageBuilder {

    private final MicroserviceDockerClient dockerClient;
    private final ImageBuildSettings settings;

    public DaemonImageBuilder(MicroserviceDockerClient dockerClient, ImageBuildSettings settings) {
        this.dockerClient = dockerClient;
        this.settings = settings;
    }

    @Override
    public void build(List<TargetPlatform> platforms, ImagePackager packager) throws MojoExecutionException {
        // one platform after another: all platforms are built with the same tags
        for (TargetPlatform platform : platforms) {
            final String imageWithVersion = settings.getImageName() + ":" + settings.getVersion();
            final Set<String> tags = Sets.newHashSet(settings.tags());

            log.info("Building microservice image for target platform {}", platform);
            dockerClient.buildDockerImage(settings.getDockerWorkDir().getAbsolutePath(), tags, settings.getBuildArgs(), platform.platform(),
                    settings.getNetwork(), settings.getTimeoutSeconds(), pullCredentials(), settings.isNoCache());
            cleanSpotifyDockerDirectory();
            if (packager == null) {
                continue;
            }

            log.info("Checking if image {} is populated to registry", imageWithVersion);
            dockerClient.waitForImageInRegistry(imageWithVersion, settings.getImageWaitTimeoutSeconds());
            TargetPlatform fileNamePlatform = platforms.size() > 1 && !TargetPlatform.AMD64.equals(platform) ? platform : null;
            packager.write(new PackagedImage(List.of(platform), fileNamePlatform,
                    outputStream -> dockerClient.saveDockerImage(imageWithVersion, outputStream)));

            if (settings.isDeleteImage()) {
                log.info("Deleting images {}", tags);
                tags.forEach(tag -> dockerClient.deleteAll(tag, true));
            } else {
                log.info("Skipping docker image cleanup");
            }
        }
    }

    @Override
    public void push(List<TargetPlatform> platforms, DockerImage target) {
        DockerImage source = DockerImage.ofName(settings.getImageName());
        if (platforms.size() > 1) {
            log.warn("Only the image built last ({}) is pushed, all platforms {} share the same tags. "
                    + "Set microservice.package.buildx=true to push one multi-platform image.", platforms.get(platforms.size() - 1), platforms);
        }
        for (String tag : List.of("latest", settings.getVersion())) {
            DockerImage pushed = target.withTag(tag);
            log.info("Publishing image {} to registry", pushed);
            dockerClient.tagImage(source.withTag(tag).toString(), pushed.toString(), tag);
            dockerClient.pushImage(pushed.toString());
        }
        cleanup(source);
        cleanup(target);
    }

    /**
     * Cleaning up directory created by spotify docker plugin.
     */
    private void cleanSpotifyDockerDirectory() throws MojoExecutionException {
        File directory = new File(settings.getBuildDir(), "docker");
        try {
            if (directory.exists()) {
                FileUtils.cleanDirectory(directory);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot clean " + directory, e);
        }
    }

    private void cleanup(DockerImage image) {
        log.info("Running cleanup on {} with tags", image);
        dockerClient.deleteAll(image.toString(), true);
    }

    private AuthConfigurations pullCredentials() {
        AuthConfigurations authConfigurations = new AuthConfigurations();
        String registryAddress = registryAddress();
        if (isNotBlank(settings.getPullRegistryUser()) && isNotBlank(settings.getPullRegistryPass())) {
            authConfigurations.addConfig(new AuthConfig()
                    .withRegistryAddress(registryAddress)
                    .withUsername(settings.getPullRegistryUser())
                    .withPassword(settings.getPullRegistryPass()));
        } else if (!AuthConfig.DEFAULT_SERVER_ADDRESS.equals(registryAddress)) {
            log.info("Using unauthenticated access to {}. To define credentials, configure both registryUser and registryPass.", registryAddress);
        }
        return authConfigurations;
    }

    private String registryAddress() {
        if (isNotBlank(settings.getPullRegistryUrl())) {
            return settings.getPullRegistryUrl();
        }
        String baseImage = settings.getBaseImage();
        int registryUrlIndex = baseImage == null ? -1 : baseImage.lastIndexOf("/");
        if (registryUrlIndex > 0) {
            return "https://" + baseImage.substring(0, registryUrlIndex);
        }
        return AuthConfig.DEFAULT_SERVER_ADDRESS;
    }
}

package com.cumulocity.agent.packaging.image;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Mojo configuration used by the image builders.
 */
@Value
@Builder(toBuilder = true)
public class ImageBuildSettings {

    /**
     * Name of the built image, tagged with {@link #version} and {@code latest}.
     */
    @NonNull
    String imageName;

    @NonNull
    String version;

    /**
     * Docker build context prepared by the {@code package} goal.
     */
    @NonNull
    File dockerWorkDir;

    /**
     * Maven build directory ({@code target}).
     */
    @NonNull
    File buildDir;

    @Singular
    Map<String, String> buildArgs;

    /**
     * Network for {@code RUN} instructions, may be {@code null}.
     */
    String network;

    int timeoutSeconds;

    /**
     * Build every layer again and pull the base image.
     */
    boolean noCache;

    /**
     * Remove the image from the docker daemon after it was written to the zip (docker daemon build).
     */
    boolean deleteImage;

    /**
     * Maximum time to wait for the built image in the docker daemon (docker daemon build).
     */
    int imageWaitTimeoutSeconds;

    /**
     * Registry of the base image and its credentials, used to pull the base image (not the push target).
     */
    String pullRegistryUrl;
    String pullRegistryUser;
    @ToString.Exclude
    String pullRegistryPass;
    String baseImage;

    /**
     * @return {@code <imageName>:<version>} and {@code <imageName>:latest}
     */
    public List<String> tags() {
        return List.of(imageName + ":" + version, imageName + ":latest");
    }
}

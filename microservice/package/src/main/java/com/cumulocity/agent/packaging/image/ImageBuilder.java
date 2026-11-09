package com.cumulocity.agent.packaging.image;

import com.cumulocity.agent.packaging.DockerImage;
import com.cumulocity.agent.packaging.TargetPlatform;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * Builds and pushes the microservice image. Each build method decides how many images it creates for the target
 * platforms: {@link DaemonImageBuilder} one per platform, {@link BuildxImageBuilder} one for all platforms.
 */
public interface ImageBuilder {

    /**
     * Builds the images and passes each one to {@code packager}.
     *
     * @param packager writes an image into the microservice zip; {@code null} keeps the image in the docker daemon
     *                 instead ({@code skipMicroservicePackage})
     */
    void build(List<TargetPlatform> platforms, ImagePackager packager) throws MojoExecutionException;

    /**
     * Pushes the image built by the {@code package} goal as {@code target:<version>} and {@code target:latest}.
     *
     * @param target the image in the registry, without tag
     */
    void push(List<TargetPlatform> platforms, DockerImage target) throws MojoExecutionException;
}

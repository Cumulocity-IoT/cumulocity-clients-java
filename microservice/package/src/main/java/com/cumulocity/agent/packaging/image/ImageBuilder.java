package com.cumulocity.agent.packaging.image;

import com.cumulocity.agent.packaging.DockerImage;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * Builds and pushes the microservice image.
 */
public interface ImageBuilder {

    /**
     * Builds the images and passes each one to {@code packager}.
     *
     * @param packager writes an image into the microservice zip; {@code null} keeps the image in the docker daemon
     *                 instead ({@code skipMicroservicePackage})
     */
    void build(List<String> platforms, ImagePackager packager) throws MojoExecutionException;

    /**
     * Pushes the image built by the {@code package} goal as {@code target:<version>} and {@code target:latest}.
     *
     * @param target the image in the registry, without tag
     */
    void push(DockerImage target) throws MojoExecutionException;
}

package com.cumulocity.agent.packaging.image;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Writes a built image into a microservice zip.
 */
@FunctionalInterface
public interface ImagePackager {

    void write(PackagedImage image) throws MojoExecutionException;
}

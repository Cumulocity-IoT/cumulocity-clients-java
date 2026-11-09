package com.cumulocity.agent.packaging.image;

import lombok.Value;

import java.util.List;

/**
 * One built image for one microservice zip. The {@link #writer} is valid only during {@link ImagePackager#write}: the
 * image builder may remove what it needs afterwards.
 */
@Value
public class PackagedImage {

    /**
     * Platforms of the image, written to {@code dockerBuildInfo} of {@code cumulocity.json}.
     */
    List<String> platforms;

    /**
     * Platform added to the zip file name, e.g. {@code <name>-<version>-linux-arm64.zip}; {@code linux/amd64} gets the
     * default name {@code <name>-<version>.zip}.
     */
    String fileNamePlatform;

    ImageWriter writer;
}

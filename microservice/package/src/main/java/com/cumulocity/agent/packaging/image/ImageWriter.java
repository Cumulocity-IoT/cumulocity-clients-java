package com.cumulocity.agent.packaging.image;

import java.io.OutputStream;

/**
 * Writes the image tar ({@code image.tar} of the microservice zip).
 */
@FunctionalInterface
public interface ImageWriter {

    void write(OutputStream outputStream) throws Exception;
}

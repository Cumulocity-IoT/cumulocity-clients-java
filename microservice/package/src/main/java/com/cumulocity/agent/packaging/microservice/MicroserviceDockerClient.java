package com.cumulocity.agent.packaging.microservice;

import com.github.dockerjava.api.model.AuthConfigurations;
import org.apache.maven.MavenExecutionException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

public interface MicroserviceDockerClient {
    void saveDockerImage(final String image, final OutputStream outputStream) throws IOException, MavenExecutionException;

    /**
     * @param noCache build every layer again and pull the base image
     */
    void buildDockerImage(String dockerDirectory, Set<String> tags, Map<String, String> buildArgs, String platform, String networkMode, Integer dockerClientTimeout, AuthConfigurations pullConfiguration, boolean noCache);

    void deleteAll(String image, boolean withForce);

    void tagImage(String image, String imageNameWithRepository, String tag);

    void pushImage(String name);

    void waitForImageInRegistry(String image, int timeOutSeconds);
}

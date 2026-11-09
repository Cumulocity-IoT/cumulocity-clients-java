package com.cumulocity.agent.packaging.microservice.buildx;

import com.cumulocity.agent.packaging.TargetPlatform;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Runs {@code docker buildx} commands. Decisions (which checks fail the build, what to build) are made by the caller.
 */
public interface BuildxClient {

    String DEFAULT_BUILDER = "default";

    /**
     * @return the output of {@code docker buildx version}, or empty when the docker CLI or the buildx plugin is not available
     */
    Optional<String> version(DockerCliContext context);

    /**
     * Returns the builder to use. With the containerd image store of the docker daemon the built-in {@code default}
     * builder is used; otherwise a temporary {@code docker-container} builder is created in the isolated buildx config
     * and removed when the JVM exits (the {@code default} builder of the classic image store cannot export images).
     */
    String selectBuilder(DockerCliContext context, List<TargetPlatform> platforms) throws MojoExecutionException;

    /**
     * @return the platforms the builder can build, e.g. {@code linux/amd64}, {@code linux/arm64}
     */
    Set<String> platforms(DockerCliContext context, String builder) throws MojoExecutionException;

    void build(DockerCliContext context, BuildRequest request) throws MojoExecutionException;

    void removeImages(DockerCliContext context, Collection<String> images);

    /**
     * Resolves the daemon address of the active docker context ({@code docker context inspect}).
     */
    Optional<String> resolveDockerHost(DockerCliContext context);
}

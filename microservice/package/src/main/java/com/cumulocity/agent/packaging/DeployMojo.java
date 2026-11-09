package com.cumulocity.agent.packaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.maven.plugins.annotations.LifecyclePhase.DEPLOY;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

/**
 * Pushes the microservice image to {@code agent-package.container.registry} as {@code <name>:<version>} and
 * {@code <name>:latest}.
 * <p>
 * By default the image built by the {@code package} goal is tagged and pushed from the local docker daemon. For several
 * target platforms this is the image built last.
 * With {@code microservice.package.buildx=true} the image built and checked by the {@code package} goal is pushed by
 * docker buildx; for several target platforms one multi-platform image (image index).
 */
@Mojo(name = "push", defaultPhase = DEPLOY, requiresDependencyResolution = RUNTIME, threadSafe = true)
@Slf4j
public class DeployMojo extends BaseMicroserviceMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || containerSkip) {
            getLog().info("skipping agent deploy");
            return;
        }
        if (isNullOrEmpty(registry)) {
            getLog().warn("docker registry is undefined skipping push");
            return;
        }

        imageBuilder().push(getTargetPlatforms(), DockerImage.ofName(name).withRegistry(registry));
    }
}

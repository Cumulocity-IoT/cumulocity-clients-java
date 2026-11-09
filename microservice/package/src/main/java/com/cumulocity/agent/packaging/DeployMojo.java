package com.cumulocity.agent.packaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.maven.plugins.annotations.LifecyclePhase.DEPLOY;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

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

        imageBuilder().push(DockerImage.ofName(name).withRegistry(registry));
    }


}

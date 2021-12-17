package com.cumulocity.agent.packaging;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.cumulocity.agent.packaging.DockerDsl.docker;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.maven.plugins.annotations.LifecyclePhase.DEPLOY;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "push", defaultPhase = DEPLOY, requiresDependencyResolution = RUNTIME, threadSafe = true)
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

        final ImmutableList<String> tags = ImmutableList.of("latest", project.getVersion());
        for (String tag : tags) {
            publish(tag);
        }
        final DockerImage image = DockerImage.ofName(name);
        cleanup(image, tags);
        cleanup(image.withRegistry(registry), tags);


    }

    private boolean configExists() {
		return Files.exists(Paths.get(System.getProperty("user.home")).resolve(".docker/config.json"));
    }

    private void publish(String tag) throws MojoExecutionException {
        final DockerImage source = DockerImage.ofName(name).withTag(tag);
        final DockerImage pushed = source.withRegistry(registry);

        //@formatter:off


	executeMojo(
            docker(),
            goal("tag"),
            configuration(
                element("image", source.toString() ),
                element("newName", pushed.toString())
            ),
            executionEnvironment(this.project, this.mavenSession, this.pluginManager)
        );
        executeMojo(
            docker(),
            goal("push"),
            configuration(
                element("imageName", pushed.toString()),
		element("useConfigFile", "true")
            ),
            executionEnvironment(this.project, this.mavenSession, this.pluginManager)
        );

        //@formatter:on

    }

    public void cleanup(DockerImage image, List<String> tags) throws MojoExecutionException {

        //@formatter:off
        executeMojo(
            docker(),
            goal("removeImage"),
            configuration(
                element("imageName", image.toString()),
                element("imageTags", tags(tags))

            ),
            executionEnvironment(this.project, this.mavenSession, this.pluginManager)
        );
        //@formatter:on
    }

    private Element[] tags(List<String> tags) {
        return FluentIterable.from(tags).transform(new Function<String, Element>() {
            @Override
            public Element apply(@Nullable String tag) {
                return element("tag", tag);
            }
        }).toArray(Element.class);
    }
}

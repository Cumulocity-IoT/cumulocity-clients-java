package com.cumulocity.agent.packaging;

import com.cumulocity.agent.packaging.image.ImageBuilder;
import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import com.cumulocity.agent.packaging.microservice.buildx.BuildxClient;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.cumulocity.agent.packaging.TargetPlatform.AMD64;
import static com.cumulocity.agent.packaging.TargetPlatform.ARM64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeployMojoTest {

    @Mock
    BuildxClient buildxClient;

    @Mock
    MicroserviceDockerClient dockerClient;

    @Mock
    MavenProject project;

    @Mock
    MavenSession mavenSession;

    @Mock
    ImageBuilder imageBuilder;

    @Spy
    @InjectMocks
    DeployMojo deployMojo;

    @BeforeEach
    void setUp() {
        deployMojo.name = "service";
        deployMojo.registry = "registry.example.com/team";
        deployMojo.dockerBuildArchs = "amd64,arm64";

        lenient().when(project.getVersion()).thenReturn("1.0.0");
        lenient().when(mavenSession.getSettings()).thenReturn(new Settings());
        lenient().doReturn(imageBuilder).when(deployMojo).imageBuilder();
    }

    @Test
    void shouldPushImageToRegistry() throws Exception {
        deployMojo.execute();

        ArgumentCaptor<DockerImage> target = ArgumentCaptor.forClass(DockerImage.class);
        verify(imageBuilder).push(eq(List.of(AMD64, ARM64)), target.capture());
        assertEquals("registry.example.com/team/service", target.getValue().toString());
    }

    @Test
    void shouldSkipWithoutRegistry() throws Exception {
        deployMojo.registry = null;

        deployMojo.execute();

        verify(imageBuilder, never()).push(any(), any());
        verifyNoInteractions(buildxClient, dockerClient);
    }

    @Test
    void shouldSkipWhenContainerIsSkipped() throws Exception {
        deployMojo.containerSkip = true;

        deployMojo.execute();

        verify(imageBuilder, never()).push(any(), any());
    }
}

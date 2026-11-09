package com.cumulocity.agent.packaging.image;

import com.cumulocity.agent.packaging.DockerImage;
import com.cumulocity.agent.packaging.TargetPlatform;
import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthConfigurations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cumulocity.agent.packaging.TargetPlatform.AMD64;
import static com.cumulocity.agent.packaging.TargetPlatform.ARM64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DaemonImageBuilderTest {

    private static final Set<String> TAGS = Set.of("service:1.0.0", "service:latest");

    @Mock
    MicroserviceDockerClient dockerClient;

    ImageBuildSettings settings;
    final List<PackagedImage> packaged = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        settings = ImageBuildSettings.builder()
                .imageName("service")
                .version("1.0.0")
                .dockerWorkDir(new File("/work"))
                .buildDir(new File("/target"))
                .buildArg("HTTP_PROXY", "http://proxy:3128")
                .timeoutSeconds(42)
                .noCache(true)
                .imageWaitTimeoutSeconds(7)
                .deleteImage(true)
                .baseImage("alpine:3")
                .dockerCli("docker")
                .builderNamePrefix("c8y-service")
                .build();
        lenient().doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(1);
            out.write("image-tar".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(dockerClient).saveDockerImage(any(), any());
    }

    @Test
    void shouldBuildOneImageAndZipPerPlatform() throws Exception {
        new DaemonImageBuilder(dockerClient, settings).build(List.of(AMD64, ARM64), packaged::add);

        InOrder order = inOrder(dockerClient);
        for (TargetPlatform platform : List.of(AMD64, ARM64)) {
            order.verify(dockerClient).buildDockerImage(eq("/work"), eq(TAGS), eq(Map.of("HTTP_PROXY", "http://proxy:3128")), eq(platform.platform()),
                    eq(null), eq(42), any(), eq(true));
            order.verify(dockerClient).waitForImageInRegistry("service:1.0.0", 7);
            order.verify(dockerClient).deleteAll("service:1.0.0", true);
        }

        assertEquals(2, packaged.size());
        assertEquals(List.of(AMD64), packaged.get(0).getPlatforms());
        assertNull(packaged.get(0).getFileNamePlatform(), "amd64 uses the default file name");
        assertEquals(List.of(ARM64), packaged.get(1).getPlatforms());
        assertEquals(ARM64, packaged.get(1).getFileNamePlatform());
        assertEquals("image-tar", write(packaged.get(1)));
    }

    @Test
    void shouldUseDefaultFileNameForSinglePlatform() throws Exception {
        new DaemonImageBuilder(dockerClient, settings).build(List.of(ARM64), packaged::add);

        assertNull(packaged.get(0).getFileNamePlatform());
    }

    @Test
    void shouldKeepImageWithoutPackager() throws Exception {
        new DaemonImageBuilder(dockerClient, settings).build(List.of(AMD64), null);

        verify(dockerClient).buildDockerImage(anyString(), any(), anyMap(), eq(AMD64.platform()), any(), anyInt(), any(), anyBoolean());
        verify(dockerClient, never()).saveDockerImage(any(), any());
        verify(dockerClient, never()).deleteAll(any(), anyBoolean());
    }

    @Test
    void shouldKeepImageWhenConfigured() throws Exception {
        new DaemonImageBuilder(dockerClient, settings.toBuilder().deleteImage(false).build()).build(List.of(AMD64), packaged::add);

        assertEquals(1, packaged.size());
        verify(dockerClient, never()).deleteAll(any(), anyBoolean());
    }

    @Test
    void shouldPassPullCredentials() throws Exception {
        ImageBuildSettings withCredentials = settings.toBuilder()
                .baseImage("test.registry/image:1")
                .pullRegistryUser("user")
                .pullRegistryPass("pass")
                .build();

        new DaemonImageBuilder(dockerClient, withCredentials).build(List.of(AMD64), null);

        ArgumentCaptor<AuthConfigurations> auth = ArgumentCaptor.forClass(AuthConfigurations.class);
        verify(dockerClient).buildDockerImage(anyString(), any(), anyMap(), anyString(), any(), anyInt(), auth.capture(), anyBoolean());
        AuthConfig config = auth.getValue().getConfigs().get("https://test.registry");
        assertNotNull(config, auth.getValue().getConfigs().toString());
        assertEquals("user", config.getUsername());
        assertEquals("pass", config.getPassword());
    }

    @Test
    void shouldPushImageFromDaemon() {
        new DaemonImageBuilder(dockerClient, settings).push(List.of(AMD64, ARM64), DockerImage.ofName("service").withRegistry("registry.example.com/team"));

        verify(dockerClient).tagImage("service:latest", "registry.example.com/team/service:latest", "latest");
        verify(dockerClient).pushImage("registry.example.com/team/service:latest");
        verify(dockerClient).tagImage("service:1.0.0", "registry.example.com/team/service:1.0.0", "1.0.0");
        verify(dockerClient).pushImage("registry.example.com/team/service:1.0.0");
        verify(dockerClient).deleteAll("service", true);
        verify(dockerClient).deleteAll("registry.example.com/team/service", true);
    }

    private static String write(PackagedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.getWriter().write(out);
        return out.toString(StandardCharsets.UTF_8);
    }
}

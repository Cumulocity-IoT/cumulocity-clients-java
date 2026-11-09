package com.cumulocity.agent.packaging.image;

import com.cumulocity.agent.packaging.DockerImage;
import com.cumulocity.agent.packaging.microservice.buildx.BuildRequest;
import com.cumulocity.agent.packaging.microservice.buildx.BuildxClient;
import com.cumulocity.agent.packaging.microservice.buildx.DockerCliContext;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.cumulocity.agent.packaging.TargetPlatform.AMD64;
import static com.cumulocity.agent.packaging.TargetPlatform.ARM64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildxImageBuilderTest {

    private static final String TEMPORARY_BUILDER = "c8y-service-1234";
    private static final DockerImage TARGET = DockerImage.ofName("service").withRegistry("registry.example.com/team");
    private static final List<String> PUSHED_TAGS = List.of("registry.example.com/team/service:1.0.0", "registry.example.com/team/service:latest");

    @Mock
    BuildxClient client;

    @TempDir
    File build;

    final FakeBuildx fakeBuildx = new FakeBuildx("image-tar");
    final List<PackagedImage> packaged = new ArrayList<>();
    final AtomicInteger buildContextPrepared = new AtomicInteger();
    ImageBuildSettings settings;

    @BeforeEach
    void setUp() throws Exception {
        File dockerWorkDir = new File(build, "docker-work");
        settings = ImageBuildSettings.builder()
                .imageName("service")
                .version("1.0.0")
                .dockerWorkDir(dockerWorkDir)
                .buildDir(build)
                .timeoutSeconds(42)
                .noCache(true)
                .baseImage("alpine:3")
                .dockerCli("docker")
                .builderNamePrefix("c8y-service")
                .build();
        lenient().when(client.version(any())).thenReturn(Optional.of("github.com/docker/buildx v0.36.1 1d8dde89"));
        lenient().when(client.selectBuilder(any(), anyList())).thenReturn(TEMPORARY_BUILDER);
        lenient().when(client.platforms(any(), anyString())).thenReturn(Set.of("linux/amd64", "linux/arm64"));
        lenient().doAnswer(fakeBuildx.answer()).when(client).build(any(), any());
    }

    @Test
    void shouldBuildSinglePlatformImageIntoZip() throws Exception {
        builder(settings).build(List.of(ARM64), packaged::add);

        List<BuildRequest> requests = captureBuildRequests();
        assertEquals(1, requests.size(), "no export before the zip is written");
        BuildRequest image = requests.get(0);
        assertEquals(List.of(ARM64), image.getPlatforms());
        assertEquals(TEMPORARY_BUILDER, image.getBuilder());
        assertEquals(settings.getDockerWorkDir(), image.getContext());
        assertTrue(image.isNoCache());
        assertEquals("type=oci,tar=false,dest=" + layoutDir().getAbsolutePath(), image.getOutput());

        assertEquals(1, packaged.size());
        assertEquals(List.of(ARM64), packaged.get(0).getPlatforms());
        assertNull(packaged.get(0).getFileNamePlatform(), "one zip with the default name");
        assertEquals("image-tar", write(packaged.get(0)));

        BuildRequest export = captureBuildRequests().get(1);
        assertEquals("type=docker,name=service:1.0.0,dest=-", export.getOutput(), "docker save format for one platform");
        assertEquals(Map.of("microservice", layoutReference()), export.getBuildContexts());
        assertTrue(Files.readString(new File(export.getContext(), "Dockerfile").toPath()).startsWith("FROM microservice"));
        assertFalse(export.isNoCache());
    }

    @Test
    void shouldBuildMultiPlatformImageAndCheckJavaVersion() throws Exception {
        builder(settings).build(List.of(ARM64, AMD64), packaged::add);
        write(packaged.get(0));

        List<BuildRequest> requests = captureBuildRequests();
        assertEquals(3, requests.size(), "build, Java version check, export");
        BuildRequest check = requests.get(1);
        assertEquals(List.of(ARM64, AMD64), check.getPlatforms());
        assertEquals(Map.of("microservice", layoutReference()), check.getBuildContexts());
        assertTrue(check.getOutput().startsWith("type=local,dest="));
        assertEquals("type=oci,name=service:1.0.0,dest=-", requests.get(2).getOutput(), "OCI image index for several platforms");
        assertEquals(List.of(ARM64, AMD64), packaged.get(0).getPlatforms());
        assertNull(packaged.get(0).getFileNamePlatform());
    }

    @Test
    void shouldPackageDespiteJavaVersionMismatchByDefault() throws Exception {
        fakeBuildx.javaVersion(ARM64, "openjdk version \"17.0.20\" 2026-07-15");

        builder(settings).build(List.of(AMD64, ARM64), packaged::add);

        assertEquals(1, packaged.size());
    }

    @Test
    void shouldNotPackageOrPushOnJavaVersionMismatchWhenConfigured() throws Exception {
        fakeBuildx.javaVersion(ARM64, "openjdk version \"17.0.20\" 2026-07-15");
        ImageBuilder builder = builder(settings.toBuilder().failOnJavaVersionMismatch(true).build());

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> builder.build(List.of(AMD64, ARM64), packaged::add));

        assertTrue(exception.getMessage().contains("different Java versions"), exception.getMessage());
        assertTrue(packaged.isEmpty());
        assertThrows(MojoExecutionException.class, () -> builder.push(List.of(AMD64, ARM64), TARGET),
                "push does not reuse the image that failed the check");
        assertTrue(captureBuildRequests().stream().noneMatch(request -> "type=registry".equals(request.getOutput())));
    }

    @Test
    void shouldNotPushPackagedImageWithJavaVersionMismatchWhenConfigured() throws Exception {
        fakeBuildx.javaVersion(ARM64, "openjdk version \"17.0.20\" 2026-07-15");
        // package only warns about the mismatch
        builder(settings).build(List.of(AMD64, ARM64), packaged::add);
        clearInvocations(client);

        ImageBuilder failOnMismatch = builder(settings.toBuilder().failOnJavaVersionMismatch(true).build());
        assertThrows(MojoExecutionException.class, () -> failOnMismatch.push(List.of(AMD64, ARM64), TARGET));

        assertEquals(1, buildContextPrepared.get(), "the image is built and checked again");
        assertTrue(captureBuildRequests().stream().noneMatch(request -> "type=registry".equals(request.getOutput())));
    }

    @Test
    void shouldLoadSinglePlatformImageWithoutPackager() throws Exception {
        builder(settings).build(List.of(AMD64), null);

        BuildRequest load = captureBuildRequests().get(1);
        assertTrue(load.isLoad());
        assertEquals(List.of("service:1.0.0", "service:latest"), load.getTags());
    }

    @Test
    void shouldLoadMultiPlatformImageOnlyWithContainerdImageStore() throws Exception {
        builder(settings).build(List.of(AMD64, ARM64), null);
        assertEquals(2, captureBuildRequests().size(), "build and check, no load with the classic image store");

        clearInvocations(client);
        when(client.selectBuilder(any(), anyList())).thenReturn(BuildxClient.DEFAULT_BUILDER);
        builder(settings).build(List.of(AMD64, ARM64), null);
        assertTrue(captureBuildRequests().get(2).isLoad());
    }

    @Test
    void shouldPushImageBuiltByPackageGoal() throws Exception {
        builder(settings).build(List.of(AMD64, ARM64), packaged::add);
        clearInvocations(client);

        builder(settings).push(List.of(AMD64, ARM64), TARGET);

        List<BuildRequest> requests = captureBuildRequests();
        assertEquals(1, requests.size(), "no second build");
        assertEquals("type=registry", requests.get(0).getOutput());
        assertEquals(PUSHED_TAGS, requests.get(0).getTags());
        assertEquals(Map.of("microservice", layoutReference()), requests.get(0).getBuildContexts());
        assertEquals(0, buildContextPrepared.get());
        verify(client, never()).removeImages(any(), any());
    }

    @Test
    void shouldBuildBeforePushWithoutPackageGoal() throws Exception {
        builder(settings).build(List.of(AMD64), packaged::add);
        clearInvocations(client);

        // other platforms than the image of the package goal
        builder(settings).push(List.of(AMD64, ARM64), TARGET);

        List<BuildRequest> requests = captureBuildRequests();
        assertEquals(3, requests.size(), "build, Java version check, push");
        assertEquals(settings.getDockerWorkDir(), requests.get(0).getContext());
        assertEquals("type=registry", requests.get(2).getOutput());
        assertEquals(1, buildContextPrepared.get());
    }

    @Test
    void shouldRemovePushedImagesFromDaemonWithDefaultBuilder() throws Exception {
        when(client.selectBuilder(any(), anyList())).thenReturn(BuildxClient.DEFAULT_BUILDER);

        builder(settings).push(List.of(AMD64), TARGET);

        verify(client).removeImages(any(), eq(PUSHED_TAGS));
    }

    @Test
    void shouldFailWithInstructionWhenBuildxIsMissing() {
        when(client.version(any())).thenReturn(Optional.empty());

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> builder(settings).build(List.of(AMD64), packaged::add));

        assertTrue(exception.getMessage().contains("docker-buildx-plugin"), exception.getMessage());
        verifyNoBuild();
    }

    @Test
    void shouldFailWhenBuildxIsTooOld() {
        when(client.version(any())).thenReturn(Optional.of("github.com/docker/buildx 0.9.1+ds1 abcdef"));

        assertThrows(MojoExecutionException.class, () -> builder(settings).build(List.of(AMD64), packaged::add));
        verifyNoBuild();
    }

    @Test
    void shouldRejectCustomNetworkForSeveralPlatforms() throws Exception {
        ImageBuilder builder = builder(settings.toBuilder().network("my-network").build());

        assertThrows(MojoExecutionException.class, () -> builder.build(List.of(AMD64, ARM64), packaged::add));
        verifyNoBuild();

        builder.build(List.of(AMD64), packaged::add);
        assertEquals("my-network", captureBuildRequests().get(0).getNetwork());
    }

    @Test
    void shouldOnlyWarnWhenEmulationIsMissing() throws Exception {
        when(client.platforms(any(), anyString())).thenReturn(Set.of("linux/amd64", "linux/386"));

        builder(settings).build(List.of(AMD64, ARM64), packaged::add);

        assertEquals(1, packaged.size());
    }

    @Test
    void shouldParseBuildxVersion() {
        assertArrayEquals(new int[]{0, 36}, BuildxImageBuilder.parseVersion("github.com/docker/buildx v0.36.1 1d8dde89"));
        assertArrayEquals(new int[]{0, 9}, BuildxImageBuilder.parseVersion("github.com/docker/buildx 0.9.1+ds1 abcdef"));
        assertNull(BuildxImageBuilder.parseVersion("unknown"));
    }

    @Test
    void shouldKeepBuildxStateInBuildDirectory() throws Exception {
        builder(settings).build(List.of(AMD64), packaged::add);

        DockerCliContext context = captureContext();
        assertEquals(new File(build, "buildx"), context.getBuildxConfigDir());
        assertEquals("c8y-service", context.getBuilderNamePrefix());
        assertEquals("docker", context.getDockerCli());
        assertEquals(42, context.getTimeoutSeconds());
        assertNull(context.getDockerConfigDir(), "the user's docker login is used without configured credentials");
    }

    @Test
    void shouldPassPullCredentialsInTemporaryDockerConfig() throws Exception {
        ImageBuildSettings withCredentials = settings.toBuilder()
                .baseImage("test.registry/image:1")
                .pullRegistryUrl("https://not.test.registry/path/")
                .pullRegistryUser("user")
                .pullRegistryPass("pass")
                .build();
        AtomicReference<JsonNode> configDuringBuild = new AtomicReference<>();
        AtomicReference<File> configDir = new AtomicReference<>();
        doAnswer(invocation -> {
            DockerCliContext context = invocation.getArgument(0);
            configDir.set(context.getDockerConfigDir());
            configDuringBuild.set(new ObjectMapper().readTree(new File(configDir.get(), "config.json")));
            return fakeBuildx.answer().answer(invocation);
        }).when(client).build(any(), any());

        builder(withCredentials).build(List.of(AMD64), null);

        String expectedAuth = Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedAuth, configDuringBuild.get().get("auths").get("not.test.registry").get("auth").asText());
        assertFalse(configDir.get().exists(), "temporary docker config is deleted after the build");
    }

    private ImageBuilder builder(ImageBuildSettings settings) {
        return new BuildxImageBuilder(client, settings, buildContextPrepared::incrementAndGet);
    }

    private File layoutDir() {
        return new File(build, "buildx-image/image");
    }

    private String layoutReference() {
        return "oci-layout://" + layoutDir().getAbsolutePath() + "@" + FakeBuildx.IMAGE_INDEX_DIGEST;
    }

    private List<BuildRequest> captureBuildRequests() throws MojoExecutionException {
        ArgumentCaptor<BuildRequest> request = ArgumentCaptor.forClass(BuildRequest.class);
        verify(client, atLeastOnce()).build(any(), request.capture());
        return request.getAllValues();
    }

    private DockerCliContext captureContext() throws MojoExecutionException {
        ArgumentCaptor<DockerCliContext> context = ArgumentCaptor.forClass(DockerCliContext.class);
        verify(client, atLeastOnce()).build(context.capture(), any());
        return context.getValue();
    }

    private void verifyNoBuild() {
        try {
            verify(client, never()).build(any(), any());
        } catch (MojoExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String write(PackagedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.getWriter().write(out);
        return out.toString(StandardCharsets.UTF_8);
    }
}

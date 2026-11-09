package com.cumulocity.agent.packaging;


import com.cumulocity.agent.packaging.image.DaemonImageBuilder;
import com.cumulocity.agent.packaging.image.ImageBuildSettings;
import com.cumulocity.agent.packaging.image.ImageBuilder;
import com.cumulocity.agent.packaging.image.ImagePackager;
import com.cumulocity.agent.packaging.image.PackagedImage;
import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.cumulocity.agent.packaging.BaseMicroserviceMojo.OTEL_JAVA_AGENT_DOWNLOAD_URL;
import static com.cumulocity.agent.packaging.TargetPlatform.AMD64;
import static com.cumulocity.agent.packaging.TargetPlatform.ARM64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class PackageMojoTest {

    public static final String MANIFEST_FILENAME = "cumulocity.json";
    public static final String MANIFEST_RELATIVE_FILEPATH = "src/test/resources/" + MANIFEST_FILENAME;
    public static final String CONFIGDIR = "src/test/resources/dummyConf";
    public static final String SRC_DOCKER_DIR = "src/test/resources/docker";

    public static final String ARTIFACT_NAME = "dummy";
    public static final String DUMMY_JAR_FILENAME = ARTIFACT_NAME + ".jar";
    public static final String PACKAGE_DIR = "/projects/dummy";
    public static final String TEST_VERSION = "99.42.123";
    public static final String IMAGE_TAR_CONTENT = "image-tar-content";

    @Mock
    MavenResourcesFiltering mavenResourcesFiltering;

    @Mock
    MavenProject mavenProject;

    @Mock
    MavenSession mavenSession;

    @Mock
    MojoExecution mojoExecution;

    @Mock
    Artifact artifact;

    @Mock
    MicroserviceDockerClient dockerClient;

    @Mock
    ImageBuilder imageBuilder;

    List<PackagedImage> packagedImages;

    @Spy
    @InjectMocks
    PackageMojo packageMojo;

    @TempDir
    File tempDir;

    File filteredResources;

    File build;
    File resources;
    File dummyArtifact;
    File dummyConfigDir = new File(CONFIGDIR);
    File srcDockerDir = new File(SRC_DOCKER_DIR);
    File manifestFile = new File(MANIFEST_RELATIVE_FILEPATH);

    @SneakyThrows
    @BeforeEach
    public void init() {
        log.info("Initializing files");
        initializeMockedResources();
        mockArtifact();
        mockMavenSession();
        mockVersion(TEST_VERSION);
        configurePojo();
        mockImageBuilder();
    }

    @SneakyThrows
    @Test
    public void testZipWithDefaultName() {
        packageMojo.execute();

        verify(imageBuilder).build(eq(List.of(AMD64)), notNull());
        validateZipFile(getExpectedZipFilePath(null), List.of(AMD64));
    }

    @SneakyThrows
    @Test
    public void testZipNameWithPlatformOfImage() {
        packagedImages = List.of(new PackagedImage(List.of(AMD64), null, this::writeImage),
                new PackagedImage(List.of(ARM64), ARM64, this::writeImage));

        packageMojo.execute();

        validateZipFile(getExpectedZipFilePath(null), List.of(AMD64));
        validateZipFile(getExpectedZipFilePath("linux-arm64"), List.of(ARM64));
    }

    @SneakyThrows
    @Test
    public void testZipOfMultiPlatformImage() {
        packageMojo.dockerBuildArchs = "amd64,arm64";
        packagedImages = List.of(new PackagedImage(List.of(AMD64, ARM64), null, this::writeImage));

        packageMojo.execute();

        verify(imageBuilder).build(eq(List.of(AMD64, ARM64)), notNull());
        validateZipFile(getExpectedZipFilePath(null), List.of(AMD64, ARM64));
    }

    @SneakyThrows
    @Test
    public void testIncompleteZipIsDeleted() {
        packagedImages = List.of(new PackagedImage(List.of(AMD64), null, out -> {
            throw new IOException("export failed");
        }));

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> packageMojo.execute());

        assertTrue(exception.getMessage().contains("Cannot create microservice package"), exception.getMessage());
        assertFalse(Files.exists(getExpectedZipFilePath(null)));
    }

    @SneakyThrows
    @Test
    public void testSkipMicroservicePackage() {
        packageMojo.skipMicroservicePackage = true;

        packageMojo.execute();

        verify(imageBuilder).build(List.of(AMD64), null);
        assertFalse(Files.exists(getExpectedZipFilePath(null)));
    }

    @SneakyThrows
    @Test
    public void testPlatformsFromMavenConfiguration() {
        //aliases are normalized, duplicates removed, order kept
        packageMojo.dockerBuildArchs = "arm64v8, amd64,linux/arm64";

        packageMojo.execute();

        verify(imageBuilder).build(eq(List.of(ARM64, AMD64)), notNull());
    }

    @SneakyThrows
    @Test
    public void testOtherPlatformIsBuiltAsGiven() {
        packageMojo.dockerBuildArchs = "linux/amd64, I386";

        packageMojo.execute();

        verify(imageBuilder).build(eq(TargetPlatform.parse(List.of("linux/amd64", "i386"))), notNull());
    }

    @SneakyThrows
    @Test
    public void testDockerBuildSpec() {
        //When I modify the docker build spec in the cumulocity.json for two architectures
        JsonNode jsonManifest = writeBuildSpec("amd64", "arm64");
        packagedImages = List.of(new PackagedImage(List.of(AMD64, ARM64), null, this::writeImage));

        packageMojo.execute();

        verify(imageBuilder).build(eq(List.of(AMD64, ARM64)), notNull());
        validateZipFile(getExpectedZipFilePath(null), List.of(AMD64, ARM64), jsonManifest);
    }

    @SneakyThrows
    @Test
    public void testMavenConfigurationOverridesBuildSpec() {
        writeBuildSpec("amd64", "arm64");
        packageMojo.dockerBuildArchs = "amd64";

        packageMojo.execute();

        verify(imageBuilder).build(eq(List.of(AMD64)), notNull());
    }

    @Test
    public void testDockerDaemonIsUsedByDefault() {
        doCallRealMethod().when(packageMojo).imageBuilder();
        assertInstanceOf(DaemonImageBuilder.class, packageMojo.imageBuilder());
    }

    @Test
    public void testImageBuildSettings() {
        packageMojo.dockerBuildNetwork = "host";
        packageMojo.deleteImage = false;
        packageMojo.registryPass = "secret";

        ImageBuildSettings settings = packageMojo.imageBuildSettings().build();

        assertEquals(ARTIFACT_NAME, settings.getImageName());
        assertEquals(TEST_VERSION, settings.getVersion());
        assertEquals(getExpectedTags(), settings.tags());
        assertEquals(resources, settings.getDockerWorkDir());
        assertEquals(build, settings.getBuildDir());
        assertEquals("host", settings.getNetwork());
        assertEquals(360, settings.getTimeoutSeconds());
        assertTrue(settings.isNoCache(), "no build cache by default");
        assertFalse(settings.isDeleteImage());
        assertEquals(360, settings.getImageWaitTimeoutSeconds());
        assertEquals("alpine:3", settings.getBaseImage());
        assertFalse(settings.toString().contains("secret"), "the password is not printed");
    }

    @SneakyThrows
    private JsonNode writeBuildSpec(String... architectures) {
        DockerBuildSpec dockerBuildSpec = new DockerBuildSpec();
        dockerBuildSpec.setTargetBuildArchitectures(Lists.newArrayList(architectures));

        ObjectMapper mapper = new ObjectMapper();
        Path manifestPathTarget = Paths.get(tempDir.getAbsolutePath(), "filtered-resources", MANIFEST_FILENAME);
        ObjectNode jsonManifest = mapper.readValue(new File(manifestPathTarget.toUri()), ObjectNode.class);
        jsonManifest.putPOJO("buildSpec", dockerBuildSpec);

        try (FileWriter fileWriter = new FileWriter(manifestPathTarget.toFile())) {
            mapper.writeValue(fileWriter, jsonManifest);
        }
        return mapper.readTree(manifestPathTarget.toFile());
    }

    @SneakyThrows
    private void validateZipFile(Path zipFilePath, List<TargetPlatform> platforms) {
        validateZipFile(zipFilePath, platforms, getOriginalManifestJson());
    }

    @SneakyThrows
    private void validateZipFile(Path zipFilePath, List<TargetPlatform> platforms, JsonNode originalManifest) {
        assertTrue(Files.exists(zipFilePath), "zip file " + zipFilePath + " exists");

        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            ZipEntry imageEntry = zipFile.getEntry("image.tar");
            assertTrue(Objects.nonNull(imageEntry), "image tar not in microservice.zip");
            assertEquals(IMAGE_TAR_CONTENT, new String(zipFile.getInputStream(imageEntry).readAllBytes(), StandardCharsets.UTF_8));

            ZipEntry manifestFileEntry = zipFile.getEntry("cumulocity.json");
            assertTrue(Objects.nonNull(manifestFileEntry), "manifest entry is null");

            //Let's check if the docker build info was added and if it contains valid information
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode manifest = objectMapper.readTree(zipFile.getInputStream(manifestFileEntry));

            DockerBuildInfo dockerBuildInfo = objectMapper.convertValue(manifest.get("dockerBuildInfo"), DockerBuildInfo.class);
            assertEquals(platforms.get(0).platform(), dockerBuildInfo.getImageArch(), "Docker build arch mismatch");
            List<String> expectedArchitectures = new ArrayList<>();
            platforms.forEach(platform -> expectedArchitectures.add(platform.platform()));
            assertEquals(expectedArchitectures, dockerBuildInfo.getImageArchitectures());
            assertEquals(SystemUtils.OS_NAME, dockerBuildInfo.getHostOS(), "Host information is valid");
            assertTrue(Math.abs(System.currentTimeMillis() - dockerBuildInfo.getBuildDate().getTime()) < 60000, "The build date in the cumulocity.json seems to be wrong");

            assertTrue(manifest instanceof ObjectNode);

            //If we remove the docker build info fragment, the cumulocity.json must be equivalent to original file again.
            ObjectNode manifestWithoutDockerBuildinfo = ((ObjectNode) manifest).remove(Lists.newArrayList("dockerBuildInfo"));
            assertEquals(originalManifest, manifestWithoutDockerBuildinfo, "There seem to be extra mutations in the json by package");
        }
    }

    private Path getExpectedZipFilePath(String suffix) {
        String expectedZipFileName = suffix == null
                ? String.format("%s-%s.zip", ARTIFACT_NAME, TEST_VERSION)
                : String.format("%s-%s-%s.zip", ARTIFACT_NAME, TEST_VERSION, suffix);
        return Paths.get(build.getAbsolutePath(), expectedZipFileName);
    }

    private void writeImage(OutputStream out) throws IOException {
        out.write(IMAGE_TAR_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private void configurePojo() throws IllegalAccessException {
        packageMojo.encoding = "UTF-8";
        packageMojo.skip = false;
        packageMojo.rpmSkip = true;
        packageMojo.containerSkip = false;
        packageMojo.build = build;
        packageMojo.name = ARTIFACT_NAME;
        packageMojo.image = ARTIFACT_NAME;

        packageMojo.dockerWorkDir = resources;
        packageMojo.directory = PACKAGE_DIR;
        packageMojo.srcConfigurationDir = dummyConfigDir;
        packageMojo.srcDockerDir = srcDockerDir;

        packageMojo.javaRuntime = System.getProperty("java.version");
        packageMojo.baseImage = "alpine:3";
        packageMojo.manifestFile = manifestFile;
        packageMojo.otelJavaAgentDownloadUrl = OTEL_JAVA_AGENT_DOWNLOAD_URL;
        packageMojo.otelJavaAgentInclude = "false";

        //the following fields are private, let's use reflection :)
        FieldUtils.writeField(packageMojo, "heap", new Memory("512M", "4G"), true);
        FieldUtils.writeField(packageMojo, "metaspace", new Memory(), true);
        FieldUtils.writeField(packageMojo, "perm", new Memory(), true);
        FieldUtils.writeField(packageMojo, "arguments", Lists.newArrayList(), true);
    }

    private void initializeMockedResources() throws IOException {

        Path filteredResourcePath = Paths.get(tempDir.getAbsolutePath(), "filtered-resources");
        filteredResources = filteredResourcePath.toFile();

        log.info("Creating temp directory {}", filteredResources.getAbsoluteFile());
        Files.createDirectories(Paths.get(filteredResources.getAbsolutePath()));

        Path source = Paths.get(MANIFEST_RELATIVE_FILEPATH);
        Path target = Paths.get(filteredResources.getAbsolutePath() + "/" + MANIFEST_FILENAME);
        log.info("Copying {} -> {}", source, target);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        build = tempDir.getAbsoluteFile();
        log.info("Test Build folder: {}", build);

        Path dockerWorkPath = Paths.get(tempDir.getAbsolutePath(), "docker-work");
        Files.createDirectories(dockerWorkPath);
        resources = dockerWorkPath.toFile();
        log.info("Test resources folder: {}", resources);

        createDummyJARFile(build);
    }

    private void mockMavenSession() {
        lenient().when(mavenSession.getSettings()).thenReturn(new Settings());

        Plugin plugin = mock(Plugin.class);
        lenient().when(plugin.getId()).thenReturn("mocked package plugin 1.23.45");

        lenient().when(mojoExecution.getPlugin()).thenReturn(plugin);
    }

    /**
     * The mocked image builder passes {@link #packagedImages} to the packager, like a real build would.
     */
    @SneakyThrows
    private void mockImageBuilder() {
        packagedImages = List.of(new PackagedImage(List.of(AMD64), null, this::writeImage));
        lenient().doReturn(imageBuilder).when(packageMojo).imageBuilder();
        lenient().doAnswer(invocation -> {
            ImagePackager packager = invocation.getArgument(1);
            if (packager != null) {
                for (PackagedImage image : packagedImages) {
                    packager.write(image);
                }
            }
            return null;
        }).when(imageBuilder).build(any(), any());
    }

    @SneakyThrows
    private void createDummyJARFile(File parent) {
        dummyArtifact = new File(parent, DUMMY_JAR_FILENAME);
        boolean success = dummyArtifact.createNewFile();
        log.info("Dummy JAR created at {}:{}", dummyArtifact.getAbsoluteFile(), success);
    }

    private void mockArtifact() {
        lenient().when(artifact.getFile()).thenReturn(dummyArtifact);
        lenient().when(mavenProject.getArtifact()).thenReturn(artifact);
        lenient().when(mavenProject.getArtifactId()).thenReturn(ARTIFACT_NAME);
    }

    private void mockVersion(String version) {
        lenient().when(mavenProject.getVersion()).thenReturn(version);
    }

    @SneakyThrows
    private JsonNode getOriginalManifestJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(new File(MANIFEST_RELATIVE_FILEPATH));
    }

    private List<String> getExpectedTags() {
        return List.of(ARTIFACT_NAME + ":" + TEST_VERSION, ARTIFACT_NAME + ":latest");
    }
}

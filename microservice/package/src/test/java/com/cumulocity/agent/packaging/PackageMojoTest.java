package com.cumulocity.agent.packaging;


import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.model.AuthConfigurations;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static com.cumulocity.agent.packaging.BaseMicroserviceMojo.OTEL_JAVA_AGENT_DOWNLOAD_URL;

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
        mockDockerImageFileForArchitectures("linux/amd64", "linux/arm64v8", "i386");
        mockVersion(TEST_VERSION);
        configurePojo();
    }

    @SneakyThrows
    @Test
    public void testContainerPackageDefault() {
        String expectedBuildArch = "linux/amd64";

        //As a default, running the packaging plugin does package a container
        packageMojo.execute();

        //Validate Docker client invocations
        verify(dockerClient, times(1)).buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(), eq(expectedBuildArch), any(), any(), any());
        verify(dockerClient, times(1)).saveDockerImage(eq(ARTIFACT_NAME + ":" + TEST_VERSION), any());
        
        //and there is a zip file with the expected name for the microservice
        validateZipFileForArchitecture(expectedBuildArch);

        //and the container is deleted
        verify(dockerClient, times(1)).deleteAll(ARTIFACT_NAME + ":" + TEST_VERSION, true);

    }

    @SneakyThrows
    @Test
    public void testSingleCustomTargetBuildArch() {

        String expectedBuildArch = "linux/arm64v8";
        packageMojo.targetBuildArchs = expectedBuildArch;

        //As a default, running the packaging plugin does package a container
        packageMojo.execute();
        //Validate Docker client invocations
        verify(dockerClient, times(1)).buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(), eq(expectedBuildArch),any(), any(), any());
        verify(dockerClient, times(1)).saveDockerImage(eq(ARTIFACT_NAME + ":" + TEST_VERSION), any());

        //and there is a zip file with the expected name for the microservice
        validateZipFileForArchitecture(expectedBuildArch);

        //and the container is deleted
        verify(dockerClient, times(1)).deleteAll(ARTIFACT_NAME + ":" + TEST_VERSION, true);

    }

    @SneakyThrows
    @Test
    public void testContainerNoDelete() {
        String expectedBuildArch = "linux/amd64";

        //When I turn off docker image deletion
        packageMojo.deleteImage = false;
        //and I package
        packageMojo.execute();
        //Validate Docker client invocations
        verify(dockerClient, times(1)).buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(),eq(expectedBuildArch), any(), any(), any());
        verify(dockerClient, times(1)).saveDockerImage(eq(ARTIFACT_NAME + ":" + TEST_VERSION), any());

        //and there is a zip file with the expected name for the microservice
        validateZipFileForArchitecture(expectedBuildArch);

        //and the container is deleted
        verify(dockerClient, never()).deleteAll(ARTIFACT_NAME + ":" + TEST_VERSION, true);

    }

    @SneakyThrows
    @Test
    public void testMultipleCustomTargetBuildArch() {

        String[] expectedBuildArch = new String[]{"linux/arm64v8", "linux/amd64", "i386"};
        packageMojo.targetBuildArchs = StringUtils.join(expectedBuildArch, ","); //construct comma-based argument

        //As a default, running the packaging plugin does package a container
        packageMojo.execute();

        //three docker images should have been built
        //Validate Docker client invocations and if there are a zip files with the expected name for each architecture
        for (String expected : expectedBuildArch) {
            validateZipFileForArchitecture(expected);
            verify(dockerClient, times(1)).buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(), eq(expected),any(), any(), any());
        }

        //3 Images saved
        verify(dockerClient, times(3)).saveDockerImage(eq(ARTIFACT_NAME + ":" + TEST_VERSION), any());
        verify(dockerClient, times(3)).deleteAll(ARTIFACT_NAME + ":" + TEST_VERSION, true);


    }


    @SneakyThrows
    @Test
    public void testDockerBuildSpec() {
        //When I modify the docker build spec in the cumulocity.json for three architectures
        String[] targetArgs = new String[]{"C64", "QNX", "mainframe"};
        mockDockerImageFileForArchitectures(targetArgs);

        DockerBuildSpec dockerBuildSpec = new DockerBuildSpec();
        dockerBuildSpec.setTargetBuildArchitectures(Lists.newArrayList(targetArgs));

        ObjectMapper mapper = new ObjectMapper();
        Path manifestPathTarget = Paths.get(tempDir.getAbsolutePath(), "filtered-resources", MANIFEST_FILENAME);
        ObjectNode jsonManifest = mapper.readValue(new File(manifestPathTarget.toUri()), ObjectNode.class);
        jsonManifest.putPOJO("buildSpec", dockerBuildSpec);

        FileWriter fileWriter = new FileWriter(manifestPathTarget.toFile());
        mapper.writeValue(fileWriter, jsonManifest);

        //and I run package
        packageMojo.execute();

        //three docker images should have been built
        //Validate Docker client invocations and if there are a zip files with the expected name for each architecture
        for (String expected : dockerBuildSpec.getTargetBuildArchitectures()) {
            validateZipFileForArchitecture(expected, jsonManifest);
            verify(dockerClient, times(1)).buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(), eq(expected), any(), any(), any());
        }

        //3 Images saved
        verify(dockerClient, times(3)).saveDockerImage(eq(ARTIFACT_NAME + ":" + TEST_VERSION), any());

        //and containers are deleted again
        verify(dockerClient, times(3)).deleteAll(ARTIFACT_NAME + ":" + TEST_VERSION, true);

    }

    @Test
    public void pullRegistryShouldOverrideBaseImageInAuthSettings() throws Exception {
        String expectedBuildArch = "linux/amd64";

        //Add pull image configuration
        packageMojo.baseImage = "test.registry/image:1";
        packageMojo.registryUrl = "https://not.test.registry";
        packageMojo.registryUser = "user";
        packageMojo.registryPass = "pass";

        //As a default, running the packaging plugin does package a container
        packageMojo.execute();

        ArgumentCaptor<AuthConfigurations> authConfiguration = ArgumentCaptor.forClass(AuthConfigurations.class);

        //Validate Docker client invocations
        verify(dockerClient, times(1)).buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(), eq(expectedBuildArch), any(), any(), authConfiguration.capture());

        AuthConfigurations authConfig = authConfiguration.getValue();

        assertEquals(1, authConfig.getConfigs().size(), "Size of list of auth configurations.");
        assertTrue(authConfig.getConfigs().containsKey("https://not.test.registry"), "Contains auth config for registry: not.test.registry");
        assertEquals("user", authConfig.getConfigs().get("https://not.test.registry").getUsername());
        assertEquals("pass", authConfig.getConfigs().get("https://not.test.registry").getPassword());
    }

    @Test
    public void pullRegistryShouldCalculateBaseImageIfEmpty() throws Exception {
        String expectedBuildArch = "linux/amd64";

        //Add pull image configuration without registry
        packageMojo.baseImage = "test.registry/image:1";
        packageMojo.registryUser = "user";
        packageMojo.registryPass = "pass";

        //As a default, running the packaging plugin does package a container
        packageMojo.execute();

        ArgumentCaptor<AuthConfigurations> authConfiguration = ArgumentCaptor.forClass(AuthConfigurations.class);

        //Validate Docker client invocations
        verify(dockerClient, times(1))
                .buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(), eq(expectedBuildArch), any(), any(), authConfiguration.capture());

        AuthConfigurations authConfig = authConfiguration.getValue();

        assertEquals(1, authConfig.getConfigs().size(), "Size of list of auth configurations.");
        assertTrue(authConfig.getConfigs().containsKey("https://test.registry"), "Contains auth config for registry: test.registry");
        assertEquals("user", authConfig.getConfigs().get("https://test.registry").getUsername());
        assertEquals("pass", authConfig.getConfigs().get("https://test.registry").getPassword());
    }

    @Test
    public void noPullImageAuthConfigShouldBeAddedWhenUserPasswordNotSpecified() throws Exception {
        String expectedBuildArch = "linux/amd64";

        //Add pull image configuration without registry
        packageMojo.baseImage = "test.registry/image:1";
        packageMojo.registryUrl = "not.test.registry";

        //As a default, running the packaging plugin does package a container
        packageMojo.execute();

        ArgumentCaptor<AuthConfigurations> authConfiguration = ArgumentCaptor.forClass(AuthConfigurations.class);

        //Validate Docker client invocations
        verify(dockerClient, times(1)).buildDockerImage(eq(resources.toString()), eq(getExpectedTags()), any(), eq(expectedBuildArch), any(), any(), authConfiguration.capture());

        AuthConfigurations authConfig = authConfiguration.getValue();

        assertEquals(0, authConfig.getConfigs().size(), "Size of list of auth configurations.");
    }

    @SneakyThrows
    private void validateZipFileForArchitecture(String buildArch) {
        validateZipFileForArchitecture(buildArch, getOriginalManifestJson());
    }

    @SneakyThrows
    private void validateZipFileForArchitecture(String buildArch, JsonNode originalManifest)  {

        //first, check if there is a properly named zip file
        Path zipFilePath = getExpectedZipFilePath(buildArch);
        assertTrue(Files.exists(zipFilePath));

        //and the zip file contains a cumulocity.json and an image tar
        Map<String, String> zipArguments = Maps.newHashMap();
        zipArguments.put("create", "false");

        ZipFile zipFile = new ZipFile(zipFilePath.toFile());
        assertTrue(Objects.nonNull(zipFile.getEntry("image.tar")), "image tar not in microservice.zip");

        ZipEntry manifestFileEntry = zipFile.getEntry("cumulocity.json");
        assertTrue(Objects.nonNull(manifestFileEntry), "manifest entry is null");

        //Let's check if the docker build info was added and if it contains valid information
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode manifest = objectMapper.readTree(zipFile.getInputStream(manifestFileEntry));

        DockerBuildInfo dockerBuildInfo = objectMapper.convertValue(manifest.get("dockerBuildInfo"), DockerBuildInfo.class);
        assertEquals(buildArch, dockerBuildInfo.getImageArch(), "Docker build arch mismatch");
        assertEquals(SystemUtils.OS_NAME, dockerBuildInfo.getHostOS(), "Host information is valid");
        assertTrue(Math.abs(System.currentTimeMillis() - dockerBuildInfo.getBuildDate().getTime()) < 60000, "The build date in the cumulocity.json seems to be wrong");

        assertTrue(manifest instanceof ObjectNode);

        //Let us make sure there are no unwanted mutations to the cumulocity.json by the package plugin
        //If we remove the docker build info fragment, the cumulocity.json must be equivalent to original file again.
        ObjectNode manifestWithoutDockerBuildinfo = ((ObjectNode) originalManifest).remove(Lists.newArrayList("dockerBuildInfo"));
        assertEquals(originalManifest, manifestWithoutDockerBuildinfo, "There seem to be extra mutations in the json by package");

    }

    private Path getExpectedZipFilePath(String buildArch) {
        String expectedZipFileName;
        if (!buildArch.equals(DockerBuildSpec.DEFAULT_TARGET_DOCKER_IMAGE_PLATFORM)) {
            expectedZipFileName = String.format("%s-%s-%s.zip", ARTIFACT_NAME, TEST_VERSION, buildArch).replaceAll("/","-");
        } else {
            expectedZipFileName = String.format("%s-%s.zip", ARTIFACT_NAME, TEST_VERSION);
        }
        return Paths.get(build.getAbsolutePath(), expectedZipFileName);
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
        when(mavenSession.getSettings()).thenReturn(new Settings());

        Plugin plugin = mock(Plugin.class);
        when(plugin.getId()).thenReturn("mocked package plugin 1.23.45");

        when(mojoExecution.getPlugin()).thenReturn(plugin);
    }


    @SneakyThrows
    private void createDummyJARFile(File parent) {
        dummyArtifact = new File(parent, DUMMY_JAR_FILENAME);
        boolean success = dummyArtifact.createNewFile();
        log.info("Dummy JAR created at {}:{}", dummyArtifact.getAbsoluteFile(), success);
    }

    private void mockArtifact() {
        when(artifact.getFile()).thenReturn(dummyArtifact);
        when(mavenProject.getArtifact()).thenReturn(artifact);
    }

    private void mockVersion(String version) {
        when(mavenProject.getVersion()).thenReturn(version);
    }


    @SneakyThrows
    private void mockDockerImageFileForArchitectures(String... architectures) {
        for (String architecture : architectures) {
            Path imgMockPath = Paths.get(build.getAbsolutePath(), String.format("image-%s.tar", architecture).replaceAll("/","-"));
            Files.createFile(imgMockPath);
            log.info("Image mock {} created", imgMockPath);
        }
    }

    @SneakyThrows
    private JsonNode getOriginalManifestJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(new File(MANIFEST_RELATIVE_FILEPATH));
    }

    private Set<String> getExpectedTags() {
        return Sets.newHashSet(ARTIFACT_NAME + ":" + TEST_VERSION, ARTIFACT_NAME + ":latest");
    }


}
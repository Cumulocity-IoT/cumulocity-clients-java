package com.cumulocity.agent.packaging;

import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import com.cumulocity.model.DataSize;
import com.cumulocity.model.Resources;
import com.cumulocity.model.application.MicroserviceManifest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthConfigurations;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Proxy;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.cumulocity.agent.packaging.RpmDsl.configuration;
import static com.cumulocity.agent.packaging.RpmDsl.*;
import static java.nio.file.Files.createDirectories;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Slf4j
@Mojo(name = "package", defaultPhase = PACKAGE, requiresDependencyResolution = RUNTIME, threadSafe = false)
public class PackageMojo extends BaseMicroserviceMojo {

    public static final DataSize MEMORY_MINIMAL_LIMIT = DataSize.parse("178Mi");

    public static final String BUILD_SPEC_FRAGMENT="buildSpec";
    public static final String DOCKER_BUILD_INFO_FRAGMENT="dockerBuildInfo";
    public static final String MANIFEST_JSON_FILENAME = "cumulocity.json";
    public static final String DOCKER_IMAGE_TAR = "image.tar";

    @Component
    protected MicroserviceDockerClient dockerClient;

    @Parameter(defaultValue = "${basedir}/src/main/configuration/cumulocity.json")
    protected File manifestFile;

    @Parameter(property = "package.name", defaultValue = "${project.artifactId}")
    protected String image;

    @Parameter(property = "microservice.package.dockerBuildNetwork")
    private String dockerBuildNetwork;

    @Parameter(property= "microservice.package.deleteImage",defaultValue = "true")
    protected Boolean deleteImage = true;

    @Parameter(property = "microservice.package.dockerBuildArchs")
    protected String targetBuildArchs;

    @Parameter(defaultValue = "${mojoExecution}")
    protected MojoExecution mojoExecution;

    @Parameter(property= "microservice.package.dockerBuildTimeout",defaultValue = "360")
    public int dockerBuildTimeout = 360;

    @Parameter(property= "microservice.package.dockerSaveWaitTimeOut",defaultValue = "360")
    public int dockerImageInRegistryMaxWaitTime = 360;

    protected ObjectMapper mapper;

    public PackageMojo() {
        super();
        mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("skipping agent packaging");
            return;
        }

        if (!rpmSkip) {
            getLog().info("rpm package " + project.getArtifactId());
            rpmPackage();
        }

        if (!containerSkip) {

            Iterable<String> buildTargetArchitectures = getTargetBuildArchitectures();
            log.info("Starting docker microservice build for the following target architectures: {}", buildTargetArchitectures);

            for (String arch: buildTargetArchitectures) {

                getLog().info("docker container " + project.getArtifactId() + " in network '" + (dockerBuildNetwork != null ? dockerBuildNetwork : "<default>") + "'");
                dockerContainer(arch);

                if (!skipMicroservicePackage) {
                    getLog().info("microservice zip container " + project.getArtifactId());
                    microserviceZip(arch);
                }
            }
        }
    }

    private void rpmPackage() throws MojoExecutionException {
        try {
            copyArtifact(new File(rpmBaseBuildDir, "bin"));
            copyFromPluginSubdirectory("rpm", rpmTmpDir);
            copyFromProjectSubdirectoryAndReplacePlaceholders(resource(rpmTmpDir.getAbsolutePath()), rpmBaseBuildDir, false);
            copyFromProjectSubdirectoryAndReplacePlaceholders(resource(srcConfigurationDir.getAbsolutePath()), new File(rpmBaseBuildDir, "etc"), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //@formatter:off
        executeMojo(
                rpm(),
                goal("attached-rpm"),
                configuration(
                        element("name", name),
                        element("group", "C8Y/Agent"),
                        element("distribution", "Cumulocity GmbH " + currentYear()),
                        element("packager", "Cumulocity GmbH"),
                        element("requires", element(name("require"), requireJava() +" >= " + javaRuntime)),
                        element("repackJars", String.valueOf(false)),
                        mappings(
                                mapping(directory("/usr/lib/" + directory),
                                        sources(source(location(new File(rpmBaseBuildDir, "bin").getAbsolutePath())))),
                                mapping(directory("/usr/lib/systemd/system"),
                                        directoryIncluded(false),
                                        configuration(true),
                                        sources(source(location(new File(rpmBaseBuildDir, "systemd").getAbsolutePath()),
                                                includes(include(String.format("%s.service", name)))))),
                                mapping(directory("/etc/init.d"),
                                        directoryIncluded(false),
                                        configuration(true),
                                        sources(source(location(new File(rpmBaseBuildDir, "init.d").getAbsolutePath()),
                                                includes(include(name))))),
                                mapping(directory("/etc/" + directory), directoryIncluded(false), configuration("noreplace"),
                                        sources(source(location(new File(rpmBaseBuildDir, "etc").getAbsolutePath())))))

                ), executionEnvironment(this.project, this.mavenSession, this.pluginManager));
        //@formatter:on
    }

    private void dockerContainer(String targetArchitecture)  {

        final File dockerWorkResources = new File(dockerWorkDir, "resources");
        try {
//          copy artifact to docker work directory
            cleanDirectory(dockerWorkResources);
            copyArtifact(dockerWorkResources);

//            copy content of plugin src/main/resources/docker folder to docker work directory replacing placeholders
            final File buildTmp = new File(build, "tmp");
            copyFromPluginSubdirectory("docker", buildTmp);
            copyFromProjectSubdirectoryAndReplacePlaceholders(resource(buildTmp.getAbsolutePath()), dockerWorkDir, false);
            cleanDirectory(buildTmp);

//            copy content of project src/main/configuration to docker work directory replacing placeholders
            copyFromProjectSubdirectoryAndReplacePlaceholders(resource(srcConfigurationDir.getAbsolutePath()), new File(dockerWorkDir, "etc"), true);

//            copy content of project src/main/docker to docker work directory replacing placeholders
            copyFromProjectSubdirectoryAndReplacePlaceholders(resource(srcDockerDir.getAbsolutePath()), dockerWorkDir, true);

//          build docker image with target architecture
            buildDockerImage(targetArchitecture);

//          cleaning up directory created by spotify docker plugin
            cleanDirectory(new File(build, "docker"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private Iterable<String> getTargetBuildArchitectures() {

        if (Objects.nonNull(targetBuildArchs)) {
            log.info("Using custom target build architectures from maven configuration (overriding microservice manifest): {}", targetBuildArchs);
            return Arrays.asList(targetBuildArchs.split(","));
        }

        final File file;

        try {
            file = filterResourceFile(manifestFile);
        } catch (IOException e) {
            log.error("Can't read manifest file {}", manifestFile);
            throw e;
        } catch (MavenFilteringException e) {
            log.error("Maven filtering exception {}", e.getMessage());
            throw e;
        }

        Map<String, Object> jsonManifest = mapper.readValue(file, Map.class);

        if (jsonManifest.containsKey(BUILD_SPEC_FRAGMENT)) {
            DockerBuildSpec buildSpec = mapper.convertValue(jsonManifest.get(BUILD_SPEC_FRAGMENT), DockerBuildSpec.class);
            log.info("Using build target images from build spec: {}", buildSpec.getTargetBuildArchitectures());
            return buildSpec.getTargetBuildArchitectures();
        } else {
            log.info("Manifest contains no buildspec and target architectures. Using defaults");
            return DockerBuildSpec.defaultBuildSpec().getTargetBuildArchitectures();
        }

        }
   private void buildDockerImage(String targetBuildArch) {
        log.info("Building microservice image for target architecture {}", targetBuildArch);
        Map<String, String> buildArgs = configureProxyBuildArguments();
        Set<String> tags = getDockerImageNameExpandedWithTags();

        if (!targetBuildArch.equals(DockerBuildSpec.DEFAULT_TARGET_DOCKER_IMAGE_PLATFORM)) {
            log.warn("Your target build architecture {} does not match the default target {}", targetBuildArch, DockerBuildSpec.DEFAULT_TARGET_DOCKER_IMAGE_PLATFORM);
            log.warn("Currently, Cumulocity only supports hosting microservices build for {}", DockerBuildSpec.DEFAULT_TARGET_DOCKER_IMAGE_PLATFORM);
            log.warn("Your image might be incompatible with cloud hosting!");
        }

        dockerClient.buildDockerImage(dockerWorkDir.getAbsolutePath(),tags,buildArgs, targetBuildArch, dockerBuildNetwork, dockerBuildTimeout, new PullCredentials().asAuthConfigurations());

    }

    private DockerBuildInfo getDockerBuildInfo(String targetBuildArch) {
        return DockerBuildInfo.defaultInfo()
                .withImageArch(targetBuildArch)
                .withCurrentBuildDate()
                .withBuilderInfo(mojoExecution.getPlugin().getId())
                .withHostArchitecture()
                .withHostOS();
    }

    private Set<String> getDockerImageNameExpandedWithTags() {
        String imageVersionTag = image+":"+project.getVersion();
        String imageLatestTag = image+":latest";

        return Sets.newHashSet(imageLatestTag, imageVersionTag);
    }

    private Map<String, String> configureProxyBuildArguments() {
        String httpProxy = null;
        String httpsProxy = null;

        //loop about Proxy settings
        for (Proxy proxy : mavenSession.getSettings().getProxies() ) {
            if ( proxy.isActive() ) {
                String pS = proxy.getProtocol() + "://" + proxy.getHost() + ":" + proxy.getPort();
                getLog().info( "Found Proxy settings: " + pS );
                if ( proxy.getProtocol().equals( "http" ) )
                    httpProxy = pS;
                if ( proxy.getProtocol().equals( "https" ) )
                    httpsProxy = pS;
            }
        }

        if (httpsProxy == null && httpProxy != null )
            httpsProxy = httpProxy;

        if (httpProxy == null && httpsProxy != null )
            httpProxy = httpsProxy;

        Map<String, String> buildArgs = Maps.newHashMap();

        if (httpProxy != null ) {
            buildArgs.put("HTTP_PROXY", httpProxy);
            buildArgs.put("http_proxy", httpProxy);
        }

        if (httpsProxy != null ) {
            buildArgs.put("HTTPS_PROXY", httpsProxy);
            buildArgs.put("https_proxy", httpsProxy);
        }

        return buildArgs;
    }

    private void microserviceZip(String targetArchitecture) {
        try {

            String targetFilename = getTargetFilename(targetArchitecture);
            log.info("Saving microservice to {}", targetFilename);
            createDirectories(build.toPath());

            String imageNameWithTagSave = String.format("%s:%s", image, project.getVersion());

            log.info("Checking if image {} is populated to registry", imageNameWithTagSave);
            dockerClient.waitForImageInRegistry(imageNameWithTagSave, dockerImageInRegistryMaxWaitTime);

            try (final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(
                    new File(build, targetFilename)))) {
                final File file = filterResourceFile(manifestFile);
                validateManifest(file);
                addPlatformTaggedMicroserviceManifestToZipFile(targetArchitecture, filterResourceFile(manifestFile), zipOutputStream);
                saveDockerImageToZipOutputStream(imageNameWithTagSave, zipOutputStream);
            }
            if(deleteImage!= null && deleteImage) {
                Set<String> imageNameWithTags = getDockerImageNameExpandedWithTags();
                log.info("Deleting all images named {} and imageNameWithTags {}", image, imageNameWithTags);
                imageNameWithTags.forEach(imageNameWithTag -> dockerClient.deleteAll(imageNameWithTag, true));

            } else{
                getLog().info("Skipping docker image cleanup");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveDockerImageToZipOutputStream(String imageNameWithTagSave, ZipOutputStream zipOutputStream) throws IOException, MavenExecutionException {
        final ZipEntry ze = new ZipEntry(DOCKER_IMAGE_TAR);
        try {
            zipOutputStream.putNextEntry(ze);
            dockerClient.saveDockerImage(imageNameWithTagSave, zipOutputStream);
        } catch (Exception e) {
            log.error("Error saving Docker image to zip file: {}", e.getMessage());
            throw new MavenExecutionException("Image export to zip file failed", e);
        } finally {
            zipOutputStream.closeEntry();
        }
    }

    private void addPlatformTaggedMicroserviceManifestToZipFile(String targetArchitecture, File manifestFile, ZipOutputStream zipOutputStream) throws IOException {

        log.info("Tagging microservice manifest with image platform");
        ObjectNode manifest = (ObjectNode) mapper.readTree(manifestFile);

        DockerBuildInfo updated = getDockerBuildInfo(targetArchitecture);
        manifest.putPOJO(DOCKER_BUILD_INFO_FRAGMENT, updated);
        String jsonString = mapper.writeValueAsString(manifest);

        final ZipEntry ze = new ZipEntry(MANIFEST_JSON_FILENAME);
        zipOutputStream.putNextEntry(ze);

        BufferedWriter zipStringWriter = new BufferedWriter(new OutputStreamWriter(zipOutputStream));
        zipStringWriter.write(jsonString);
        zipStringWriter.flush();

        zipOutputStream.closeEntry();
    }


    private void validateManifest(File file) throws IOException, MavenExecutionException {
        log.info("Validating manifest");
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charsets.UTF_8)) {
            final MicroserviceManifest manifest = MicroserviceManifest.from(reader);
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<MicroserviceManifest>> constraintViolations = validator.validate(manifest);

            Set<ManifestConstraintViolation> violations = validateMemory(manifest);
            for (ConstraintViolation<MicroserviceManifest> cviolation: constraintViolations) {
                violations.add(new ManifestConstraintViolation(cviolation.getPropertyPath().toString(), cviolation.getMessage()));
            }

            if (!violations.isEmpty()) {
                log.error("Manifest validation error!");
                violations.forEach(violation -> {
                    log.error("Validation error: {} {}", violation.getPath(), violation.getMessage());
                });

                throw new MavenExecutionException("Could not validate manifest", new IllegalArgumentException());
            }
        }
    }

    private Set<ManifestConstraintViolation> validateMemory(MicroserviceManifest manifest) {
        final Resources resources = manifest.getResources();
        if (resources != null && resources.getMemoryLimit().isPresent()) {
            final DataSize memoryLimit = resources.getMemoryLimit().get();
            if (memoryLimit.compareTo(MEMORY_MINIMAL_LIMIT ) < 0) {
                return Sets.newHashSet(new ManifestConstraintViolation("resources.memory", "For java project memory needs to be at least " + MEMORY_MINIMAL_LIMIT));
            }
        }
        return Sets.newHashSet();
    }


    private File filterResourceFile(File source) throws IOException, MavenFilteringException {
        final File filteredDir = new File(build, File.separator + "filtered-resources");
        final MavenResourcesExecution execution = new MavenResourcesExecution(
                ImmutableList.of(resource(source)),
                filteredDir,
                project,
                encoding,
                ImmutableList.of(),
                ImmutableList.of(),
                mavenSession);
        createDirectories(filteredDir.toPath());
        execution.setOverwrite(true);
        mavenResourcesFiltering.filterResources(execution);
        return new File(filteredDir, File.separator + source.getName());
    }

    private Resource resource(File file) {
        Resource resource = new Resource();
        resource.setDirectory(file.getParent());
        resource.setFiltering(true);
        resource.setIncludes(ImmutableList.of(file.getName()));
        resource.setExcludes(ImmutableList.of());
        return resource;
    }

    private String requireJava() {
        if ("8".equals(getJavaVersion())) {
            return "java";
        }
        return "java-" + getJavaVersion();
    }

    @Value
    private static class ManifestConstraintViolation {
        String path;
        String message;
    }

    private class PullCredentials {

        public AuthConfigurations asAuthConfigurations(){
            AuthConfigurations authConfigurations = new AuthConfigurations();
            String registryAddress = getRegistryAddress();
            if (credentialsDefined()) {
                authConfigurations.addConfig(new AuthConfig()
                        .withRegistryAddress(registryAddress)
                        .withUsername(registryUser)
                        .withPassword(registryPass));
            } else if (isNonDefaultRegistry(registryAddress)) {
                getLog().info("Using unauthenticated access to " + registryAddress + ". " +
                        "To define credentials, configure both registryUser and registryPass.");
            }

            return authConfigurations;
        }

        private boolean credentialsDefined() {
            return isNotBlank(registryUser) && isNotBlank(registryPass);
        }

        private String getRegistryAddress() {
            if (isNotBlank(registryUrl)) {
                return registryUrl;
            }
            int registryUrlIndex = baseImage.lastIndexOf("/");
            if (registryUrlIndex > 0) {
                return "https://" + baseImage.substring(0, registryUrlIndex);
            } else {
                return AuthConfig.DEFAULT_SERVER_ADDRESS;
            }
        }

        private boolean isNonDefaultRegistry(String registryAddress) {
            return !AuthConfig.DEFAULT_SERVER_ADDRESS.equals(registryAddress);
        }
    }
}

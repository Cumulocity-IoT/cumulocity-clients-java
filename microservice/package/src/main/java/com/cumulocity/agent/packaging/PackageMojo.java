package com.cumulocity.agent.packaging;

import com.cumulocity.agent.packaging.image.ImageBuildSettings;
import com.cumulocity.agent.packaging.image.ImageWriter;
import com.cumulocity.agent.packaging.image.PackagedImage;
import com.cumulocity.model.DataSize;
import com.cumulocity.model.Resources;
import com.cumulocity.model.application.MicroserviceManifest;
import tools.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

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

    @Parameter(property = "package.name", defaultValue = "${project.artifactId}")
    protected String image;

    @Parameter(property= "microservice.package.deleteImage",defaultValue = "true")
    protected Boolean deleteImage = true;

    @Parameter(property = "microservice.package.dockerBuildArchs")
    protected String targetBuildArchs;

    @Parameter(defaultValue = "${mojoExecution}")
    protected MojoExecution mojoExecution;

    @Parameter(property= "microservice.package.dockerSaveWaitTimeOut",defaultValue = "360")
    public int dockerImageInRegistryMaxWaitTime = 360;

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

            try {
                prepareDockerWorkDir();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            getLog().info("docker container " + project.getArtifactId() + " in network '" + (dockerBuildNetwork != null ? dockerBuildNetwork : "<default>") + "'");
            imageBuilder().build(ImmutableList.copyOf(buildTargetArchitectures), skipMicroservicePackage ? null : this::writeMicroserviceZip);
        }
    }

    @Override
    protected ImageBuildSettings.ImageBuildSettingsBuilder imageBuildSettings() {
        return super.imageBuildSettings()
                .imageName(image)
                .deleteImage(deleteImage != null && deleteImage)
                .imageWaitTimeoutSeconds(dockerImageInRegistryMaxWaitTime);
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
    private DockerBuildInfo getDockerBuildInfo(String targetBuildArch) {
        return DockerBuildInfo.defaultInfo()
                .withImageArch(targetBuildArch)
                .withCurrentBuildDate()
                .withBuilderInfo(mojoExecution.getPlugin().getId())
                .withHostArchitecture()
                .withHostOS();
    }


    private void writeMicroserviceZip(PackagedImage image) {
        try {
            getLog().info("microservice zip container " + project.getArtifactId());
            String targetFilename = getTargetFilename(image.getFileNamePlatform());
            log.info("Saving microservice to {}", targetFilename);
            createDirectories(build.toPath());

            try (final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(
                    new File(build, targetFilename)))) {
                final File file = filterResourceFile(manifestFile);
                validateManifest(file);
                addPlatformTaggedMicroserviceManifestToZipFile(image.getPlatforms().get(0), filterResourceFile(manifestFile), zipOutputStream);
                saveDockerImageToZipOutputStream(image.getWriter(), zipOutputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveDockerImageToZipOutputStream(ImageWriter imageWriter, ZipOutputStream zipOutputStream) throws IOException, MavenExecutionException {
        final ZipEntry ze = new ZipEntry(DOCKER_IMAGE_TAR);
        try {
            zipOutputStream.putNextEntry(ze);
            imageWriter.write(zipOutputStream);
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
            ValidatorFactory factory = Validation.byDefaultProvider().configure()
                    .messageInterpolator(new ParameterMessageInterpolator())
                    .buildValidatorFactory();
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

}

package com.cumulocity.agent.packaging;

import com.cumulocity.agent.packaging.image.ImageBuildSettings;
import com.cumulocity.agent.packaging.image.PackagedImage;
import com.cumulocity.model.DataSize;
import com.cumulocity.model.Resources;
import com.cumulocity.model.application.MicroserviceManifest;
import tools.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
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

    /**
     * Removes the image from the local docker daemon after it was saved to the microservice zip (docker daemon build).
     */
    @Parameter(property= "microservice.package.deleteImage",defaultValue = "true")
    protected Boolean deleteImage = true;

    @Parameter(defaultValue = "${mojoExecution}")
    protected MojoExecution mojoExecution;

    /**
     * Maximum time in seconds to wait for the built image in the docker daemon. Not used with docker buildx.
     */
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
            List<TargetPlatform> platforms = getTargetPlatforms();
            log.info("Starting docker microservice build for the following target platforms: {}", platforms);
            try {
                prepareDockerWorkDir();
            } catch (Exception e) {
                throw new MojoExecutionException("Cannot prepare docker build context", e);
            }
            getLog().info("docker container " + project.getArtifactId() + " in network '" + (dockerBuildNetwork != null ? dockerBuildNetwork : "<default>") + "'");
            imageBuilder().build(platforms, skipMicroservicePackage ? null : this::writeMicroserviceZip);
        }
    }

    @Override
    protected ImageBuildSettings.ImageBuildSettingsBuilder imageBuildSettings() {
        return super.imageBuildSettings()
                .imageName(image)
                .deleteImage(deleteImage == null || deleteImage)
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

    private DockerBuildInfo getDockerBuildInfo(List<TargetPlatform> platforms) {
        return DockerBuildInfo.defaultInfo()
                .withImageArch(platforms.get(0).platform())
                .withImageArchitectures(platforms.stream().map(TargetPlatform::platform).collect(Collectors.toList()))
                .withCurrentBuildDate()
                .withBuilderInfo(mojoExecution.getPlugin().getId())
                .withHostArchitecture()
                .withHostOS();
    }

    private void writeMicroserviceZip(PackagedImage image) throws MojoExecutionException {
        getLog().info("microservice zip container " + project.getArtifactId());
        String targetFilename = image.getFileNamePlatform() == null ? getDefaultTargetFilename() : getTargetFilename(image.getFileNamePlatform().platform());
        File zip = new File(build, targetFilename);
        log.info("Saving microservice to {}", targetFilename);
        try {
            createDirectories(build.toPath());
            try (final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zip))) {
                final File file = filterResourceFile(manifestFile);
                validateManifest(file);
                addPlatformTaggedMicroserviceManifestToZipFile(image.getPlatforms(), file, zipOutputStream);
                zipOutputStream.putNextEntry(new ZipEntry(DOCKER_IMAGE_TAR));
                try {
                    image.getWriter().write(zipOutputStream);
                } finally {
                    zipOutputStream.closeEntry();
                }
            }
        } catch (Exception e) {
            // an incomplete zip must not be uploaded
            FileUtils.deleteQuietly(zip);
            throw new MojoExecutionException("Cannot create microservice package " + targetFilename, e);
        }
    }

    private void addPlatformTaggedMicroserviceManifestToZipFile(List<TargetPlatform> platforms, File manifestFile, ZipOutputStream zipOutputStream) throws IOException {

        log.info("Tagging microservice manifest with image platform");
        ObjectNode manifest = (ObjectNode) mapper.readTree(manifestFile);

        DockerBuildInfo updated = getDockerBuildInfo(platforms);
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

package com.cumulocity.agent.packaging;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Resources.asByteSource;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public abstract class BaseMicroserviceMojo extends AbstractMojo {

    @Parameter(property = "agent-package.container.registry")
    protected String registry;

    @Component
    protected BuildPluginManager pluginManager;

    @Parameter(property = "package.name", defaultValue = "${project.artifactId}")
    protected String name;

    @Parameter(property = "package.directory", defaultValue = "${project.artifactId}")
    protected String directory;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession mavenSession;

    @Parameter(defaultValue = "${project.build.directory}")
    protected File build;

    @Parameter(defaultValue = "${project.build.directory}/rpm-tmp")
    protected File rpmTmpDir;

    @Parameter(defaultValue = "${project.build.directory}/docker-work")
    protected File dockerWorkDir;

    @Parameter(defaultValue = "${project.build.directory}/rpm-base-build")
    protected File rpmBaseBuildDir;

    @Parameter(defaultValue = "${basedir}/src/main/configuration")
    protected File srcConfigurationDir;

    @Parameter(defaultValue = "${basedir}/src/main/docker")
    protected File srcDockerDir;

    @Parameter(defaultValue = "false", property = "skip.agent.package")
    protected boolean skip;

    @Parameter(defaultValue = "true", property = "skip.agent.package.rpm")
    protected boolean rpmSkip;

    @Parameter(defaultValue = "false", property = "skip.agent.package.container")
    protected boolean containerSkip;

    @Parameter(defaultValue = "false", property = "skip.microservice.package")
    protected boolean skipMicroservicePackage;

    @Parameter(defaultValue = "${maven.compiler.target}")
    protected String javaRuntime;

    @Component
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Parameter(property = "package.description", defaultValue = "${project.description}")
    private String description;

    @Parameter(property = "agent-package.jvmArgs")
    private List<String> jvmArgs;

    @Parameter(property = "agent-package.arguments")
    private List<String> arguments;

    @Parameter(property = "project.build.sourceEncoding", defaultValue = "UTF-8")
    protected String encoding;

    @Parameter(property = "nonFilteredFileExtensions", defaultValue = "jks,jar")
    private List<String> nonFilteredFileExtensions;

    @Parameter(property = "agent-package.heap")
    private Memory heap;

    @Parameter(property = "agent-package.perm")
    private Memory perm;

    protected void copyFromProjectSubdirectoryAndReplacePlaceholders(Resource src, File destination, boolean override) throws Exception {
        final MavenResourcesExecution execution = new MavenResourcesExecution(ImmutableList.of(src), destination, project, encoding,
                                                                                 ImmutableList.<String>of(), ImmutableList.<String>of(),
                                                                                 mavenSession);
        getLog().info("copy resources from " + src + " to" + destination);
        createDirectories(destination.toPath());
        execution.setOverwrite(override);
        execution.setFilterFilenames(true);
        execution.setNonFilteredFileExtensions(nonFilteredFileExtensions);
        execution.setDelimiters(Sets.newLinkedHashSet(ImmutableSet.of("@*@")));
        execution.setSupportMultiLineFiltering(true);
        final Properties props = new Properties();
        props.put("package.name", name);
        props.put("package.directory", directory);
        props.put("package.description", firstNonNull(description, name + " Service"));
        props.put("package.jvm-heap", Joiner.on(' ').join(getJvmHeap()));
        props.put("package.jvm-perm", Joiner.on(' ').join(getJvmPerm()));
        props.put("package.jvm-gc", Joiner.on(' ').join(getJvmGc()));
        props.put("package.arguments", Joiner.on(' ').join(arguments));
        props.put("package.required-java", javaRuntime);
        props.put("package.java-version", getJavaVersion());
        execution.setAdditionalProperties(props);
        mavenResourcesFiltering.filterResources(execution);
    }

    private List<String> getJvmHeap() {
        return heap.isEmpty() ? Collections.emptyList() : ImmutableList.of(
                "-Xms" + heap.getMin(),
                "-Xmx" + heap.getMax()
        );
    }

    private List<String> getJvmPerm() {
        if (perm.isEmpty()) {
            return Collections.emptyList();
        }

        if (isJava7()) {
            return ImmutableList.of(
                    "-XX:PermSize=" + perm.getMin(),
                    "-XX:MaxPermSize=" + perm.getMax()
            );
        } else {
            return ImmutableList.of(
                    "-XX:MetaspaceSize=" + perm.getMin(),
                    "-XX:MaxMetaspaceSize=" + perm.getMax()
            );
        }
    }

    private List<String> getJvmGc() {
        if (jvmArgs == null || jvmArgs.isEmpty()) {
            return ImmutableList.<String>builder().add("-XX:+UseConcMarkSweepGC", "-XX:+CMSParallelRemarkEnabled",
                    "-XX:+ScavengeBeforeFullGC", "-XX:+CMSScavengeBeforeRemark").build();
        } else
            return jvmArgs;
    }

    public void copyFromPluginSubdirectory(String src, File dest) throws IOException {
        copyTemplates(fromDirectory(src), dest);
    }

    private void copyTemplates(Predicate<? super ClassPath.ResourceInfo> filter, File dest) throws IOException {
        getLog().debug("initialize templates");

        for (ClassPath.ResourceInfo resource : loadTemplates(filter)) {
            final URL url = resource.url();
            getLog().debug("template found " + resource.getResourceName());

            final String path = new File(resource.getResourceName()).getPath();
            final File destination = new File(dest, path.substring(path.indexOf(File.separator)));
            Files.createParentDirs(destination);
            if (!destination.exists() || !asByteSource(url).contentEquals(asByteSource(destination))) {
                asByteSource(url).copyTo(asByteSink(destination));
            }
        }

    }

    protected void copyArtifact(@NonNull File destination) throws IOException {
        getLog().debug("copyArtifactTo " + project.getArtifact().getFile().toString());

        final File to = new File(destination, String.format("%s.jar", name));
        Files.createParentDirs(to);
        getLog().debug(String.format("copy artifact %s to %s ", project.getArtifact().getFile().getAbsolutePath(), to.getAbsolutePath()));
        if (!to.exists() || !asByteSource(project.getArtifact().getFile()).contentEquals(asByteSource(to))) {
            java.nio.file.Files.copy(project.getArtifact().getFile().toPath(), to.toPath(), REPLACE_EXISTING);
        }
    }

    protected void cleanDirectory(@NonNull File destination) throws IOException {
        getLog().debug("cleanDirectory " + destination.getAbsolutePath());

        if (destination.exists()) {
            FileUtils.cleanDirectory(destination);
        }
    }

    private ImmutableList<ClassPath.ResourceInfo> loadTemplates(Predicate<? super ClassPath.ResourceInfo> filter) throws IOException {
        return FluentIterable.from(ClassPath.from(getClass().getClassLoader()).getResources()).filter(filter).toList();
    }

    protected Predicate<ClassPath.ResourceInfo> fromDirectory(final String directory) {
        return new Predicate<ClassPath.ResourceInfo>() {
            public boolean apply(ClassPath.ResourceInfo input) {
                return input.getResourceName().startsWith(directory);
            }
        };
    }

    public Resource resource(String resourceDirectory) {
        return resource(resourceDirectory, ImmutableList.<String>of(), ImmutableList.<String>of());
    }

    public Resource resource(String resourceDirectory, List<String> includes, List<String> excludes) {
        Resource resource = new Resource();
        resource.setDirectory(resourceDirectory);
        resource.setFiltering(true);
        resource.setIncludes(includes);
        resource.setExcludes(excludes);
        return resource;
    }

    private String getJavaVersion() {
        if (javaRuntime.startsWith("1.")) {
            return javaRuntime.substring(2);
        }
        return javaRuntime;
    }

    private boolean isJava7() {
        return "7".equals(getJavaVersion());
    }
}

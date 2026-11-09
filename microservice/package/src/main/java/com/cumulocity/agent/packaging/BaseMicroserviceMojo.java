package com.cumulocity.agent.packaging;

import com.cumulocity.agent.packaging.image.DaemonImageBuilder;
import com.cumulocity.agent.packaging.image.ImageBuildSettings;
import com.cumulocity.agent.packaging.image.ImageBuilder;
import com.cumulocity.agent.packaging.microservice.MicroserviceDockerClient;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Resources.asByteSource;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.firstNonEmpty;

public abstract class BaseMicroserviceMojo extends AbstractMojo {

    public static final String TARGET_FILENAME_PATTERN_NON_DEFAULT_ARCH = "%s-%s-%s.zip";
    public static final String TARGET_FILENAME_PATTERN_DEFAULT_ARCH = "%s-%s.zip";
    /*
    * The shortcut "OTEL" used in several places in this project is an abbreviation
    * for "OpenTelemetry", a framework for generating and exporting monitoring data.
    */
    public static final String OTEL_JAVA_AGENT_DOWNLOAD_URL =
           "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.19.0/opentelemetry-javaagent.jar";

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

    @Parameter(defaultValue = "${maven.compiler.release}, ${maven.compiler.target}, ${microservice.java.version}")
    protected String javaRuntime;

    @Component
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Parameter(property = "package.description", defaultValue = "${project.description}")
    private String description;

    @Parameter(property = "agent-package.jvmArgs")
    private List<String> jvmArgs;

    @Parameter(property = "agent-package.otel-java-agent-download-url", defaultValue = OTEL_JAVA_AGENT_DOWNLOAD_URL)
    protected String otelJavaAgentDownloadUrl;

    @Parameter(property = "agent-package.otel-java-agent-include", defaultValue = "false")
    protected String otelJavaAgentInclude;

    @Parameter(property = "agent-package.arguments")
    private List<String> arguments;

    @Parameter(property = "project.build.sourceEncoding", defaultValue = "UTF-8")
    protected String encoding;

    @Parameter(property = "nonFilteredFileExtensions", defaultValue = "jks,jar")
    private List<String> nonFilteredFileExtensions;

    @Parameter(property = "agent-package.heap")
    private Memory heap;

    @Deprecated
    @Parameter(property = "agent-package.perm")
    private Memory perm;

    @Parameter(property = "agent-package.metaspace")
    private Memory metaspace;

    @Parameter(property = "package.docker.baseImage", defaultValue = "alpine:3")
    protected String baseImage;

    @Parameter(property = "package.docker.registryUrl")
    protected String registryUrl;

    @Parameter(property = "package.docker.registryUser")
    protected String registryUser;

    @Parameter(property = "package.docker.registryPass")
    protected String registryPass;

    @Component
    protected MicroserviceDockerClient dockerClient;

    @Parameter(defaultValue = "${basedir}/src/main/configuration/cumulocity.json")
    protected File manifestFile;

    /**
     * Comma separated target platforms (amd64, arm64; other docker platforms are used as given with a warning).
     * Overrides {@code buildSpec.targetBuildArchitectures} of the microservice manifest.
     * <p>
     * The former pom name {@code targetBuildArchs} is still accepted, but deprecated.
     */
    @Parameter(property = "microservice.package.dockerBuildArchs", alias = "targetBuildArchs")
    protected String dockerBuildArchs;

    /**
     * Builds every layer again and pulls the base image, so all platforms install the current packages (e.g. Java).
     * Set to {@code false} to use the docker build cache.
     */
    @Parameter(property = "microservice.package.dockerBuildNoCache", defaultValue = "true")
    protected boolean dockerBuildNoCache = true;

    @Parameter(property = "microservice.package.dockerBuildNetwork")
    protected String dockerBuildNetwork;

    @Parameter(property = "microservice.package.dockerBuildTimeout", defaultValue = "360")
    public int dockerBuildTimeout = 360;

    protected ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .enable(SerializationFeature.INDENT_OUTPUT).build();


    protected void copyFromProjectSubdirectoryAndReplacePlaceholders(Resource src, File destination, boolean override) throws Exception {
        final MavenResourcesExecution execution = new MavenResourcesExecution(ImmutableList.of(src), destination, project, encoding,
                                                                                 ImmutableList.of(), ImmutableList.of(),
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
        props.put("package.jvm-meta", Joiner.on(' ').join(getJvmMeta()));
        props.put("package.jvm-mem", Joiner.on(' ').join(getJvmMem()));
        props.put("package.jvm-gc", Joiner.on(' ').join(getJvmGc()));
        props.put("package.arguments", Joiner.on(' ').join(arguments));
        props.put("package.java-version", getJavaVersion());
        props.put("package.otel-java-agent-download-url", otelJavaAgentDownloadUrl);
        props.put("package.otel-java-agent-include", otelJavaAgentInclude);
        props.put("package.required-java", javaRuntime);
        props.put("package.docker.baseImage", baseImage);
        execution.setAdditionalProperties(props);
        mavenResourcesFiltering.filterResources(execution);
    }

    private List<String> getJvmMem() {
        return Lists.newArrayList(Iterables.concat(getJvmHeap(), getJvmMeta()));
    }

    private List<String> getJvmHeap() {
        return heap.isEmpty() ? Collections.emptyList() : ImmutableList.of(
                "-Xms" + heap.getMin(),
                "-Xmx" + heap.getMax()
        );
    }

    private List<String> getJvmMeta() {
        if (metaspace.isEmpty() && perm.isEmpty()) {
            return Collections.emptyList();
        }
        return ImmutableList.of(
                "-XX:MetaspaceSize=" + firstNonEmpty(metaspace.getMin(), perm.getMin()),
                "-XX:MaxMetaspaceSize=" + firstNonEmpty(metaspace.getMax(), perm.getMax())
                );
    }

    private List<String> getJvmGc() {
        if (jvmArgs == null || jvmArgs.isEmpty()) {
            return ImmutableList.<String>builder()
                    .add("-XX:+UseG1GC",
                         "-XX:+UseStringDeduplication",
                         "-XX:MinHeapFreeRatio=25",
                         "-XX:MaxHeapFreeRatio=75"
                        ).build();
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
        return resource(resourceDirectory, ImmutableList.of(), ImmutableList.of());
    }

    public Resource resource(String resourceDirectory, List<String> includes, List<String> excludes) {
        Resource resource = new Resource();
        resource.setDirectory(resourceDirectory);
        resource.setFiltering(true);
        resource.setIncludes(includes);
        resource.setExcludes(excludes);
        return resource;
    }

    String getJavaVersion() {
        Pattern pattern = Pattern.compile("\\d+\\.?\\d*");
        Matcher matcher = pattern.matcher(javaRuntime);

        if (matcher.find()) {
            javaRuntime = matcher.group();
        } else {
            throw new IllegalArgumentException("Wrong format or missing java version. Missing at least one property from list: " +
                    "maven.compiler.release, maven.compiler.target, microservice.java.version in valid format (1.8, 11 etc.)");
        }

        if (javaRuntime.startsWith("1.")) {
            return javaRuntime.substring(2);
        }
        return javaRuntime;
    }

    protected String getTargetFilename(String targetArchitecture) {
        if (targetArchitecture.equals(DockerBuildSpec.DEFAULT_TARGET_DOCKER_IMAGE_PLATFORM)) {
            return getDefaultTargetFilename();
        }

        return String.format(TARGET_FILENAME_PATTERN_NON_DEFAULT_ARCH, name, project.getVersion(), targetArchitecture).replaceAll("/","-");
    }

    protected String getDefaultTargetFilename() {
        return String.format(TARGET_FILENAME_PATTERN_DEFAULT_ARCH, name, project.getVersion());
    }

    /**
     * Target platforms from {@code microservice.package.dockerBuildArchs}, the manifest build spec, or amd64.
     */
    protected List<TargetPlatform> getTargetPlatforms() throws MojoExecutionException {
        List<TargetPlatform> platforms = readTargetPlatforms();
        platforms.stream().filter(platform -> !platform.isSupported()).forEach(platform ->
                getLog().warn("Selected architecture " + platform + " may not be supported by Cumulocity. Supported architectures: "
                        + TargetPlatform.supportedValues()));
        return platforms;
    }

    private List<TargetPlatform> readTargetPlatforms() throws MojoExecutionException {
        if (dockerBuildArchs != null) {
            getLog().info("Using target build architectures from maven configuration (overriding microservice manifest): " + dockerBuildArchs);
            return TargetPlatform.parse(Arrays.asList(dockerBuildArchs.split(",")));
        }
        Map<String, Object> jsonManifest;
        try {
            jsonManifest = mapper.readValue(filterResourceFile(manifestFile), Map.class);
        } catch (IOException | MavenFilteringException e) {
            throw new MojoExecutionException("Can't read manifest file " + manifestFile, e);
        }
        if (jsonManifest.containsKey(DockerBuildSpec.BUILD_SPEC_FRAGMENT)) {
            DockerBuildSpec buildSpec = mapper.convertValue(jsonManifest.get(DockerBuildSpec.BUILD_SPEC_FRAGMENT), DockerBuildSpec.class);
            getLog().info("Using target build architectures from build spec: " + buildSpec.getTargetBuildArchitectures());
            return TargetPlatform.parse(buildSpec.getTargetBuildArchitectures());
        }
        getLog().info("Manifest contains no build spec. Using default target architecture " + TargetPlatform.AMD64);
        return TargetPlatform.parse(Collections.emptyList());
    }

    protected File filterResourceFile(File source) throws IOException, MavenFilteringException {
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

    protected Resource resource(File file) {
        Resource resource = new Resource();
        resource.setDirectory(file.getParent());
        resource.setFiltering(true);
        resource.setIncludes(ImmutableList.of(file.getName()));
        resource.setExcludes(ImmutableList.of());
        return resource;
    }

    /**
     * Copies the jar, the plugin docker templates, project configuration and project docker files to the docker
     * build context, replacing placeholders.
     */
    protected void prepareDockerWorkDir() throws Exception {
        final File dockerWorkResources = new File(dockerWorkDir, "resources");
        // copy artifact to docker work directory
        cleanDirectory(dockerWorkResources);
        copyArtifact(dockerWorkResources);

        // copy content of plugin src/main/resources/docker folder to docker work directory replacing placeholders
        final File buildTmp = new File(build, "tmp");
        copyFromPluginSubdirectory("docker", buildTmp);
        copyFromProjectSubdirectoryAndReplacePlaceholders(resource(buildTmp.getAbsolutePath()), dockerWorkDir, false);
        cleanDirectory(buildTmp);

        // copy content of project src/main/configuration to docker work directory replacing placeholders
        copyFromProjectSubdirectoryAndReplacePlaceholders(resource(srcConfigurationDir.getAbsolutePath()), new File(dockerWorkDir, "etc"), true);

        // copy content of project src/main/docker to docker work directory replacing placeholders
        copyFromProjectSubdirectoryAndReplacePlaceholders(resource(srcDockerDir.getAbsolutePath()), dockerWorkDir, true);
    }

    protected Map<String, String> configureProxyBuildArguments() {
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

    /**
     * The image builder of the selected build method.
     */
    protected ImageBuilder imageBuilder() {
        return new DaemonImageBuilder(dockerClient, imageBuildSettings().build());
    }

    protected ImageBuildSettings.ImageBuildSettingsBuilder imageBuildSettings() {
        return ImageBuildSettings.builder()
                .imageName(name)
                .version(project.getVersion())
                .dockerWorkDir(dockerWorkDir)
                .buildDir(build)
                .buildArgs(configureProxyBuildArguments())
                .network(dockerBuildNetwork)
                .timeoutSeconds(dockerBuildTimeout)
                .noCache(dockerBuildNoCache)
                .pullRegistryUrl(registryUrl)
                .pullRegistryUser(registryUser)
                .pullRegistryPass(registryPass)
                .baseImage(baseImage);
    }

}

package com.cumulocity.agent.packaging.microservice.buildx;

import com.cumulocity.agent.packaging.TargetPlatform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Image built once into an OCI layout directory.
 * <p>
 * The Java version of every platform is read from the layout, and the image is exported (into the microservice zip,
 * to a registry or the docker daemon) from the same layout. Export and check therefore use exactly the same layers;
 * the application is not built again. The {@code push} goal reuses the layout built by the {@code package} goal.
 */
@Slf4j(topic = "BuildxClient")
public class OciLayoutImage {

    static final String CONTEXT_NAME = "microservice";
    static final String JAVA_VERSION_FILE = "java-version";
    static final String COMPLETE_FILE = "complete.properties";

    static final String JAVA_VERSION_DOCKERFILE = String.join("\n",
            "FROM " + CONTEXT_NAME + " AS probe",
            "RUN java -version > /tmp/" + JAVA_VERSION_FILE + " 2>&1",
            "FROM scratch",
            "COPY --from=probe /tmp/" + JAVA_VERSION_FILE + " /" + JAVA_VERSION_FILE,
            "");

    static final String EXPORT_DOCKERFILE = "FROM " + CONTEXT_NAME + "\n";

    private final BuildxClient client;
    private final DockerCliContext context;
    private final String builder;
    private final List<TargetPlatform> platforms;
    private final File workDir;

    private OciLayoutImage(BuildxClient client, DockerCliContext context, String builder, List<TargetPlatform> platforms, File workDir) {
        this.client = client;
        this.context = context;
        this.builder = builder;
        this.platforms = platforms;
        this.workDir = workDir;
    }

    /**
     * Builds the image described by {@code request} into {@code <workDir>/image}. The output and tags of the request
     * are ignored.
     */
    public static OciLayoutImage build(BuildxClient client, DockerCliContext context, BuildRequest request, File workDir) throws MojoExecutionException {
        try {
            FileUtils.deleteDirectory(workDir);
            Files.createDirectories(workDir.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot prepare directory " + workDir, e);
        }
        OciLayoutImage image = new OciLayoutImage(client, context, request.getBuilder(), request.getPlatforms(), workDir);
        client.build(context, request.toBuilder()
                .clearTags()
                .load(false)
                .output("type=oci,tar=false,dest=" + image.layoutDir().getAbsolutePath())
                .build());
        return image;
    }

    /**
     * Marks the image as complete (built and checked), so {@link #existing} returns it.
     *
     * @param sameJavaVersion result of the Java version check
     */
    public void complete(boolean sameJavaVersion) throws MojoExecutionException {
        writeFile(COMPLETE_FILE, "platforms=" + TargetPlatform.toPlatformList(platforms) + "\n"
                + "sameJavaVersion=" + sameJavaVersion + "\n");
    }

    /**
     * @param requireSameJavaVersion ignore an image whose platforms have different Java versions
     * @return the complete image built before into {@code workDir} for the same platforms, or empty
     */
    public static Optional<OciLayoutImage> existing(BuildxClient client, DockerCliContext context, String builder,
                                                    List<TargetPlatform> platforms, File workDir, boolean requireSameJavaVersion) {
        File completeFile = new File(workDir, COMPLETE_FILE);
        if (!new File(workDir, "image/index.json").isFile() || !completeFile.isFile()) {
            return Optional.empty();
        }
        Properties complete = new Properties();
        try (Reader reader = Files.newBufferedReader(completeFile.toPath(), StandardCharsets.UTF_8)) {
            complete.load(reader);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (!TargetPlatform.toPlatformList(platforms).equals(complete.getProperty("platforms"))
                || (requireSameJavaVersion && !Boolean.parseBoolean(complete.getProperty("sameJavaVersion")))) {
            return Optional.empty();
        }
        return Optional.of(new OciLayoutImage(client, context, builder, platforms, workDir));
    }

    /**
     * Runs {@code java -version} in every platform of the image.
     *
     * @return output of {@code java -version} per platform, in the order of the platforms
     */
    public Map<TargetPlatform, String> javaVersions() throws MojoExecutionException {
        File outputDir = new File(workDir, "java-version");
        client.build(context, BuildRequest.builder()
                .builder(builder)
                .context(dockerfileContext("java-version-check", JAVA_VERSION_DOCKERFILE))
                .platforms(platforms)
                .buildContext(CONTEXT_NAME, reference())
                .output("type=local,dest=" + outputDir.getAbsolutePath())
                .build());

        Map<TargetPlatform, String> versions = new LinkedHashMap<>();
        for (TargetPlatform platform : platforms) {
            // the local exporter writes one directory per platform, e.g. linux_arm64, for several platforms
            File file = platforms.size() == 1
                    ? new File(outputDir, JAVA_VERSION_FILE)
                    : new File(new File(outputDir, platform.platform().replace('/', '_')), JAVA_VERSION_FILE);
            if (!file.isFile()) {
                throw new MojoExecutionException("Cannot read the Java version of platform " + platform + ", " + file + " does not exist");
            }
            try {
                versions.put(platform, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim());
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot read the Java version of platform " + platform + " from " + file, e);
            }
        }
        return versions;
    }

    /**
     * Pushes the image from the layout to the registry of the tags.
     */
    public void push(List<String> tags) throws MojoExecutionException {
        export(tags, "type=registry", false, null);
    }

    /**
     * Loads the image from the layout into the docker daemon.
     */
    public void load(List<String> tags) throws MojoExecutionException {
        export(tags, null, true, null);
    }

    /**
     * Writes the image as a tar to {@code out}: in {@code docker save} format for one platform, as an OCI image
     * index for several platforms.
     *
     * @param name image name stored in the tar, e.g. {@code service:1.0}
     */
    public void writeTo(String name, OutputStream out) throws MojoExecutionException {
        String type = platforms.size() == 1 ? "docker" : "oci";
        export(List.of(), "type=" + type + ",name=" + name + ",dest=-", false, out);
    }

    private void export(List<String> tags, String output, boolean load, OutputStream stdout) throws MojoExecutionException {
        log.info("Exporting image for {}", platforms);
        client.build(context, BuildRequest.builder()
                .builder(builder)
                .context(dockerfileContext("export", EXPORT_DOCKERFILE))
                .platforms(platforms)
                .buildContext(CONTEXT_NAME, reference())
                .tags(tags)
                .output(output)
                .load(load)
                .stdout(stdout)
                .build());
    }

    File layoutDir() {
        return new File(workDir, "image");
    }

    /**
     * @return named build context of the image, pinned to the digest of the image index in the layout
     */
    String reference() throws MojoExecutionException {
        File index = new File(layoutDir(), "index.json");
        try {
            JsonNode manifests = JsonMapper.builder().build().readTree(index).path("manifests");
            String digest = manifests.path(0).path("digest").asString("");
            if (manifests.size() != 1 || digest.isEmpty()) {
                throw new MojoExecutionException("Unexpected OCI layout " + index + ", expected exactly one image: " + manifests);
            }
            return "oci-layout://" + layoutDir().getAbsolutePath() + "@" + digest;
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Cannot read OCI layout " + index, e);
        }
    }

    private File dockerfileContext(String name, String dockerfile) throws MojoExecutionException {
        File dir = new File(workDir, name);
        writeFile(name + "/Dockerfile", dockerfile);
        return dir;
    }

    private void writeFile(String path, String content) throws MojoExecutionException {
        File file = new File(workDir, path);
        try {
            Files.createDirectories(file.getParentFile().toPath());
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write " + file, e);
        }
    }
}

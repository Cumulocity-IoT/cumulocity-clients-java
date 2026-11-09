package com.cumulocity.agent.packaging.image;

import com.cumulocity.agent.packaging.TargetPlatform;
import com.cumulocity.agent.packaging.microservice.buildx.BuildRequest;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Answer for {@code BuildxClient.build} that writes what docker buildx would write for the requested output:
 * an OCI layout directory, the {@code java -version} output per platform, or an image tar to stdout.
 */
class FakeBuildx {

    static final String JAVA_VERSION = "openjdk version \"17.0.21\" 2026-10-21\nOpenJDK Runtime Environment (build 17.0.21+7-alpine-r0)";
    static final String IMAGE_INDEX_DIGEST = "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final Map<TargetPlatform, String> javaVersions = new HashMap<>();
    private final String imageTarContent;

    FakeBuildx(String imageTarContent) {
        this.imageTarContent = imageTarContent;
    }

    FakeBuildx javaVersion(TargetPlatform platform, String version) {
        javaVersions.put(platform, version);
        return this;
    }

    Answer<Void> answer() {
        return invocation -> {
            BuildRequest request = invocation.getArgument(1);
            String output = request.getOutput();
            if (output == null) {
                return null;
            }
            if (output.startsWith("type=oci,tar=false,dest=")) {
                File layout = new File(output.substring("type=oci,tar=false,dest=".length()));
                Files.createDirectories(layout.toPath());
                Files.writeString(new File(layout, "index.json").toPath(),
                        "{\"schemaVersion\":2,\"manifests\":[{\"mediaType\":\"application/vnd.oci.image.index.v1+json\",\"digest\":\""
                                + IMAGE_INDEX_DIGEST + "\",\"size\":100}]}");
            } else if (output.startsWith("type=local,dest=")) {
                File dest = new File(output.substring("type=local,dest=".length()));
                for (TargetPlatform platform : request.getPlatforms()) {
                    File dir = request.getPlatforms().size() == 1 ? dest : new File(dest, platform.platform().replace('/', '_'));
                    Files.createDirectories(dir.toPath());
                    Files.writeString(new File(dir, "java-version").toPath(), javaVersions.getOrDefault(platform, JAVA_VERSION) + "\n");
                }
            } else if (output.endsWith("dest=-")) {
                request.getStdout().write(imageTarContent.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        };
    }
}

package com.cumulocity.agent.packaging.microservice.buildx;

import com.cumulocity.agent.packaging.TargetPlatform;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Checks that all platforms of a multi-platform image run the same Java version (the full {@code java -version} output).
 * <p>
 * The Java version can differ when the build cache holds an older Java layer for one platform, when the package mirrors
 * of the architectures are out of sync, or when a custom base image has different Java versions for its platforms.
 */
@Slf4j
public class JavaVersionCheck {

    private final boolean failOnMismatch;
    private final boolean noCache;

    /**
     * @param failOnMismatch fail the build instead of logging a warning
     * @param noCache        whether the image was built without the build cache (otherwise the message mentions the cache)
     */
    public JavaVersionCheck(boolean failOnMismatch, boolean noCache) {
        this.failOnMismatch = failOnMismatch;
        this.noCache = noCache;
    }

    /**
     * @return {@code true} when all platforms have the same Java version; {@code false} after a warning (a mismatch,
     * or the version could not be read)
     */
    public boolean check(OciLayoutImage image) throws MojoExecutionException {
        Map<TargetPlatform, String> versions;
        try {
            versions = image.javaVersions();
        } catch (MojoExecutionException e) {
            if (failOnMismatch) {
                throw new MojoExecutionException("Cannot check the Java version of the image platforms: " + e.getMessage(), e);
            }
            log.warn("Cannot check the Java version of the image platforms: {}", e.getMessage());
            return false;
        }
        return check(versions);
    }

    boolean check(Map<TargetPlatform, String> versions) throws MojoExecutionException {
        Set<String> distinct = new LinkedHashSet<>(versions.values());
        if (distinct.size() <= 1) {
            log.info("All platforms {} use Java: {}", versions.keySet(), firstLine(distinct.isEmpty() ? "" : distinct.iterator().next()));
            return true;
        }
        StringBuilder message = new StringBuilder("The platforms of the image have different Java versions:");
        versions.forEach((platform, version) -> message.append("\n  ").append(platform).append(": ")
                .append(version.replace("\n", "\n    ")));
        message.append("\nThe microservice may behave differently on each platform. Build again later (package mirrors of ")
                .append("the architectures may be out of sync) or use a base image with a fixed Java version.");
        if (!noCache) {
            message.append(" The docker build cache is used (microservice.package.dockerBuildNoCache=false), cached layers ")
                    .append("may contain an older Java version.");
        }
        if (failOnMismatch) {
            throw new MojoExecutionException(message.toString());
        }
        message.append(" Set microservice.package.failOnJavaVersionMismatch=true to fail the build.");
        log.warn(message.toString());
        return false;
    }

    private static String firstLine(String value) {
        int end = value.indexOf('\n');
        return end < 0 ? value : value.substring(0, end);
    }
}

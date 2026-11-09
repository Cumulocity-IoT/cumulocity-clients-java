package com.cumulocity.agent.packaging.microservice.buildx;

import com.cumulocity.agent.packaging.TargetPlatform;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.cumulocity.agent.packaging.TargetPlatform.AMD64;
import static com.cumulocity.agent.packaging.TargetPlatform.ARM64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaVersionCheckTest {

    private static final String JAVA_17_0_21 = "openjdk version \"17.0.21\" 2026-10-21\nOpenJDK Runtime Environment (build 17.0.21+7-alpine-r0)";
    private static final String JAVA_17_0_20 = "openjdk version \"17.0.20\" 2026-07-15\nOpenJDK Runtime Environment (build 17.0.20+8-alpine-r0)";

    @Test
    void shouldPassWithSameVersion() throws MojoExecutionException {
        assertTrue(new JavaVersionCheck(true, true).check(versions(JAVA_17_0_21, JAVA_17_0_21)));
    }

    @Test
    void shouldOnlyWarnOnMismatchByDefault() throws MojoExecutionException {
        assertFalse(new JavaVersionCheck(false, true).check(versions(JAVA_17_0_21, JAVA_17_0_20)));
    }

    @Test
    void shouldFailOnMismatchWhenConfigured() {
        MojoExecutionException exception = assertThrows(MojoExecutionException.class,
                () -> new JavaVersionCheck(true, true).check(versions(JAVA_17_0_21, JAVA_17_0_20)));

        assertTrue(exception.getMessage().contains("different Java versions"), exception.getMessage());
        assertTrue(exception.getMessage().contains("linux/amd64: openjdk version \"17.0.21\""), exception.getMessage());
        assertTrue(exception.getMessage().contains("linux/arm64: openjdk version \"17.0.20\""), exception.getMessage());
        assertFalse(exception.getMessage().contains("build cache"), exception.getMessage());
    }

    @Test
    void shouldMentionBuildCacheWhenUsed() {
        MojoExecutionException exception = assertThrows(MojoExecutionException.class,
                () -> new JavaVersionCheck(true, false).check(versions(JAVA_17_0_21, JAVA_17_0_20)));

        assertTrue(exception.getMessage().contains("dockerBuildNoCache=false"), exception.getMessage());
    }

    @Test
    void shouldFailOnMissingVersionOnlyWhenConfigured() throws MojoExecutionException {
        OciLayoutImage image = mock(OciLayoutImage.class);
        when(image.javaVersions()).thenThrow(new MojoExecutionException("java-version does not exist"));

        assertFalse(new JavaVersionCheck(false, true).check(image));

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> new JavaVersionCheck(true, true).check(image));
        assertTrue(exception.getMessage().contains("Cannot check the Java version"), exception.getMessage());
    }

    private static Map<TargetPlatform, String> versions(String amd64, String arm64) {
        Map<TargetPlatform, String> versions = new LinkedHashMap<>();
        versions.put(AMD64, amd64);
        versions.put(ARM64, arm64);
        return versions;
    }
}

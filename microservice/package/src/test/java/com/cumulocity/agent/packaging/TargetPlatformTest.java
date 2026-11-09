package com.cumulocity.agent.packaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.cumulocity.agent.packaging.TargetPlatform.AMD64;
import static com.cumulocity.agent.packaging.TargetPlatform.ARM64;
import static org.junit.jupiter.api.Assertions.*;

class TargetPlatformTest {

    @ParameterizedTest
    @ValueSource(strings = {"amd64", "x86_64", "linux/amd64", " LINUX/AMD64 "})
    void shouldParseAmd64Aliases(String value) {
        assertEquals(List.of(AMD64), TargetPlatform.parse(List.of(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"arm64", "aarch64", "arm64v8", "linux/arm64", "linux/arm64/v8", "linux/arm64v8"})
    void shouldParseArm64Aliases(String value) {
        assertEquals(List.of(ARM64), TargetPlatform.parse(List.of(value)));
    }

    @Test
    void shouldRemoveDuplicatesAndKeepOrder() {
        assertEquals(List.of(ARM64, AMD64), TargetPlatform.parse(Arrays.asList("arm64", "amd64", "linux/arm64", "")));
    }

    @Test
    void shouldDefaultToAmd64() {
        assertEquals(List.of(AMD64), TargetPlatform.parse(Collections.emptyList()));
        assertEquals(List.of(AMD64), TargetPlatform.parse(null));
    }

    @Test
    void shouldKeepOtherPlatformsAsGiven() {
        List<TargetPlatform> platforms = TargetPlatform.parse(List.of("amd64", " I386 ", "linux/arm/v7", "i386"));

        assertEquals("linux/amd64,i386,linux/arm/v7", TargetPlatform.toPlatformList(platforms));
        assertTrue(platforms.get(0).isSupported());
        assertFalse(platforms.get(1).isSupported());
    }

    @Test
    void shouldListSupportedValues() {
        assertEquals("amd64 (linux/amd64), arm64 (linux/arm64)", TargetPlatform.supportedValues());
    }

    @Test
    void shouldJoinPlatforms() {
        assertEquals("linux/amd64,linux/arm64", TargetPlatform.toPlatformList(List.of(AMD64, ARM64)));
    }
}

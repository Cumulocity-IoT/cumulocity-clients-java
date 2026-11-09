package com.cumulocity.agent.packaging;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Target platform of a container image.
 * <p>
 * Aliases of the platforms supported by Cumulocity ({@link #AMD64}, {@link #ARM64}) are normalized to the docker notation,
 * e.g. {@code aarch64} to {@code linux/arm64}. Any other value is passed to docker as given.
 */
public final class TargetPlatform {

    public static final TargetPlatform AMD64 = new TargetPlatform("linux/amd64", true);
    public static final TargetPlatform ARM64 = new TargetPlatform("linux/arm64", true);

    private static final Map<String, TargetPlatform> ALIASES = Map.ofEntries(
            Map.entry("amd64", AMD64),
            Map.entry("x86_64", AMD64),
            Map.entry("linux/amd64", AMD64),
            Map.entry("arm64", ARM64),
            Map.entry("aarch64", ARM64),
            Map.entry("arm64v8", ARM64),
            Map.entry("linux/arm64", ARM64),
            Map.entry("linux/arm64/v8", ARM64),
            Map.entry("linux/arm64v8", ARM64));

    private static final List<TargetPlatform> SUPPORTED = List.of(AMD64, ARM64);

    private final String platform;
    private final boolean supported;

    private TargetPlatform(String platform, boolean supported) {
        this.platform = platform;
        this.supported = supported;
    }

    /**
     * @return platform in docker notation, e.g. {@code linux/arm64}
     */
    public String platform() {
        return platform;
    }

    /**
     * @return the platform without the {@code linux/} prefix, e.g. {@code arm64}
     */
    public String architecture() {
        return platform.startsWith("linux/") ? platform.substring("linux/".length()) : platform;
    }

    /**
     * @return whether the platform is one of the platforms supported by Cumulocity
     */
    public boolean isSupported() {
        return supported;
    }

    /**
     * @return the normalized platform, or the trimmed and lower-cased value if it is not an alias of a supported platform
     */
    public static Optional<TargetPlatform> of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        TargetPlatform known = ALIASES.get(normalized);
        if (known != null) {
            return Optional.of(known);
        }
        return Optional.of(new TargetPlatform(normalized, false));
    }

    /**
     * Parses user provided platforms. Removes duplicates and keeps the order.
     * An empty input results in the default platform (amd64).
     */
    public static List<TargetPlatform> parse(Collection<String> values) {
        LinkedHashSet<TargetPlatform> result = new LinkedHashSet<>();
        if (values != null) {
            values.forEach(value -> of(value).ifPresent(result::add));
        }
        if (result.isEmpty()) {
            result.add(AMD64);
        }
        return new ArrayList<>(result);
    }

    public static String toPlatformList(Collection<TargetPlatform> platforms) {
        return platforms.stream().map(TargetPlatform::platform).collect(Collectors.joining(","));
    }

    public static String supportedValues() {
        return SUPPORTED.stream().map(p -> p.architecture() + " (" + p.platform + ")").collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TargetPlatform && platform.equals(((TargetPlatform) o).platform);
    }

    @Override
    public int hashCode() {
        return platform.hashCode();
    }

    @Override
    public String toString() {
        return platform;
    }
}

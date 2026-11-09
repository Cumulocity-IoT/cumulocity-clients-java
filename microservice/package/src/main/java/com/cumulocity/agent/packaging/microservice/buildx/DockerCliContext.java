package com.cumulocity.agent.packaging.microservice.buildx;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.File;

/**
 * Settings for invoking the docker CLI from a single mojo execution.
 */
@Value
@Builder(toBuilder = true)
public class DockerCliContext {

    /**
     * Docker CLI binary.
     */
    @NonNull
    @Builder.Default
    String dockerCli = "docker";

    /**
     * Isolated buildx state directory (passed as {@code BUILDX_CONFIG}); keeps the user's builders untouched.
     */
    @NonNull
    File buildxConfigDir;

    /**
     * Prefix of builder names created by the plugin, e.g. {@code c8y-my-service}.
     */
    @NonNull
    String builderNamePrefix;

    /**
     * Timeout of each long-running command (build, export); the commands of one goal do not share it.
     */
    @Builder.Default
    int timeoutSeconds = 360;

    /**
     * Docker configuration directory with registry credentials ({@code docker --config}), may be {@code null}.
     */
    File dockerConfigDir;

    /**
     * Docker daemon address ({@code docker --host}), may be {@code null}. Needed with {@link #dockerConfigDir},
     * because that directory has no docker contexts.
     */
    String dockerHost;
}

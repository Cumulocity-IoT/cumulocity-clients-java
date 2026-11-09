package com.cumulocity.agent.packaging.microservice.buildx;

import com.cumulocity.agent.packaging.TargetPlatform;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class BuildRequest {

    @NonNull
    String builder;

    @NonNull
    File context;

    @Singular
    List<TargetPlatform> platforms;

    @Singular
    Map<String, String> buildArgs;

    @Singular
    List<String> tags;

    /**
     * Network mode for RUN instructions, may be {@code null}.
     */
    String network;

    /**
     * Value of {@code --output}, e.g. {@code type=oci,dest=...}; mutually exclusive with {@link #load}.
     */
    String output;

    /**
     * Load the result into the docker daemon ({@code --load}); several platforms need the containerd image store.
     */
    boolean load;

    /**
     * Additional named build contexts ({@code --build-context name=value}), e.g. {@code oci-layout://...}.
     */
    @Singular
    Map<String, String> buildContexts;

    /**
     * Ignore the build cache and pull base images again ({@code --no-cache --pull}).
     */
    boolean noCache;

    /**
     * Receives the stdout of the build, e.g. the image of {@code --output type=docker,dest=-}; may be {@code null}.
     */
    OutputStream stdout;
}

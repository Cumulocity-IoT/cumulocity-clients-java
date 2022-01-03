package com.cumulocity.agent.packaging;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
public class DockerBuildSpec {

    public static final String DEFAULT_TARGET_DOCKER_IMAGE_PLATFORM="amd64";
    List<String> targetBuildArchitectures = Lists.newArrayList(DEFAULT_TARGET_DOCKER_IMAGE_PLATFORM);

    public static DockerBuildSpec defaultBuildSpec() {
        return new DockerBuildSpec();
    }
}

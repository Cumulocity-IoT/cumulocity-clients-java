package com.cumulocity.agent.packaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.SystemUtils;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class DockerBuildInfo {

    /**
     * The first platform of the image. Kept for compatibility, {@link #imageArchitectures} lists all platforms.
     */
    @Deprecated
    private String imageArch;

    /**
     * All platforms of the image, e.g. {@code [linux/amd64, linux/arm64]}.
     */
    private List<String> imageArchitectures;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Date buildDate;

    private String builderInfo;
    private String hostPlatform;
    private String hostOS;
    private String hostOSVersion;

    public static  DockerBuildInfo defaultInfo () {
        return new DockerBuildInfo();
    }

    public static DockerBuildInfo withBuildArchitecture(String buildArchitecture) {
        DockerBuildInfo result = new DockerBuildInfo();
        result.setImageArch(buildArchitecture);
        return result;
    }

    public DockerBuildInfo withCurrentBuildDate() {
        this.setBuildDate(new Date());
        return this;
    }

    public DockerBuildInfo withHostArchitecture() {
        this.setHostPlatform(SystemUtils.OS_ARCH);
        return this;
    }

    public DockerBuildInfo withHostOS() {
        this.setHostOS(SystemUtils.OS_NAME);
        this.setHostOSVersion(SystemUtils.OS_VERSION);
        return this;
    }

    public DockerBuildInfo withImageArch(String targetArchitecture) {
        this.imageArch = targetArchitecture;
        return this;
    }

    public DockerBuildInfo withImageArchitectures(List<String> imageArchitectures) {
        this.imageArchitectures = imageArchitectures;
        return this;
    }

    public DockerBuildInfo withBuilderInfo(String builderInfo) {
        this.setBuilderInfo(builderInfo);
        return this;
    }


}

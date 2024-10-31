package com.cumulocity.model.application;

import com.cumulocity.model.*;
import com.cumulocity.model.application.microservice.validation.Probe;
import com.cumulocity.model.application.microservice.validation.Provider;
import com.cumulocity.model.application.microservice.validation.ValidContextPath;
import com.cumulocity.model.application.microservice.validation.ValidProbe;
import com.google.common.base.Function;
import lombok.*;
import lombok.Builder.Default;
import org.svenson.JSONProperty;
import org.svenson.JSONTypeHint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.BufferedReader;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyList;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MicroserviceManifest extends JSONBase {

    public enum Isolation {
        PER_TENANT, MULTI_TENANT
    }

    public enum Scale {
        NONE, AUTO
    }

    @NotNull
    @Size(min = 1)
    private String apiVersion;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @ValidContextPath
    private String contextPath;
    @NotNull
    @Size(min = 1)
    private String version;
    @NotNull
    @Valid
    private Provider provider;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private Isolation isolation;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String expose;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private Scale scale;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Min(1)
    @Max(5)
    private Integer replicas;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String price;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Valid
    @ValidProbe
    private Probe livenessProbe;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Valid
    @ValidProbe
    private Probe readinessProbe;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Valid
    private RequestedResources requestedResources;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    @Valid
    private Resources resources;
    @Getter(onMethod_ = {
            @JSONProperty(ignoreIfNull = true),
            @JSONTypeHint(ConfigurationOption.class)
    })
    @Valid
    @Singular
    private List<ConfigurationOption> settings;

    @Pattern(regexp = "[a-zA-Z]+")
    private String settingsCategory;

    private List<String> requiredRoles;

    private List<String> roles;

    @Getter(onMethod_ = {
            @JSONProperty(ignoreIfNull = true),
            @JSONTypeHint(Extension.class)
    })
    @Singular
    private List<Extension> extensions;

    @Default
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private BillingMode billingMode = BillingMode.RESOURCES;

    @JSONProperty(ignoreIfNull = true)
    public List<String> getRequiredRoles() {
        return firstNonNull(requiredRoles, emptyList());
    }

    @JSONProperty(ignoreIfNull = true)
    public List<String> getRoles() {
        return firstNonNull(roles, emptyList());
    }

    public static MicroserviceManifest from(String manifest) {
        return JSONBase.fromJSON(manifest, MicroserviceManifest.class);
    }

    public static MicroserviceManifest from(BufferedReader reader) {
        return JSONBase.fromJSON(reader, MicroserviceManifest.class);
    }

    public static Function<MicroserviceManifest, String> toVersion() {
        return new Function<MicroserviceManifest, String>() {
            @Override
            public String apply(final MicroserviceManifest manifest) {
                return manifest.getVersion();
            }
        };
    }

    public enum BillingMode {
        SUBSCRIPTION, RESOURCES
    }

}

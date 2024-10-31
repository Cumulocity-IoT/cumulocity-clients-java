package com.cumulocity.rest.representation.application;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import com.cumulocity.rest.representation.tenant.TenantReferenceRepresentation;
import lombok.*;
import org.svenson.JSONProperty;
import org.svenson.JSONTypeHint;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "applicationRepresentation")
public class ApplicationRepresentation extends AbstractExtensibleRepresentation {

    public enum Type {
        EXTERNAL, HOSTED, MICROSERVICE, FEATURE, APAMA_CEP_RULE;
    }

    public static final String MICROSERVICE = "MICROSERVICE";

    private String id;

    @Size(max = 128)
    private String name;

    @Size(max = 128)
    private String key;

    private String type;

    @Pattern(regexp = "PRIVATE|MARKET|SHARED")
    private String availability;

    private TenantReferenceRepresentation owner;

    @Size(max = 255)
    private String contextPath;

    @Size(max = 255)
    @Deprecated
    private String resourcesUrl;

    @Deprecated
    private String resourcesUsername;

    @Deprecated
    private String resourcesPassword;

    @Size(max = 255)
    private String externalUrl;

    private ManifestRepresentation manifest;

    private String activeVersionId;

    /**
     * Roles that are required for microservice in order to make requests to platform instance.
     */
    private List<String> requiredRoles;

    /**
     * Roles that are required for users in order to make requests to microservice instance;
     */
    private List<String> roles;

    private List<ApplicationVersionRepresentation> applicationVersions;

    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    @JSONProperty(ignoreIfNull = true)
    public TenantReferenceRepresentation getOwner() {
        return owner;
    }

    public void setOwner(TenantReferenceRepresentation owner) {
        this.owner = owner;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    @Deprecated
    @JSONProperty(ignoreIfNull = true)
    public String getResourcesUrl() {
        return resourcesUrl;
    }

    @Deprecated
    public void setResourcesUrl(String resourcesUrl) {
        this.resourcesUrl = resourcesUrl;
    }

    @Deprecated
    @JSONProperty(ignoreIfNull = true)
    public String getResourcesUsername() {
        return resourcesUsername;
    }

    @Deprecated
    public void setResourcesUsername(String resourcesUsername) {
        this.resourcesUsername = resourcesUsername;
    }

    @Deprecated
    @JSONProperty(ignoreIfNull = true)
    public String getResourcesPassword() {
        return resourcesPassword;
    }

    @Deprecated
    public void setResourcesPassword(String resourcesPassword) {
        this.resourcesPassword = resourcesPassword;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    @JSONProperty(ignoreIfNull = true)
    public ManifestRepresentation getManifest() {
        return manifest;
    }

    public void setManifest(ManifestRepresentation manifest) {
        this.manifest = manifest;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getActiveVersionId() {
        return activeVersionId;
    }

    public void setActiveVersionId(String activeVersionId) {
        this.activeVersionId = activeVersionId;
    }

    @JSONProperty(ignoreIfNull = true)
    public List<String> getRequiredRoles() {
        return requiredRoles;
    }

    @JSONProperty(ignoreIfNull = true)
    public List<String> getRoles() {
        return roles;
    }

    @JSONProperty(ignoreIfNull = true)
    @JSONTypeHint(ApplicationVersionRepresentation.class)
    public List<ApplicationVersionRepresentation> getApplicationVersions() {
        return applicationVersions;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getUrl() {
        return url;
    }

}

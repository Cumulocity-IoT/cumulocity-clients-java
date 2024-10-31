package com.cumulocity.rest.representation.user;

import com.cumulocity.rest.representation.CustomPropertiesMapRepresentation;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import org.svenson.JSONProperty;
import org.svenson.JSONTypeHint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.Size;

public class GroupRepresentation extends CustomPropertiesMapRepresentation {

    private Long id;

    @Size(max = 254, message = "maximum length is 254 characters")
    private String name;

    private String description;

    private UserReferenceCollectionRepresentation users;

    private RoleReferenceCollectionRepresentation roles;

    private Map<String, List<String>> devicePermissions;

    private List<ApplicationRepresentation> applications = new ArrayList<ApplicationRepresentation>();

    public void setId(Long id) {
        this.id = id;
    }

    @JSONProperty(ignoreIfNull = true)
    public Long getId() {
        return id;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JSONProperty(ignoreIfNull = true)
    public UserReferenceCollectionRepresentation getUsers() {
        return users;
    }

    public void setUsers(UserReferenceCollectionRepresentation users) {
        this.users = users;
    }

    @JSONProperty(ignoreIfNull = true)
    public RoleReferenceCollectionRepresentation getRoles() {
        return roles;
    }

    public void setRoles(RoleReferenceCollectionRepresentation roles) {
        this.roles = roles;
    }

    @JSONProperty(ignoreIfNull = true)
    public Map<String, List<String>> getDevicePermissions() {
        return devicePermissions;
    }

    public void setDevicePermissions(Map<String, List<String>> devicePermissions) {
        this.devicePermissions = devicePermissions;
    }

    @JSONProperty(ignoreIfNull = true)
    public List<ApplicationRepresentation> getApplications() {
        return applications;
    }

    @JSONTypeHint(ApplicationRepresentation.class)
    public void setApplications(List<ApplicationRepresentation> applications) {
        this.applications = applications;
    }
}

package com.cumulocity.rest.representation.user;

import com.cumulocity.model.DateTimeConverter;
import com.cumulocity.rest.representation.CustomPropertiesMapRepresentation;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import org.joda.time.DateTime;
import org.svenson.JSONProperty;
import org.svenson.JSONTypeHint;
import org.svenson.converter.JSONConverter;

import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.cumulocity.model.util.DateTimeUtils.newUTC;
import static com.google.common.base.MoreObjects.firstNonNull;

public class UserRepresentation extends CustomPropertiesMapRepresentation {

    private String id;

    @Size(max = 1000)
    private String userName;

    private String owner;

    private String delegatedBy;

    private String password;

    private String firstName;

    private String lastName;

    @Size(max = 256, message = "maximum length is 256 characters")
    private String phone;

    @Size(max = 256, message = "maximum length is 256 characters")
    private String email;

    private String passwordStrength;

    private Boolean shouldResetPassword;

    private Boolean supportUserEnabled;

    private DateTime lastPasswordChange;

    private Boolean enabled;

    private Map<String, List<String>> devicePermissions;

    private GroupReferenceCollectionRepresentation groups;

    private RoleReferenceCollectionRepresentation roles;

    private List<ApplicationRepresentation> applications;

    private Boolean sendPasswordResetEmail;

    private Boolean twoFactorAuthenticationEnabled;

    private Boolean newsletter;

    private Integer subusersCount;

    private String displayName;

    @JSONProperty(ignoreIfNull = true)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getDelegatedBy() {
        return delegatedBy;
    }

    public void setDelegatedBy(String delegatedBy) {
        this.delegatedBy = delegatedBy;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getPasswordStrength() {
        return passwordStrength;
    }

    public void setPasswordStrength(String passwordStrength) {
        this.passwordStrength = passwordStrength;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getShouldResetPassword() {
        return shouldResetPassword;
    }

    public void setShouldResetPassword(Boolean shouldResetPassword) {
        this.shouldResetPassword = shouldResetPassword;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getSupportUserEnabled() {
        return supportUserEnabled;
    }

    public void setSupportUserEnabled(Boolean supportUserEnabled) {
        this.supportUserEnabled = supportUserEnabled;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @JSONProperty(ignoreIfNull = true)
    public Map<String, List<String>> getDevicePermissions() {
        return devicePermissions;
    }

    public void setDevicePermissions(Map<String, List<String>> devicePermissions) {
        this.devicePermissions = devicePermissions;
    }

    @JSONProperty(ignoreIfNull = true)
	public GroupReferenceCollectionRepresentation getGroups() {
		return groups;
	}

	public void setGroups(GroupReferenceCollectionRepresentation groups) {
		this.groups = groups;
	}

	@JSONProperty(ignoreIfNull = true)
	public RoleReferenceCollectionRepresentation getRoles() {
		return roles;
	}

	public void setRoles(RoleReferenceCollectionRepresentation roles) {
		this.roles = roles;
	}

	@JSONProperty(ignoreIfNull = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JSONProperty(value = "deprecated_LastPasswordChange", ignore = true)
    @Deprecated
    public Date getLastPasswordChange() {
        return lastPasswordChange == null ? null : lastPasswordChange.toDate();
    }

    @Deprecated
    public void setLastPasswordChange(Date lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange == null ? null : newUTC(lastPasswordChange);
    }

    @JSONProperty(value = "lastPasswordChange", ignoreIfNull = true)
    @JSONConverter(type = DateTimeConverter.class)
    public DateTime getLastPasswordChangeDateTime() {
        return lastPasswordChange;
    }

    public void setLastPasswordChangeDateTime(DateTime lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    @JSONProperty(ignoreIfNull = true)
    public List<ApplicationRepresentation> getApplications() {
        return applications;
    }

    @JSONTypeHint(ApplicationRepresentation.class)
    public void setApplications(List<ApplicationRepresentation> applications) {
        this.applications = applications;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getSendPasswordResetEmail() {
        return sendPasswordResetEmail;
    }

    @JSONProperty(ignore = true)
    public boolean shouldSendPasswordResetEmail() {
        return firstNonNull(sendPasswordResetEmail, false);
    }

    @JSONProperty(ignoreIfNull = true)
    public void setSendPasswordResetEmail(Boolean sendPasswordResetEmail) {
        this.sendPasswordResetEmail = sendPasswordResetEmail;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getTwoFactorAuthenticationEnabled() {
        return twoFactorAuthenticationEnabled;
    }

    public void setTwoFactorAuthenticationEnabled(Boolean twoFactorAuthenticationEnabled) {
        this.twoFactorAuthenticationEnabled = twoFactorAuthenticationEnabled;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getNewsletter() {
        return newsletter;
    }

    public void setNewsletter(Boolean newsletter) {
        this.newsletter = newsletter;
    }

    @JSONProperty(ignoreIfNull = true)
    public Integer getSubusersCount() {
        return subusersCount;
    }

    public void setSubusersCount(Integer subusersCount) {
        this.subusersCount = subusersCount;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

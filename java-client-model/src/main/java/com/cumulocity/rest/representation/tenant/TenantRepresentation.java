package com.cumulocity.rest.representation.tenant;

import com.cumulocity.model.DateTimeConverter;
import com.cumulocity.model.IDTypeConverter;
import com.cumulocity.model.JSONBase;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.CustomPropertiesMapRepresentation;
import com.cumulocity.rest.representation.application.ApplicationReferenceCollectionRepresentation;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;
import org.svenson.JSONProperty;
import org.svenson.converter.JSONConverter;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Date;

public class TenantRepresentation extends CustomPropertiesMapRepresentation {

    private String id;

    @Size(max = 256)
    private String domain;

    @Pattern(regexp = "ACTIVE|SUSPENDED")
    private String status;

    @Size(max = 256)
    private String company;

    @Size(max = 50)
    private String adminName;

    @Size(max = 32)
    private String adminPass;

    private String adminEmail;

    @Size(max = 30)
    private String contactName;

    private String contactPhone;

    private ApplicationReferenceCollectionRepresentation applications;

    private ApplicationReferenceCollectionRepresentation ownedApplications;

    private Boolean sendPasswordResetEmail;

    private DateTime dateCreated;

    private SupportUserDetailsRepresentation supportUser;

    @Size(max = 32)
    private String parent;

    private Boolean allowCreateTenants;

    private Long storageLimitPerDevice;

    private GId tenantPolicyId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    @JSONProperty(ignoreIfNull = true)
    public ApplicationReferenceCollectionRepresentation getApplications() {
        return applications;
    }

    public void setApplications(ApplicationReferenceCollectionRepresentation applications) {
        this.applications = applications;
    }

    @JSONProperty(ignoreIfNull = true)
    public ApplicationReferenceCollectionRepresentation getOwnedApplications() {
        return ownedApplications;
    }

    public void setOwnedApplications(ApplicationReferenceCollectionRepresentation ownedApplications) {
        this.ownedApplications = ownedApplications;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getAdminPass() {
        return adminPass;
    }

    public void setAdminPass(String adminPass) {
        this.adminPass = adminPass;
    }

    @JSONProperty(ignoreIfNull = true)
    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    @JSONProperty(ignoreIfNull = true)
    public Boolean getSendPasswordResetEmail() {
        return sendPasswordResetEmail;
    }

    @JSONProperty(ignore = true)
    public boolean shouldSendPasswordResetEmail() {
        return MoreObjects.firstNonNull(sendPasswordResetEmail, false);
    }

    public void setSendPasswordResetEmail(Boolean sendPasswordResetEmail) {
        this.sendPasswordResetEmail = sendPasswordResetEmail;
    }

    @JSONProperty(ignoreIfNull = true)
    public SupportUserDetailsRepresentation getSupportUser() {
        return supportUser;
    }

    public void setSupportUser(SupportUserDetailsRepresentation supportUser) {
        this.supportUser = supportUser;
    }

    @JSONProperty(value = "deprecated_DateCreated", ignore = true)
    @Deprecated
    public Date getDateCreated() {
        return dateCreated == null ? null : dateCreated.toDate();
    }

    @JSONProperty(value = "creationTime", ignoreIfNull = true)
    @JSONConverter(type = DateTimeConverter.class)
    public DateTime getCreatedDateTime() {
        return dateCreated;
    }

    public void setCreatedDateTime(DateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public String toJSON() {
        return JSONBase.getJSONGenerator().forValue(this);
    }

    @JSONProperty(ignoreIfNull = true)
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@JSONProperty(ignoreIfNull = true)
	public Boolean getAllowCreateTenants() {
		return allowCreateTenants;
	}

	public void setAllowCreateTenants(Boolean allowCreateTenants) {
		this.allowCreateTenants = allowCreateTenants;
	}

    @JSONProperty(ignoreIfNull = true)
    public Long getStorageLimitPerDevice() {
        return storageLimitPerDevice;
    }

    public void setStorageLimitPerDevice(Long storageLimitPerDevice) {
        this.storageLimitPerDevice = storageLimitPerDevice;
    }

    public void setDateCreated(DateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @JSONProperty(ignoreIfNull = true)
    @JSONConverter(type = IDTypeConverter.class)
    public GId getTenantPolicyId() {
        return tenantPolicyId;
    }

    public void setTenantPolicyId(GId tenantPolicyId) {
        this.tenantPolicyId = tenantPolicyId;
    }
}

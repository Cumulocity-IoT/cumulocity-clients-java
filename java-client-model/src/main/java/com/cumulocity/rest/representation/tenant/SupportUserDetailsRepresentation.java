package com.cumulocity.rest.representation.tenant;

import com.cumulocity.model.DateTimeConverter;
import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import org.joda.time.DateTime;
import org.svenson.JSONProperty;
import org.svenson.converter.JSONConverter;

import jakarta.validation.constraints.NotNull;

public class SupportUserDetailsRepresentation extends AbstractExtensibleRepresentation {

    @NotNull
    private Boolean enabled;

    private DateTime expiryDate;

    private Integer activeRequestCount;

    @JSONProperty(value = "enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @JSONProperty(value = "expiryDate")
    @JSONConverter(type = DateTimeConverter.class)
    public DateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(DateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    @JSONProperty(value = "activeRequestCount")
    public Integer getActiveRequestCount() {
        return activeRequestCount;
    }

    public void setActiveRequestCount(Integer activeRequestCount) {
        this.activeRequestCount = activeRequestCount;
    }
}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.device.registrant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegisterProperties {

    @NotNull
    protected DeviceType deviceType;

    @Null
    private String uplinkCallback;

    @NotBlank
    private String lnsConnectionName;

    @JsonIgnore
    public DeviceType getDeviceType() {
        return deviceType;
    }

    @JsonProperty("deviceType")
    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getUplinkCallback() {
        return uplinkCallback;
    }

    public void setUplinkCallback(String uplinkCallback) {
        this.uplinkCallback = uplinkCallback;
    }

}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec;

import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * The <b>Codec</b> interface exposes methods to provide the uniquely supported devices. The class which implements this interface should be annotated with "@Component".
 */
public interface Codec {

    /**
     * This method returns a set of uniquely supported devices w.r.t the device manufacturer and the device model.
     *
     * @return Set
     */
    @NotNull @NotEmpty Set<DeviceInfo> supportsDevices();
}

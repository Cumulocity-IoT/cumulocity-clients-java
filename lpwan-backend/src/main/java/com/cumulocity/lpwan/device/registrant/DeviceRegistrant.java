/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.device.registrant;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;

public interface DeviceRegistrant<T extends DeviceRegisterProperties, R extends RegisterResponse> {

    /**
     * Register device in the Cumulocity platform.
     *
     * @param registerResponse the register response
     * @param deviceRegisterProperties the device register properties.
     *
     * @return ManagedObjectRepresentation of the created device in Cumulocity platform
     */
    ManagedObjectRepresentation registerDeviceInC8Y(R registerResponse, T deviceRegisterProperties);

    /**
     * Register device in the LPWAN provider platform.
     *
     * @param deviceRegisterProperties the device register properties.
     *
     * @return Response of the registration
     */
    R registerDeviceInProvider(T deviceRegisterProperties);
}

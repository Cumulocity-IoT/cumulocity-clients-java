package com.cumulocity.lpwan.device.registrant;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;

import lombok.Data;

@Data
public class DeviceRegistrantService {

    private DeviceRegistrant deviceRegistrant;
    
    public DeviceRegistrantService(DeviceRegistrant deviceRegistrant) {
        this.deviceRegistrant = deviceRegistrant;
    }

    /**
     * Executes calls to registers device first in lpwan provider and then in the Cumulocity platform.
     *
     * @param deviceRegisterProperties the device register properties.
     *
     * @return ManagedObjectRepresentation of the created device in the Cumulocity platform.
     */
    public ManagedObjectRepresentation newRegisterRequest(DeviceRegisterProperties deviceRegisterProperties) {
        RegisterResponse response = deviceRegistrant.registerDeviceInProvider(deviceRegisterProperties);
        return deviceRegistrant.registerDeviceInC8Y(response, deviceRegisterProperties);
    }

}

package com.cumulocity.sdk.client.notification2;

import lombok.Getter;

/**
 * Target APIs that subscribers can subscribe to in device context (per device)
 */
public enum DeviceContextTargetApi {
    /**
     * Events created on the device
     */
    EVENTS("events"),
    /**
     * Events created on the device and its child devices
     */
    EVENTS_WITH_CHILDREN("eventsWithChildren"),
    /**
     * Alarms created on the device
     */
    ALARMS("alarms"),
    /**
     * Alarms created on the device and its child devices
     */
    ALARMS_WITH_CHILDREN("alarmsWithChildren"),
    /**
     * Measurements created on the device
     */
    MEASUREMENTS("measurements"),
    /**
     * Managed object modifications
     */
    MANAGED_OBJECTS("managedobjects"),
    /**
     * Operations created on the device
     */
    OPERATIONS("operations"),
    /**
     * All APIs supported in device context, can't be combined with other targets
     */
    ALL("*");

    @Getter
    private final String target;

    DeviceContextTargetApi(String target) {
        this.target = target;
    }
}

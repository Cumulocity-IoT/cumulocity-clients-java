package com.cumulocity.sdk.client.notification2;

import lombok.Getter;

/**
 * APIs that subscribers can subscribe to in tenant context
 */
@SuppressWarnings("unused")
public enum TenantContextTargetApi {
    /**
     * Alarms in tenant
     */
    ALARMS("alarms"),
    /**
     * Events in tenant
     */
    EVENTS("events"),
    /**
     * Inventory objects in tenant
     */
    MANAGED_OBJECTS("managedobjects"),
    /**
     * Operations in tenant
     */
    OPERATIONS("operations"),
    /**
     * All APIs supported in tenant context, can't be combined with other targets
     */
    ALL("*");

    @Getter
    private final String target;

    TenantContextTargetApi(String target) {
        this.target = target;
    }
}

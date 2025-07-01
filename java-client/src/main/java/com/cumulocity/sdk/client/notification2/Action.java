package com.cumulocity.sdk.client.notification2;

/**
 * Represents the logical event that generated the notification, such as a CREATE of an alarm or measurement.
 */
public enum Action {
    CREATE, UPDATE, DELETE, NONE
}

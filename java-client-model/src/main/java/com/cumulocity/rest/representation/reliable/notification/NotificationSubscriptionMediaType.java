package com.cumulocity.rest.representation.reliable.notification;

import com.cumulocity.rest.representation.CumulocityMediaType;

import jakarta.ws.rs.core.MediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name).
 */
public class NotificationSubscriptionMediaType extends CumulocityMediaType {

    public static final NotificationSubscriptionMediaType NOTIFICATION_SUBSCRIPTION = new NotificationSubscriptionMediaType("subscription");

    public static final String NOTIFICATION_SUBSCRIPTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "subscription+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final NotificationSubscriptionMediaType NOTIFICATION_SUBSCRIPTION_COLLECTION = new NotificationSubscriptionMediaType("subscriptionCollection");

    public static final String NOTIFICATION_SUBSCRIPTION_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "subscriptionCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final NotificationSubscriptionMediaType NOTIFICATION_SUBSCRIPTION_API = new NotificationSubscriptionMediaType("subscriptionApi");

    public static final String NOTIFICATION_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "notificationApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public NotificationSubscriptionMediaType(String string) {
        super(string);
    }
}

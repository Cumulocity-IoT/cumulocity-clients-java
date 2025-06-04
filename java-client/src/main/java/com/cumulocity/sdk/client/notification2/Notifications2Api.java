package com.cumulocity.sdk.client.notification2;

import com.cumulocity.sdk.client.notification2.exception.Notifications2SubscriptionAlreadyEstablishedException;

import java.util.Optional;

/**
 * Simplified API for Notifications 2.0 that hides most of the complexity and allows easy usage of these features.
 */
@SuppressWarnings("unused")
public interface Notifications2Api {
    /**
     * Makes new subscription based on requested definition.
     * <br/><br/>
     * If connection is already existing and active, {@link Notifications2SubscriptionAlreadyEstablishedException} will be thrown
     *
     * @param subscription subscription definition
     * @param listener     notification listener
     */
    void subscribe(Subscription subscription, NotificationListener listener);

    /**
     * Closes server connection and stops given subscription
     * <br/><br/>
     * If connection is not existing or not active, nothing will happen.
     *
     * @param subscriptionId subscription identifier to unsubscribe
     * @param unsubscribe true if token should be unsubscribed (only taken into account for persistent targets)
     */
    void disconnect(Subscription.ID subscriptionId, boolean unsubscribe);

    /**
     * Used to clean up platform resources created for given subscription.
     * <br/><br/>
     * <b>WARNING!</b> Please be aware that deleting subscription will close all of its connections - even from other applications (i.e. instances of the same microservice).
     *
     * @param subscriptionId subscription identifier to delete
     */
    void delete(Subscription.ID subscriptionId);

    /**
     * Finds and returns low-level web socket object if one was created for subscription
     *
     * @param subscriptionId subscription identifier
     */
    Optional<Object> getRawWebSocket(Subscription.ID subscriptionId);
}

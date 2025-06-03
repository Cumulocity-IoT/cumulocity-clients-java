package com.cumulocity.sdk.client.messaging.notifications;

import com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionRepresentation;
import com.cumulocity.sdk.client.SDKException;

/**
 * Manage notification subscriptions
 */
public interface NotificationSubscriptionApi {

    /**
     * Creates a subscription to a source.
     * 
     * @param representation initial values for subscription
     * @return subscription populated with an id
     * @throws SDKException 
     */
    NotificationSubscriptionRepresentation subscribe(NotificationSubscriptionRepresentation representation) throws SDKException;
    
    /**
     * Gets all the subscriptions.
     * 
     * @return all the subscriptions
     * @throws SDKException 
     */
    NotificationSubscriptionCollection getSubscriptions() throws SDKException;

    /**
     * Gets all the subscriptions matching a filter. If the filter is null,
     * return all subscriptions.
     * 
     * @param filter values to be matched on
     * @return subscriptions matching values
     * @throws SDKException 
     */
    NotificationSubscriptionCollection getSubscriptionsByFilter(NotificationSubscriptionFilter filter) throws SDKException;
    
    /**
     * Delete by object.
     * 
     * @param subscription to delete
     * @throws SDKException 
     */
    void delete(NotificationSubscriptionRepresentation subscription) throws SDKException;
    
    /**
     * Delete by ID.
     * 
     * @param subscriptionId of subscription to delete
     * @throws SDKException 
     */
    void deleteById(String subscriptionId) throws SDKException;

    /**
     * Deletes all subscriptions matching a given filter.
     *
     * @deprecated This method is deprecated and will throw an exception if the filter includes
     * {@code subscription}, {@code typeFilter} or both.
     * Use {@link #delete(NotificationSubscriptionRepresentation)},
     * {@link #deleteById(String)}, or {@link #deleteBySource(String)} instead.
     *
     * @param filter the filter criteria for deleting matching subscriptions
     * @throws SDKException if the request fails or if disallowed filters are present
     */
    @Deprecated
    void deleteByFilter(NotificationSubscriptionFilter filter) throws SDKException;
    
    /**
     * Deletes all subscriptions to a source in managed object context.
     * 
     * @param source 
     */
    void deleteBySource(String source);

    /**
     * Deletes all subscriptions of the current tenant.
     */
    void deleteTenantSubscriptions();
}

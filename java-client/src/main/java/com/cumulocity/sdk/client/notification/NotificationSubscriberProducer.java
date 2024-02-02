package com.cumulocity.sdk.client.notification;

import java.util.HashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.rest.representation.notification.NotificationRepresentation;
import com.cumulocity.rest.representation.notification.NotificationRepresentation.*;
import com.cumulocity.sdk.client.notification.Subscriber;
import com.cumulocity.sdk.client.notification.SubscriberBuilder;

/**
 * Utility class for creating {@linkplain Subscriber subscribers} to the real-time
 * notification API.
 *
 * This class is used as follows (example):
 * <pre>{@code
 * // 'parameters' object of type PlatformParameters must be provided
 * NotificationSubscriberProducer producer = new NotificationSubscriberProducer(parameters);
 * producer.getSubscriber(Endpoint.RealtimeNotifications, AlarmNotificationRepresentation.class)
 * .subscribe(new GId("*"), new SubscriptionListener<>() { ... });
 * }</pre>
 *
 * When implementing a microservice, an instance of this class can also be obtained
 * as autowired bean:
 * <pre>{@code
 * @Autowired
 * private NotificationSubscriberProducer producer;
 * }</pre>
 * With this setup, a bean instance that uses the correct platform parameters is
 * automatically provided depending on the context (tenant scope or user scope)
 * where it is used.
 */
public class NotificationSubscriberProducer {

    /**
     * Enumeration of the available real-time notification endpoints.
     * <ul>
     * <li>{@link #RealtimeNotifications}</li>
     * <li>{@link #OperationNotifications}</li>
     * </ul>
     */
    public enum Endpoint {
        /**
         * Generic real-time notification endpoint.
         * This endpoint is usable for receiving notifications for any type of
         * domain model objects (Measurements, Events, etc.) that supports
         * real-time notifications.
         */
        RealtimeNotifications("notification/realtime"),
        /**
         * Special real-time notification endpoint for Operations.
         * Use this endpoint for receiving operations for Agents (i.&thinsp;e.
         * Managed Objects having a {@code com_cumulocity_model_Agent} fragment).
         * For receiving operations for regular Devices (having a {@code c8y_IsDevice}
         * fragment), use the {@link #RealtimeNotifications} endpoint instead.
         */
        OperationNotifications("notification/operations");

        /**
         * The URL path of this endpoint.
         */
        public final String path;

        Endpoint(String path) {
            this.path = path;
        }
    }

    private static final HashMap<
            ImmutablePair<Endpoint, Class<? extends NotificationRepresentation<?>>>,
            String /*subscriptionBase*/>
            subscriptionBaseMap = new HashMap<>();
    static {
        subscriptionBaseMap.put(
                new ImmutablePair<>(Endpoint.RealtimeNotifications, ManagedObjectNotificationRepresentation.class),
                "/managedobjects/");
        subscriptionBaseMap.put(
                new ImmutablePair<>(Endpoint.RealtimeNotifications, MeasurementNotificationRepresentation.class),
                "/measurements/");
        subscriptionBaseMap.put(
                new ImmutablePair<>(Endpoint.RealtimeNotifications, EventNotificationRepresentation.class),
                "/events/");
        subscriptionBaseMap.put(
                new ImmutablePair<>(Endpoint.RealtimeNotifications, AlarmNotificationRepresentation.class),
                "/alarms/");
        subscriptionBaseMap.put(
                new ImmutablePair<>(Endpoint.RealtimeNotifications, OperationNotificationRepresentation.class),
                "/operations/");
        subscriptionBaseMap.put(
                new ImmutablePair<>(Endpoint.OperationNotifications, OperationNotificationRepresentation.class),
                "/");
    }

    private final PlatformParameters parameters;

    /**
     * The constructor of this class.
     *
     * @param parameters  the information needed for accessing the real-time
     * notification API (tenant address, credentials, etc.) 
     */
    public NotificationSubscriberProducer(PlatformParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Create a {@link Subscriber} to the given endpoint for receiving real-time
     * notifications of the given data type.
     *
     * The data type depends on the type of domain model objects for which
     * notifications shall be received. Use the following types:
     * <ul>
     * <li>{@link AlarmNotificationRepresentation} for notifications on Alarms</li>
     * <li>{@link EventNotificationRepresentation} for notifications on Events</li>
     * <li>{@link ManagedObjectNotificationRepresentation} for notifications on Managed Objects</li>
     * <li>{@link MeasurementNotificationRepresentation} for notifications on Measurements</li>
     * <li>{@link OperationNotificationRepresentation} for notifications on Operations</li>
     * </ul>
     *
     * @param <T>  the desired data type as indicated by the {@code type} parameter
     * @param endpoint  the desired real-time notification endpoint
     * @param type  the class of the desired data type
     * @return a subscriber with the specified configuration
     */
    public <T extends NotificationRepresentation<?>>
            Subscriber<GId, T> getSubscriber(Endpoint endpoint, Class<T> type) {

        String subscriptionBase = subscriptionBaseMap.get(new ImmutablePair<>(endpoint, type));
        if (subscriptionBase == null)
            throw new IllegalArgumentException("Invalid combination of endpoint and type.");
        return new SubscriberBuilder<GId, T>()
                .withParameters(parameters)
                .withEndpoint(endpoint.path)
                .withSubscriptionNameResolver(id -> subscriptionBase + id.getValue())
                .withDataType(type)
                .withMessageDeliveryAcknowlage(true)
                .build();
    }

}

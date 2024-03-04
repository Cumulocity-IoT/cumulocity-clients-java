package com.cumulocity.sdk.client.notification;

import java.util.Map;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.notification.NotificationRepresentation;
import com.cumulocity.rest.representation.notification.NotificationRepresentation.*;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.google.common.collect.ImmutableMap;

/**
 * Utility class for creating {@linkplain Subscriber subscribers} to the real-time
 * notification API.
 *
 * This class is used as follows (example):
 * <pre>{@code
 * NotificationSubscriberProducer producer = new NotificationSubscriberProducer(parameters);
 * producer.getSubscriber(Endpoint.RealtimeNotifications, NotificationType.ALARM)
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
     * This utility class has a constant for each domain model object type
     * that supports notifications. These constants are used as inputs
     * to some methods in the context of the enclosing class.
     *
     * @param <S>  the type of domain model object
     * @param <T>  the type of notification object corresponding to
     *     the domain model object type
     */
    // This could be an Enum, if JEP 301 (enhanced enums with sharper typing) was available.
    public static final class NotificationType<S extends AbstractExtensibleRepresentation, T extends NotificationRepresentation<S>> {
        public static final NotificationType<ManagedObjectRepresentation, ManagedObjectNotificationRepresentation>
                MANAGED_OBJECT = new NotificationType<>(ManagedObjectRepresentation.class, ManagedObjectNotificationRepresentation.class);
        public static final NotificationType<MeasurementRepresentation, MeasurementNotificationRepresentation>
                MEASUREMENT = new NotificationType<>(MeasurementRepresentation.class, MeasurementNotificationRepresentation.class);
        public static final NotificationType<EventRepresentation, EventNotificationRepresentation>
                EVENT = new NotificationType<>(EventRepresentation.class, EventNotificationRepresentation.class);
        public static final NotificationType<AlarmRepresentation, AlarmNotificationRepresentation>
                ALARM = new NotificationType<>(AlarmRepresentation.class, AlarmNotificationRepresentation.class);
        public static final NotificationType<OperationRepresentation, OperationNotificationRepresentation>
                OPERATION = new NotificationType<>(OperationRepresentation.class, OperationNotificationRepresentation.class);

        public final Class<S> resourceRepresentation;
        public final Class<T> notificationRepresentation;

        private NotificationType(Class<S> resourceRepresentation, Class<T> notificationRepresentation) {
            this.resourceRepresentation = resourceRepresentation;
            this.notificationRepresentation = notificationRepresentation;
        }
    }

    /**
     * Enumeration of the available real-time notification endpoints.
     * <ul>
     * <li>{@link #RealtimeNotifications}</li>
     * <li>{@link #OperationNotifications}</li>
     * </ul>
     */
    public enum Endpoint {
        /**
         * Generic real-time notification endpoint “{@code notification/realtime}”.
         * This endpoint is usable for receiving notifications for any type of
         * domain model objects (Measurements, Events, etc.) that supports
         * real-time notifications.
         */
        RealtimeNotifications("notification/realtime",
                ImmutableMap.of(
                        NotificationType.MANAGED_OBJECT, "/managedobjects/",
                        NotificationType.MEASUREMENT, "/measurements/",
                        NotificationType.EVENT, "/events/",
                        NotificationType.ALARM, "/alarms/",
                        NotificationType.OPERATION, "/operations/")),

        /**
         * Real-time notification endpoint “{@code notification/operations}”.
         * Use this endpoint for receiving operations for Agents (i.&thinsp;e.
         * Managed Objects having a {@code com_cumulocity_model_Agent} fragment).
         * For receiving operations for regular Devices (having a {@code c8y_IsDevice}
         * fragment), use the {@link #RealtimeNotifications} endpoint instead.
         */
        OperationNotifications("notification/operations",
                ImmutableMap.of(NotificationType.OPERATION, "/"));

        /**
         * The URL path of this endpoint.
         */
        public final String path;

        /**
         * This map provides the prefix of subscription channels (like {@code "/alarms/"})
         * for each supported data type.
         */
        public final Map<NotificationType<?,?>, String> supportedChannelPrefixes;

        Endpoint(String path, Map<NotificationType<?,?>, String> prefixes) {
            this.path = path;
            this.supportedChannelPrefixes = prefixes;
        }
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
     * Creates a subscriber to the given endpoint for receiving real-time
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
     * @param type  the desired type of notification
     * @return a subscriber with the specified configuration
     */
    public <T extends NotificationRepresentation<?>>
            Subscriber<GId, T> getSubscriber(Endpoint endpoint, NotificationType<?,T> type) {

        String channelPrefix = endpoint.supportedChannelPrefixes.get(type);
        if (channelPrefix == null)
            throw new IllegalArgumentException("Invalid combination of endpoint and type.");
        return new SubscriberBuilder<GId, T>()
                .withParameters(parameters)
                .withEndpoint(endpoint.path)
                .withSubscriptionNameResolver(id -> channelPrefix + id.getValue())
                .withDataType(type.notificationRepresentation)
                .withMessageDeliveryAcknowlage(true)
                .build();
    }

}

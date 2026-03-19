package com.cumulocity.sdk.client.notification2.internal;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionFilterRepresentation;
import com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionCollection;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionFilter;
import com.cumulocity.sdk.client.notification2.NotificationListener;
import com.cumulocity.sdk.client.notification2.Notifications2Api;
import com.cumulocity.sdk.client.notification2.Subscription;
import com.cumulocity.sdk.client.notification2.config.Notifications2Properties;
import com.cumulocity.sdk.client.notification2.exception.Notifications2NotEnabledException;
import com.cumulocity.sdk.client.notification2.exception.Notifications2SubscriptionAlreadyEstablishedException;
import com.cumulocity.sdk.client.util.StringUtils;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Implements {@link Notifications2Api} based on our latest Core/Pulsar magic. Hides most of the complexity from the final subscriber.
 * <br/><br/>
 * There are two main components encapsulating notifications logic:
 * <ul>
 *     <li>this class - keeps internal subscriptions cache and creates necessary platform objects</li>
 *     <li>{@link WebSocketClient} - created for each subscription, responsible for establishing and maintaining websocket connection and tokens management</li>
 * </ul>
 */
@Slf4j
public class Notifications2ApiImpl implements Notifications2Api {
    public static final String CONTEXT_DEVICE = "mo";
    public static final String CONTEXT_TENANT = "tenant";
    // created locally
    private final Map<Subscription.ID, WebSocketClient> clientMap = new ConcurrentHashMap<>();

    // constructor args
    private final Notifications2Properties notifications2Properties;
    private final String tenantId;
    private final Platform platform;

    @Setter(AccessLevel.PACKAGE) // for unit tests
    private BiFunction<Subscription, NotificationListener, WebSocketClient> clientFactoryFunction;

    public Notifications2ApiImpl(Notifications2Properties notifications2Properties, String tenantId, Platform platform) {
        this.notifications2Properties = notifications2Properties;
        this.tenantId = tenantId;
        this.platform = platform;
        clientFactoryFunction = this::createClient;
        if (this.notifications2Properties.isDisabled()) {
            log.info("C8Y.notifications2.websocketUrl is empty - Notifications 2.0 will be disabled");
        }
    }

    private WebSocketClient createClient(Subscription subscription, NotificationListener listener) {
        return new WebSocketClient(notifications2Properties.getWebsocketUrl(), subscription.getId().getSubscriber(), subscription.getId().getName(), subscription.getAckMode(),
                tenantId, subscription.getDeviceId(), listener,
                Duration.ofSeconds(5L), notifications2Properties.getTokenRefreshInterval(), subscription.isShared(), subscription.isPersistent(),
                platform, new TooTallNateWebSocketConnector());
    }

    @Override
    public void subscribe(Subscription subscription, NotificationListener listener) {
        ensureNotifications2Enabled();
        log.trace("Subscribing {}", subscription);
        if (isClientAlreadyRunning(subscription.getId())) {
            log.warn("{} already subscribed - skipping", subscription);
            throw new Notifications2SubscriptionAlreadyEstablishedException("Subscription is already active! " + subscription);
        }
        // create or reuse subscription
        createOrReuseExistingSubscription(subscription);

        log.trace("Creating new WebSocketClient");
        WebSocketClient client = clientFactoryFunction.apply(subscription, listener);
        clientMap.put(subscription.getId(), client);
        client.start();
    }

    @Override
    public void disconnect(Subscription.ID subscriptionId, boolean unsubscribe) {
        log.trace("Unsubscribing {}", subscriptionId);
        ensureNotifications2Enabled();
        WebSocketClient client = clientMap.get(subscriptionId);
        if (client != null) {
            try {
                log.trace("Stopping websocket client");
                client.stop(unsubscribe);
            } finally {
                clientMap.remove(subscriptionId);
                log.trace("Websocket client removed");
            }
        }
    }

    @Override
    public void delete(Subscription.ID subscriptionId) {
        log.trace("Deleting {}", subscriptionId);
        ensureNotifications2Enabled();
        disconnect(subscriptionId, true);
        final NotificationSubscriptionCollection notificationSubscriptionCollection = platform.getNotificationSubscriptionApi()
                .getSubscriptionsByFilter(new NotificationSubscriptionFilter().bySubscription(subscriptionId.getName()));
        List<NotificationSubscriptionRepresentation> subscriptions = notificationSubscriptionCollection.get().getSubscriptions();
        if (!CollectionUtils.isEmpty(subscriptions)) {
            subscriptions.forEach(s -> {
                log.trace("Deleting {}", s);
                platform.getNotificationSubscriptionApi().delete(s);
            });
        }
    }

    @Override
    public Optional<Object> getRawWebSocket(Subscription.ID subscriptionId) {
        ensureNotifications2Enabled();
        if (clientMap.containsKey(subscriptionId)) {
            return Optional.ofNullable(clientMap.get(subscriptionId).getRawWebSocket());
        }
        return Optional.empty();
    }


    private void createOrReuseExistingSubscription(Subscription subscription) {
        if (subscriptionExists(subscription.getId().getName())) {
            log.trace("Reusing existing subscription {}", subscription.getId().getName());
        } else {
            log.trace("Subscription {} does not exist. Creating...", subscription.getId().getName());
            createSubscription(subscription);
        }
    }

    private boolean isClientAlreadyRunning(Subscription.ID subscriptionId) {
        if (clientMap.containsKey(subscriptionId)) {
            if (clientMap.get(subscriptionId).isRunning()) {
                return true;
            } else {
                clientMap.remove(subscriptionId);
                return false;
            }
        }
        return false;
    }

    private void ensureNotifications2Enabled() {
        if (this.notifications2Properties.isDisabled()) {
            throw new Notifications2NotEnabledException("Notifications 2.0 disabled - to enable please set C8Y.notifications2.websocketUrl property");
        }
    }

    /**
     * Finds if subscription with given name exists in the platform
     *
     * @param subscriptionName to look up
     * @return true/false
     */
    boolean subscriptionExists(String subscriptionName) {
        final NotificationSubscriptionCollection notificationSubscriptionCollection = platform.getNotificationSubscriptionApi()
                .getSubscriptionsByFilter(new NotificationSubscriptionFilter().bySubscription(subscriptionName));
        List<NotificationSubscriptionRepresentation> subscriptions = notificationSubscriptionCollection.get().getSubscriptions();
        return subscriptions != null && subscriptions.size() > 0;
    }

    /**
     * Creates a {@link com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionRepresentation}
     * object in the platform making
     *
     * @param subscription request definition
     */
    void createSubscription(Subscription subscription) {
        log.debug("Creating new subscription representation {}", subscription);

        NotificationSubscriptionFilterRepresentation subscriptionFilter = new NotificationSubscriptionFilterRepresentation();
        subscriptionFilter.setApis(subscription.getTargetApis().stream().toList());
        if (StringUtils.isNotBlank(subscription.getTypeFilter())) {
            subscriptionFilter.setTypeFilter(subscription.getTypeFilter());
        }

        NotificationSubscriptionRepresentation subscriptionRepresentation = new NotificationSubscriptionRepresentation();
        if (subscription.isTenantSubscription()) {
            subscriptionRepresentation.setContext(CONTEXT_TENANT);
        }
        else {
            subscriptionRepresentation.setContext(CONTEXT_DEVICE);
            ManagedObjectRepresentation source = new ManagedObjectRepresentation();
            source.setId(GId.asGId(subscription.getDeviceId()));
            subscriptionRepresentation.setSource(source);
        }

        subscriptionRepresentation.setSubscription(subscription.getId().getName());
        subscriptionRepresentation.setSubscriptionFilter(subscriptionFilter);
        subscriptionRepresentation.setNonPersistent(!subscription.isPersistent());

        platform.getNotificationSubscriptionApi().subscribe(subscriptionRepresentation);
    }
}

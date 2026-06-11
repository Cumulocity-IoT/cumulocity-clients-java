package com.cumulocity.sdk.client.notification2.internal;

import com.cumulocity.rest.representation.reliable.notification.NotificationTokenRequestRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.messaging.notifications.Token;
import com.cumulocity.sdk.client.notification2.AckMode;
import com.cumulocity.sdk.client.notification2.Notification;
import com.cumulocity.sdk.client.notification2.NotificationListener;
import com.cumulocity.sdk.client.notification2.exception.AckFailedException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for establishing and maintaining a websocket connection + connection token management.
 * It also notifies about received messages (notifications).
 * An instance of this class is created per subscription.
 */
@Slf4j
public class WebSocketClient implements WebSocketConnectorListener {
    // constants
    private static final String URL_PATTERN = "%s/notification2/consumer/?token=%s&consumer=%s";
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private static final long TOKEN_EXPIRY_MINUTES = 24 * 60;
    private static final Duration DEFAULT_RECONNECT_DELAY = Duration.ofSeconds(5L);
    public static final String MESSAGE_SHUTDOWN = "Shutdown";

    // required constructor arguments
    private final String webSocketBaseUrl;
    private final String subscriber;
    private final String subscriptionName;
    @Setter
    private AckMode ackMode;
    private final String tenantId;
    private final String deviceId;
    private final NotificationListener notificationListener;
    @Setter // for unit tests
    private Duration reconnectDelay;
    private final boolean isTokenShared;
    private final boolean isTokenPersistent;
    private final Platform platform;
    private final ScheduledExecutorService scheduler;
    private final WebSocketConnector connector;

    // created on the fly
    private volatile Token token;
    private volatile Object rawSocket;

    // handle for scheduled reconnect task
    ScheduledFuture<?> connectTaskHandle = null;

    public WebSocketClient(String webSocketBaseUrl, String subscriber, String subscriptionName, AckMode ackMode,
                           String tenantId, String deviceId, NotificationListener notificationListener,
                           boolean isTokenShared, boolean isTokenPersistent, Platform platform, WebSocketConnector connector) {
        this.webSocketBaseUrl = webSocketBaseUrl;
        this.subscriber = subscriber;
        this.subscriptionName = subscriptionName;
        this.ackMode = ackMode;
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.notificationListener = notificationListener;
        this.reconnectDelay = DEFAULT_RECONNECT_DELAY;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.connector = connector;
        this.platform = platform;
        this.isTokenShared = isTokenShared;
        this.isTokenPersistent = isTokenPersistent;
    }

    /**
     * Connects to web socket
     */
    public void start() {
        log.debug("{} {} Starting client", subscriber, subscriptionName);
        connect();
    }

    /**
     * Disconnects and stops the client including scheduled jobs for reconnection
     */
    public void stop(boolean unsubscribe) {
        log.debug("{} {} Shutting down", subscriber, subscriptionName);

        try {
            scheduler.shutdownNow();
        }
        catch (Exception e) {
            log.warn("Scheduler shutdown failed", e);
        }
        try {
            boolean terminated = scheduler.awaitTermination(5, TimeUnit.SECONDS);
            log.trace("{} {} Scheduler shut down {}", subscriber, subscriptionName, terminated);
        } catch (InterruptedException e) {
            log.trace("{} {} Scheduler couldn't shut down in 5 seconds... forcing cleanup", subscriber, subscriptionName);
        }

        if (rawSocket != null) {
            try {
                connector.close(NORMAL_CLOSURE_STATUS, MESSAGE_SHUTDOWN);
                log.trace("{} {} Websocket closed", subscriber, subscriptionName);
            } catch (Exception e) {
                log.warn("{} {} Error closing websocket, details in TRACE logs", subscriber, subscriptionName);
                log.trace(e.getMessage(), e);
            } finally {
                rawSocket = null;
            }
        }

        if (isTokenPersistent && unsubscribe) {
            unsubscribeToken();
        }
        log.debug("{} {} Shutdown complete", subscriber, subscriptionName);
    }

    private void connect() {
        try {
            createToken();
            String url = String.format(URL_PATTERN, webSocketBaseUrl, token.getTokenString(), subscriber);
            log.trace("Connecting to: {}", url);

            connector.setUri(url);
            connector.connect(this);
        }
        catch (Exception e) {
            log.warn("Unable to connect - scheduling reconnection", e);
            reconnect();
        }
    }


    void sendAck(String ackHeader) throws AckFailedException {
        try {
            connector.send(ackHeader);
        } catch (Exception e) {
            log.warn("Exception when sending ACK message for subscriber {}", subscriber, e);
            throw new AckFailedException(e.getMessage(), e);
        }
    }

    synchronized boolean reconnect() {
        if (!scheduler.isTerminated() && !scheduler.isShutdown()) {
            if (connectTaskHandle != null) {
                connectTaskHandle.cancel(true);
            }
            log.trace("{} {} Scheduling reconnection in {} milliseconds", subscriber, subscriptionName, reconnectDelay.toMillis());
            connectTaskHandle = scheduler.schedule(this::connect, reconnectDelay.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } else {
            log.trace("{} {} Cancelling reconnection request due to client termination", subscriber, subscriptionName);
            return false;
        }
    }

    @Override
    public void onWebsocketClosed(int code, String reason) {
        log.debug("Connection closed, subscriber: {} subscriptionName: {}, reason: {}", subscriber, subscriptionName, reason);
        if (scheduler.isTerminated() || scheduler.isShutdown()) {
            log.debug("{} {} Client terminated and won't reconnect", subscriber, subscriptionName);
            return;
        }
        if (reason != null && reason.contains(MESSAGE_SHUTDOWN)) {
            log.debug("{} {} Client received shutdown message", subscriber, subscriptionName);
            return;
        }
        reconnect();
    }

    @Override
    public void onWebsocketError(Throwable t) {
        if (scheduler.isTerminated() || scheduler.isShutdown()) {
            log.debug("{} {} Client terminated and won't reconnect", subscriber, subscriptionName);
            return;
        }
        if (t.getMessage() != null && t.getMessage().contains(MESSAGE_SHUTDOWN)) {
            log.debug("{} {} Client received shutdown message", subscriber, subscriptionName);
            return;
        }
        log.warn("Connection failure, subscriber: {}, subscriptionName {}, error message: {}", subscriber, subscriptionName, t.getMessage(), t);
        reconnect();
    }

    @Override
    public void onWebsocketOpen() {
        rawSocket = connector.getRawSocket();
        log.debug("{} {} Successfully connected", subscriber, subscriptionName);
    }

    @Override
    public void onWebsocketMessage(String text) {
        String uuid = UUID.randomUUID().toString();
        log.debug("Received new message for subscriber {}. Message content available in TRACE logs", subscriber);
        log.trace(text + "\nAssigned UUID: " + uuid);

        Notification notification = Notification.parse(text);
        log.trace("{} Parsed Notification", uuid);

        String ackHeader = notification.getAckHeader();
        if (ackMode == AckMode.NONE) {
            log.trace("{} No ACK will be sent (AckMode.NONE used)", uuid);
        } else if (ackHeader == null) {
            log.trace("{} No ACK will be sent (ACK header is null)", uuid);
        } else if (ackMode == AckMode.IMMEDIATE) {
            log.trace("{} Sending IMMEDIATE ACK", uuid);
            try {
                sendAck(ackHeader);
            } catch (AckFailedException e) {
                log.warn("{} Failed to send ACK message, reconnecting, listener won't be triggered", uuid);
                reconnect();
                return;
            }
        }
        try {
            log.trace("{} Processing message in listener", uuid);
            notificationListener.onMessage(notification, subscriptionName, tenantId, deviceId);
            if (ackMode == AckMode.SYNCHRONOUS && notification.getAckHeader() != null) {
                log.trace("{} Sending POST_PROCESS ACK", uuid);
                try {
                    sendAck(ackHeader);
                }
                catch (AckFailedException e) {
                    log.warn("{} Failed to send ACK message, reconnecting, message might be reprocessed after reconnect", uuid, e);
                    reconnect();
                }
            }
        }
        catch (Exception e) {
            log.warn("{} notification listener threw an exception", uuid, e);
            if (ackMode == AckMode.SYNCHRONOUS) {
                log.warn("{} notification won't be sent for the message", uuid);
            }
        }
    }

    public Object getRawWebSocket() {
        return rawSocket;
    }

    public boolean isRunning() {
        return rawSocket != null;
    }

    /**
     * Creates a token used when connecting to websocket server. A new token is created on each connection attempt.
     */
    void createToken() {
        log.debug("Generating new token for {} and subscription {}", subscriber, subscriptionName);

        final NotificationTokenRequestRepresentation tokenRequestRepresentation =
                new NotificationTokenRequestRepresentation(subscriber, subscriptionName, null, true, TOKEN_EXPIRY_MINUTES, isTokenShared, !isTokenPersistent);
        token = platform.getTokenApi().create(tokenRequestRepresentation);
    }

    /**
     * This method is used to unsubscribe token after shutting down connection
     */
    void unsubscribeToken() {
        if (token == null) {
            return;
        }
        log.debug("Unsubscribing token (content in TRACE logs)");
        log.trace(token.toString());
        try {
            platform.getTokenApi().unsubscribe(token);
        }
        catch (Exception e) {
            log.warn("Couldn't unsubscribe token", e);
        }
        finally {
            token = null;
        }
    }
}

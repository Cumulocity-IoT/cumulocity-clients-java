package com.cumulocity.sdk.client.notification2.internal;

import com.cumulocity.rest.representation.reliable.notification.NotificationTokenRequestRepresentation;
import com.cumulocity.sdk.client.messaging.notifications.Token;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import com.cumulocity.sdk.client.notification2.AckMode;
import com.cumulocity.sdk.client.notification2.Notification;
import com.cumulocity.sdk.client.notification2.NotificationListener;
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
    public static final String MESSAGE_TOKEN_REFRESH = "Token refresh";
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
    @Setter
    private Duration reconnectDelay;
    @Setter
    private Duration tokenRefreshInterval;
    private final boolean isTokenShared;
    private final boolean isTokenPersistent;
    private final TokenApi tokenApi;
    private final ScheduledExecutorService scheduler;
    private final WebSocketConnector connector;

    // created on the fly
    private Token token;
    private Object rawSocket;
    private boolean connected = false;

    // handles for scheduled tasks
    ScheduledFuture<?> connectTaskHandle = null;
    ScheduledFuture<?> tokenRefreshTaskHandle = null;

    public WebSocketClient(String webSocketBaseUrl, String subscriber, String subscriptionName, AckMode ackMode,
                           String tenantId, String deviceId, NotificationListener notificationListener, Duration reconnectDelay, Duration tokenRefreshInterval,
                           boolean isTokenShared, boolean isTokenPersistent, TokenApi tokenApi, WebSocketConnector connector) {
        this.webSocketBaseUrl = webSocketBaseUrl;
        this.subscriber = subscriber;
        this.subscriptionName = subscriptionName;
        this.ackMode = ackMode;
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.notificationListener = notificationListener;
        this.reconnectDelay = reconnectDelay;
        this.tokenRefreshInterval = tokenRefreshInterval;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.connector = connector;
        this.tokenApi = tokenApi;
        this.isTokenShared = isTokenShared;
        this.isTokenPersistent = isTokenPersistent;
    }

    /**
     * Connects to web socket and schedules token refresh job
     * package private
     */
    public void start() {
        log.debug("{} {} Starting client", subscriber, subscriptionName);
        connect();
    }

    /**
     * Disconnects and stops the client including scheduled jobs for reconnection and token refresh
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
        if (tokenRefreshTaskHandle != null) {
            tokenRefreshTaskHandle.cancel(true);
        }
        if (token == null) {
            createToken();
        } else {
            refreshToken();
        }
        scheduleTokenRefresh();
        String url = String.format(URL_PATTERN, webSocketBaseUrl, token.getTokenString(), subscriber);
        log.trace("Connecting to: {}", url);

        connector.setUri(url);
        connector.connect(this);
    }


    void sendAck(String ackHeader) {
        try {
            connector.send(ackHeader);
        } catch (Exception e) {
            log.warn("{} Exception when sending ACK message for subscriber, details in TRACE logs, {}", subscriber, e.getMessage());
            log.trace(e.getMessage(), e);
        }
    }

    boolean reconnect() {
        if (!scheduler.isTerminated()) {
            if (connectTaskHandle != null) {
                connectTaskHandle.cancel(true);
            }
            if (tokenRefreshTaskHandle != null) {
                tokenRefreshTaskHandle.cancel(true);
            }
            log.trace("{} {} Scheduling reconnection in {} milliseconds", subscriber, subscriptionName, reconnectDelay.toMillis());
            connectTaskHandle = scheduler.schedule(this::connect, reconnectDelay.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } else {
            log.trace("{} {} Cancelling reconnection request due to client termination", subscriber, subscriptionName);
            return false;
        }
    }

    private void scheduleTokenRefresh() {
        log.trace("{} {} Scheduling next token refresh in {} milliseconds", subscriber, subscriptionName, tokenRefreshInterval.toMillis());
        tokenRefreshTaskHandle = scheduler.schedule(() -> {
            if (connected) {
                log.trace("{} {} Refreshing token and reconnecting", subscriber, subscriptionName);
                if (rawSocket != null) {
                    log.trace("{} {} Closing websocket", subscriber, subscriptionName);
                    connector.close(NORMAL_CLOSURE_STATUS, MESSAGE_TOKEN_REFRESH);
                }
                log.trace("{} {} Reconnecting", subscriber, subscriptionName);
                connect();
            }
        }, tokenRefreshInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void onWebsocketClosed(int code, String reason) {
        connected = false;
        log.debug("Connection closed, subscriber: {} subscriptionName: {}, reason: {}", subscriber, subscriptionName, reason);
        if (scheduler.isTerminated() || scheduler.isShutdown()) {
            log.debug("{} {} Client terminated and won't reconnect", subscriber, subscriptionName);
            return;
        }
        if (reason.contains(MESSAGE_SHUTDOWN) || reason.contains(MESSAGE_TOKEN_REFRESH)) {
            log.debug("{} {} Client received a shutdown or token refresh message", subscriber, subscriptionName);
            return;
        }
        reconnect();
    }

    @Override
    public void onWebsocketError(Throwable t) {
        connected = false;
        log.debug("Connection failure, subscriber: {}, subscriptionName {}, error message: {}", subscriber, subscriptionName, t.getMessage(), t);
        if (scheduler.isTerminated() || scheduler.isShutdown()) {
            log.debug("{} {} Client terminated and won't reconnect", subscriber, subscriptionName);
            return;
        }
        if (t.getMessage() != null && (t.getMessage().contains(MESSAGE_SHUTDOWN) || t.getMessage().contains(MESSAGE_TOKEN_REFRESH))) {
            log.debug("{} {} Client received a shutdown or token refresh message", subscriber, subscriptionName);
            return;
        }
        reconnect();
    }

    @Override
    public void onWebsocketOpen() {
        rawSocket = connector.getRawSocket();
        connected = true;
        log.debug("{} {} Successfully connected", subscriber, subscriptionName);
    }

    @Override
    public void onWebsocketMessage(String text) {
        String uuid = UUID.randomUUID().toString();
        log.debug("Received new message for subscriber {}. Message content available in TRACE logs", subscriber);
        log.trace(text + "\nAssigned UUID: " + uuid);

        Notification notification = Notification.parse(text);
        log.trace(uuid + " Parsed Notification");

        String ackHeader = notification.getAckHeader();
        if (ackMode == AckMode.NONE) {
            log.trace(uuid + " No ACK will be sent (AckMode.NONE used)");
        } else if (ackHeader == null) {
            log.trace(uuid + " No ACK will be sent (ACK header is null)");
        } else if (ackMode == AckMode.IMMEDIATE) {
            log.trace(uuid + " Sending IMMEDIATE ACK");
            sendAck(ackHeader);
        }
        try {
            log.trace(uuid + " Processing message in listener");
            notificationListener.onMessage(notification, subscriptionName, tenantId, deviceId);
            if (ackMode == AckMode.SYNCHRONOUS && notification.getAckHeader() != null) {
                log.trace(uuid + " Sending POST_PROCESS ACK");
                sendAck(ackHeader);
            }
        } catch (Exception e) {
            log.warn(uuid + " notification listener threw an exception", e);
            if (ackMode == AckMode.SYNCHRONOUS) {
                log.warn(uuid + " notification won't be sent for the message");
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
     * Obtains a token that can be used when connecting to websocket server
     */
    void createToken() {
        long expirationMinutes = tokenRefreshInterval.plusMinutes(1).toMinutes();
        log.debug("Generating new token for {} and subscription {}, expiration after {} minutes", subscriber, subscriptionName, expirationMinutes);

        final NotificationTokenRequestRepresentation tokenRequestRepresentation =
                new NotificationTokenRequestRepresentation(subscriber, subscriptionName, null, true, expirationMinutes, isTokenShared, !isTokenPersistent);
        token = tokenApi.create(tokenRequestRepresentation);
    }

    /**
     * This method is used to refresh the token before it's expired
     */
    void refreshToken() {
        log.debug("Refreshing token (content in TRACE logs)");
        log.trace(token.toString());
        try {
            token = tokenApi.refresh(token);
        }
        catch (Exception e) {
            log.warn("Couldn't refresh token - creating new instead", e);
            createToken();
        }
    }

    /**
     * This method is used to unsubscribe token after shutting down connection
     */
    void unsubscribeToken() {
        log.debug("Unsubscribing token (content in TRACE logs)");
        log.trace(token.toString());
        try {
            tokenApi.unsubscribe(token);
        }
        catch (Exception e) {
            log.warn("Couldn't unsubscribe token", e);
        }
        finally {
            token = null;
        }
    }
}

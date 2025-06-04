package com.cumulocity.sdk.client.notification2.internal;

import com.cumulocity.rest.representation.reliable.notification.NotificationTokenRequestRepresentation;
import com.cumulocity.sdk.client.messaging.notifications.Token;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import com.cumulocity.sdk.client.notification2.AckMode;
import com.cumulocity.sdk.client.notification2.Notification;
import com.cumulocity.sdk.client.notification2.NotificationListener;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static com.cumulocity.sdk.client.notification2.internal.WebSocketClient.MESSAGE_SHUTDOWN;
import static com.cumulocity.sdk.client.notification2.internal.WebSocketClient.MESSAGE_TOKEN_REFRESH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class WebSocketClientTest {
    private static final String WS_URL = "ws://localhost:7711";
    private static final String MESSAGE = """
            thisIsACK
            ThisIsHeader1
            ThisIsHeader2
            {"message": "this is payload"}
                                """;
    private static final String PAYLOAD_ONLY_MESSAGE = "{\"message\": \"this is payload\"}";
    public static final String SUBSCRIBER = "subscriber1";
    public static final String SUBSCRIPTION_NAME = "mySub";
    public static final AckMode ACK_MODE = AckMode.IMMEDIATE;
    public static final String TENANT_ID = "tenant1";
    public static final String DEVICE_ID = "dev1";
    public static final boolean IS_TOKEN_SHARED = false;
    private static final boolean IS_TOKEN_PERSISTENT = false;
    private final Token token = mock(Token.class);
    private final NotificationListener notificationListener = mock(NotificationListener.class);
    private final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    private final ArgumentCaptor<NotificationTokenRequestRepresentation> tokenReqestCaptor = ArgumentCaptor.forClass(NotificationTokenRequestRepresentation.class);
    private final TokenApi tokenApi = mock(TokenApi.class);
    private final WebSocketConnector connector = mock(WebSocketConnector.class);

    private WebSocketClient client;

    private Long lastAckTimestamp;
    private Long lastListenerNotification;

    @BeforeEach
    public void setup() {
        when(tokenApi.create(any())).thenReturn(token);
        when(tokenApi.refresh(any())).thenReturn(token);
        when(token.getTokenString()).thenReturn("abcd");
        when(connector.getRawSocket()).thenReturn(new Object());
        initClient(Duration.ofMinutes(10));
        lastAckTimestamp = null;
        lastListenerNotification = null;
        doAnswer(invocationOnMock -> {
            lastAckTimestamp = System.currentTimeMillis();
            log.info("connector sleep start");
            sleep(100);
            log.info("connector sleep end");
            return null;
        }).when(connector).send(any());
        doAnswer(invocationOnMock -> {
            lastListenerNotification = System.currentTimeMillis();
            log.info("listener sleep start");
            sleep(100);
            log.info("listener sleep end");
            return null;
        }).when(notificationListener).onMessage(any(), any(), any(), any());
    }

    private void initClient(Duration tokenRefreshInterval) {
        client = new WebSocketClient(WS_URL, SUBSCRIBER, SUBSCRIPTION_NAME, ACK_MODE,
                TENANT_ID, DEVICE_ID, notificationListener, Duration.ofSeconds(5L), tokenRefreshInterval,
                IS_TOKEN_SHARED, IS_TOKEN_PERSISTENT, tokenApi, connector);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Test
    public void shouldConnect() {
        client.start();
        client.onWebsocketOpen();
        verify(connector).connect(eq(client));
        verify(tokenApi).create(tokenReqestCaptor.capture());
        assertNotNull(client.getRawWebSocket());
        assertTrue(client.isRunning());
        NotificationTokenRequestRepresentation tokenRequest = tokenReqestCaptor.getValue();
        assertEquals(SUBSCRIBER, tokenRequest.getSubscriber());
        assertEquals(SUBSCRIPTION_NAME, tokenRequest.getSubscription());
        assertEquals(IS_TOKEN_SHARED, tokenRequest.isShared());
        assertEquals(!IS_TOKEN_PERSISTENT, tokenRequest.isNonPersistent());
        assertTrue(tokenRequest.isSigned());
    }

    @Test
    public void shouldRefreshToken() {
        client.setTokenRefreshInterval(Duration.ofSeconds(2));
        client.start();
        client.onWebsocketOpen();
        sleep(3000);
        client.stop(false);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(1)).refresh(eq(token));
        verify(connector, times(2)).connect(eq(client));
        assertFalse(client.isRunning());
    }

    @Test
    public void shouldReconnectOnWebSocketClose() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.onWebsocketClosed(101, "random");
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(1)).refresh(eq(token));
        verify(connector, times(2)).connect(eq(client));
        assertTrue(client.isRunning());
    }

    @Test
    public void shouldNotReconnectOnWebsocketClosedAfterStop() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.stop(false);
        client.onWebsocketClosed(101, "random");
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(0)).refresh(any());
        verify(connector, times(1)).connect(eq(client));
        assertFalse(client.isRunning());
    }

    @Test
    public void shouldNotReconnectOnWebsocketClosedWhenMessageIsTokenRefresh() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.onWebsocketClosed(101, MESSAGE_TOKEN_REFRESH);
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(0)).refresh(any());
        verify(connector, times(1)).connect(eq(client));
    }

    @Test
    public void shouldNotReconnectOnWebsocketClosedWhenMessageIsShutdown() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.onWebsocketClosed(101, MESSAGE_SHUTDOWN);
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(0)).refresh(any());
        verify(connector, times(1)).connect(eq(client));
    }

    @Test
    public void shouldReconnectOnError() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.onWebsocketError(new RuntimeException("some error"));
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(1)).refresh(eq(token));
        verify(connector, times(2)).connect(eq(client));
        assertTrue(client.isRunning());
    }

    @Test
    public void shouldNotReconnectOnWebsocketErrorAfterStop() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.stop(false);
        client.onWebsocketError(new RuntimeException("some error"));
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(0)).refresh(any());
        verify(connector, times(1)).connect(eq(client));
        assertFalse(client.isRunning());
    }

    @Test
    public void shouldNotReconnectOnWebsocketErrorWhenMessageIsTokenRefresh() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.onWebsocketError(new RuntimeException(MESSAGE_TOKEN_REFRESH));
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(0)).refresh(any());
        verify(connector, times(1)).connect(eq(client));
    }

    @Test
    public void shouldNotReconnectOnWebsocketErrorWhenMessageIsShutdown() {
        client.setReconnectDelay(Duration.ofMillis(100));
        client.start();
        client.onWebsocketOpen();
        client.onWebsocketError(new RuntimeException(MESSAGE_SHUTDOWN));
        sleep(200);

        verify(tokenApi, times(1)).create(any());
        verify(tokenApi, times(0)).refresh(any());
        verify(connector, times(1)).connect(eq(client));
    }

    @Test
    public void shouldSendAckImmediately() {
        client.onWebsocketMessage(MESSAGE);
        verify(connector).send(eq("thisIsACK"));
        verify(notificationListener).onMessage(notificationCaptor.capture(), any(), any(), any());
        Notification notification = notificationCaptor.getValue();
        assertEquals("thisIsACK", notification.getAckHeader());
        assertEquals(2, notification.getHeaders().size());
        assertNotNull(notification.getPayload());
        assertTrue(lastAckTimestamp < lastListenerNotification);
    }

    @Test
    public void shouldSendAckAfterProcessingMessage() {
        client.setAckMode(AckMode.SYNCHRONOUS);
        client.onWebsocketMessage(MESSAGE);
        verify(connector).send(eq("thisIsACK"));
        verify(notificationListener).onMessage(notificationCaptor.capture(), any(), any(), any());
        Notification notification = notificationCaptor.getValue();
        assertEquals("thisIsACK", notification.getAckHeader());
        assertEquals(2, notification.getHeaders().size());
        assertNotNull(notification.getPayload());
        assertTrue(lastAckTimestamp > lastListenerNotification);
    }

    @Test
    public void shouldNotSendAck() {
        client.setAckMode(AckMode.NONE);
        client.onWebsocketMessage(MESSAGE);
        verify(connector, times(0)).send(any());
        verify(notificationListener).onMessage(notificationCaptor.capture(), any(), any(), any());
        Notification notification = notificationCaptor.getValue();
        assertEquals("thisIsACK", notification.getAckHeader());
        assertEquals(2, notification.getHeaders().size());
        assertNotNull(notification.getPayload());
        assertNull(lastAckTimestamp);
        assertNotNull(lastListenerNotification);
    }

    @Test
    public void shouldNotSendAckWhenTheresNoAckHeader() {
        client.setAckMode(AckMode.IMMEDIATE);
        client.onWebsocketMessage(PAYLOAD_ONLY_MESSAGE);
        verify(connector, times(0)).send(any());
        verify(notificationListener).onMessage(notificationCaptor.capture(), eq(SUBSCRIPTION_NAME), eq(TENANT_ID), eq(DEVICE_ID));
        Notification notification = notificationCaptor.getValue();
        assertNull(notification.getAckHeader());
        assertTrue(notification.getHeaders().isEmpty());
        assertNotNull(notification.getPayload());
        assertNull(lastAckTimestamp);
        assertNotNull(lastListenerNotification);
    }

    @Test
    public void shouldSendAckAfterProcessingMessageWhenListenerThrowsException() {
        client.setAckMode(AckMode.SYNCHRONOUS);
        doThrow(new RuntimeException("Oh no!")).when(notificationListener).onMessage(any(), any(), any(), any());

        client.onWebsocketMessage(MESSAGE);
        verify(connector, times(0)).send(any());
        verify(notificationListener).onMessage(notificationCaptor.capture(), any(), any(), any());
        Notification notification = notificationCaptor.getValue();
        assertEquals("thisIsACK", notification.getAckHeader());
        assertEquals(2, notification.getHeaders().size());
        assertNotNull(notification.getPayload());
        assertNull(lastAckTimestamp);
    }

    @Test
    public void shouldProcessMessageEvenIfSendAckFailed() {
        client.setAckMode(AckMode.SYNCHRONOUS);
        doThrow(new RuntimeException("Oh no!")).when(connector).send(any());

        client.onWebsocketMessage(MESSAGE);

        verify(connector, times(1)).send(any());
        verify(notificationListener).onMessage(notificationCaptor.capture(), eq(SUBSCRIPTION_NAME), eq(TENANT_ID), eq(DEVICE_ID));
        Notification notification = notificationCaptor.getValue();
        assertNotNull(notification.getPayload());
        assertNull(lastAckTimestamp);
        assertNotNull(lastListenerNotification);
    }

    @Test
    public void shouldCancelReconnectionWhenSchedulerIsCancelled() {
        client.start();
        client.stop(false);
        assertFalse(client.reconnect());
    }
}

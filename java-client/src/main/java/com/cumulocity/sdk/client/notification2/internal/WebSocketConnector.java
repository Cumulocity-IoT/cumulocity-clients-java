package com.cumulocity.sdk.client.notification2.internal;

/**
 * Abstraction for the object that will make actual websocket connection and report connection state changes and received messages
 * <br/>
 * By default, we use TooTallNate websocket client implementation {@link TooTallNateWebSocketConnector}
 */
public interface WebSocketConnector {
    /**
     * @param uri to connect to
     */
    void setUri(String uri);

    /**
     * starts websocket connection
     *
     * @param listener object that will receive messages and connection state changes
     */
    void connect(WebSocketConnectorListener listener);

    /**
     * closes connection
     *
     * @param status  status/reason according to websocket specification
     * @param message close message
     */
    void close(int status, String message);

    /**
     * sends text message through websocket
     *
     * @param message text message to send
     */
    void send(String message);

    /**
     * @return raw websocket object for low-level access. Actual class depends on implementation used in the project.
     */
    Object getRawSocket();
}

package com.cumulocity.sdk.client.notification2.internal;

import com.cumulocity.sdk.client.notification2.Notifications2Api;

/**
 * Used to transfer websocket events/messages/state changes from WS library to
 * {@link Notifications2Api} implementation. Classes implementing
 * {@link WebSocketConnector} must call these methods when applicable.
 */
public interface WebSocketConnectorListener {
    /**
     * connection established
     */
    void onWebsocketOpen();

    /**
     * connection error
     *
     * @param t exception details
     */
    void onWebsocketError(Throwable t);

    /**
     * message received
     *
     * @param message message text
     */
    void onWebsocketMessage(String message);

    /**
     * connection closed
     *
     * @param code   closing code
     * @param reason closing reason
     */
    void onWebsocketClosed(int code, String reason);
}

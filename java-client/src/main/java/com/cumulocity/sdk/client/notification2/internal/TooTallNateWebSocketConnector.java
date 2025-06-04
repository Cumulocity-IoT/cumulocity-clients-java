package com.cumulocity.sdk.client.notification2.internal;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TooTallNateWebSocketConnector implements WebSocketConnector {
    private String uri;

    private WebSocketClient client;

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public void connect(WebSocketConnectorListener listener) {
        client = new WebSocketClient(URI.create(uri), new Draft_6455(), null, 10000) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                listener.onWebsocketOpen();
            }

            @Override
            public void onMessage(String s) {
                listener.onWebsocketMessage(s);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                listener.onWebsocketClosed(i, s);
            }

            @Override
            public void onError(Exception e) {
                listener.onWebsocketError(e);
            }
        };
        client.connect();
    }

    @Override
    public void close(int status, String message) {
        client.close(status, message);
    }

    @Override
    public void send(String message) {
        client.send(message);
    }

    @Override
    public Object getRawSocket() {
        return client.getConnection();
    }
}

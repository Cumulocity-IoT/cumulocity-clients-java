package com.cumulocity.mqtt.service.sdk.websocket;

import com.cumulocity.mqtt.service.sdk.listener.ConnectionListener;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Deprecated(forRemoval = true)
abstract class AbstractWebSocketClient extends WebSocketClient {

    private final String sourceId;
    private final ConnectionListener connectionListener;

    private String connectionError;

    AbstractWebSocketClient(final URI serverUri, final String sourceId, final ConnectionListener connectionListener) {
        super(serverUri);
        this.sourceId = sourceId;
        this.connectionListener = connectionListener;
    }

    @Override
    public boolean connectBlocking(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        if (!super.connectBlocking(timeout, timeUnit)) {
            throw new RuntimeException(isNotBlank(connectionError) ? connectionError : "Cannot connect: unknown error");
        }
        return true;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.debug("Web socket connection open for '{}' with status '{}' and message '{}'", uri, handshake.getHttpStatus(), handshake.getHttpStatusMessage());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (!super.isOpen()) {
            connectionError = String.format("Cannot connect: %s %s", reason, code);
        }
        connectionListener.onDisconnected(String.format("%s%d by the %s", reason, code, remote ? "client" : "server"), sourceId);
    }

    @Override
    public void onError(Exception e) {
        if (!super.isOpen()) {
            connectionError = String.format("Cannot connect: %s", e.getMessage());
        }
        connectionListener.onError(e, sourceId);
    }

}

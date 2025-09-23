package com.cumulocity.mqtt.service.sdk.listener;

import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link ConnectionListener} that logs errors and disconnections.
 */
@Slf4j
@Deprecated(forRemoval = true)
public class LoggingConnectionListener implements ConnectionListener {
    @Override
    public void onError(Throwable error, String sourceId) {
        log.error("Connection {} error", sourceId, error);
    }

    @Override
    public void onDisconnected(String reason, String sourceId) {
        log.info("Connection {} disconnected because of {}", sourceId, reason);
    }
}

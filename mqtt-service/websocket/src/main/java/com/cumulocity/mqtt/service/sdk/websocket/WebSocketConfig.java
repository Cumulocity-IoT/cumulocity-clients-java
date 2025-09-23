package com.cumulocity.mqtt.service.sdk.websocket;

import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import lombok.Data;

@Data
@Deprecated(forRemoval = true)
class WebSocketConfig {

    private static final long DEFAULT_CONNECTION_TIMEOUT_MILLIS = 30000;

    private String baseUrl;
    private TokenApi tokenApi;
    private long connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MILLIS;

}

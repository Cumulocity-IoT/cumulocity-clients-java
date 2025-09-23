package com.cumulocity.mqtt.service.sdk.websocket;

import lombok.RequiredArgsConstructor;

import java.util.Base64;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
@Deprecated(forRemoval = true)
class WebSocketMessage {

    private final String ackHeader;
    private final byte[] avroPayload;

    static WebSocketMessage parse(String message) {
        String header = "";
        int i = message.indexOf('\n');
        if (i != -1) {
            header = message.substring(0, i);
            message = message.substring(i + 1);
        }

        final byte[] avroPayload = Base64.getDecoder().decode(message);

        if (header.isEmpty()) {
            return new WebSocketMessage(null, avroPayload);
        }

        return new WebSocketMessage(header, avroPayload);
    }

    Optional<String> getAckHeader() {
        return Optional.ofNullable(ackHeader);
    }

    byte[] getAvroPayload() {
        return avroPayload;
    }

}

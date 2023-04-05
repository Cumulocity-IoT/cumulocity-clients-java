package com.cumulocity.generic.mqtt.client.websocket;

import com.cumulocity.generic.mqtt.client.GenericMqttMessageListener;
import com.cumulocity.generic.mqtt.client.converter.GenericMqttMessageConverter;
import com.cumulocity.generic.mqtt.client.model.GenericMqttMessage;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Optional;

@Slf4j
class GenericMqttWebSocketClient extends WebSocketClient {

    private final GenericMqttMessageConverter genericMqttMessageConverter = new GenericMqttMessageConverter();
    private GenericMqttMessageListener listener;

    public GenericMqttWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    public GenericMqttWebSocketClient(URI serverUri, GenericMqttMessageListener listener) {
        super(serverUri);
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.debug("Web socket connection open for '{}' with status '{}' and message '{}'", uri, handshake.getHttpStatus(), handshake.getHttpStatusMessage());
    }

    @Override
    public void onMessage(String message) {
        if (listener == null) {
            return;
        }

        final WebSocketMessage webSocketMessage = WebSocketMessage.parse(message);

        final Optional<String> ackHeader = webSocketMessage.getAckHeader();
        final byte[] avroPayload = webSocketMessage.getAvroPayload();

        final GenericMqttMessage genericMqttMessage = genericMqttMessageConverter.decode(avroPayload);

        listener.onMessage(genericMqttMessage);

        ackHeader.ifPresent(this::send);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.debug("Web socket connection closed for '{}' with core '{}' reason '{}' by {}", uri, code, reason, remote ? "client" : "server");
    }

    @Override
    public void onError(Exception e) {
        if (listener == null) {
            return;
        }

        listener.onError(e);
    }
}

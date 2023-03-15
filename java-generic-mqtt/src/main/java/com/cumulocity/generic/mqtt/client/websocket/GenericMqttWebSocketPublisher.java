package com.cumulocity.generic.mqtt.client.websocket;

import com.cumulocity.generic.mqtt.client.GenericMqttConnectionConfig;
import com.cumulocity.generic.mqtt.client.GenericMqttPublisher;
import com.cumulocity.generic.mqtt.client.model.GenericMqttMessage;
import com.cumulocity.generic.mqtt.client.websocket.exception.GenericMqttWebSocketClientException;
import com.cumulocity.rest.representation.reliable.notification.NotificationTokenRequestRepresentation;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import org.apache.pulsar.client.api.Schema;

import java.net.URI;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

class GenericMqttWebSocketPublisher implements GenericMqttPublisher {

    private final static String WEBSOCKET_URL_PATTERN = "%s/notification2/producer/?token=%s";
    private static final String TOKEN_TYPE = "mqtt";

    private final String webSocketBaseUrl;
    private final TokenApi tokenApi;

    private final GenericMqttConnectionConfig config;

    private GenericMqttWebSocketClient producer;

    public GenericMqttWebSocketPublisher(String webSocketBaseUrl, TokenApi tokenApi, GenericMqttConnectionConfig config) {
        this.webSocketBaseUrl = webSocketBaseUrl;
        this.tokenApi = tokenApi;
        this.config = config;
    }

    @Override
    public void publish(GenericMqttMessage genericMqttMessage) {
        final NotificationTokenRequestRepresentation tokenRequestRepresentation = getTokenRepresentation(config.getTopic());
        final String token = tokenApi
                .create(tokenRequestRepresentation)
                .getTokenString();

        if (token == null) {
            throw new GenericMqttWebSocketClientException(String.format("Token could not be created for topic %s", config.getTopic()));
        }

        if (producer == null) {
            try {
                final URI uri = new URI(String.format(WEBSOCKET_URL_PATTERN, webSocketBaseUrl, token));
                producer = new GenericMqttWebSocketClient(uri);
                producer.connectBlocking();
            } catch (Exception e) {
                throw new GenericMqttWebSocketClientException("Error publishing message!", e);
            }
        }

        final Schema<GenericMqttMessage> avro = Schema.AVRO(GenericMqttMessage.class);
        final byte[] data = avro.encode(genericMqttMessage);

        producer.send(data);
    }

    @Override
    public void close() throws Exception {
        if (producer != null) {
            producer.close();
        }
    }

    private NotificationTokenRequestRepresentation getTokenRepresentation(String topic) {
        final NotificationTokenRequestRepresentation representation = new NotificationTokenRequestRepresentation();
        representation.setSubscriber("javaGenericMqttWSClientPub" + randomAlphabetic(10));
        representation.setSubscription(topic);
        representation.setType(TOKEN_TYPE);
        representation.setSigned(true);
        representation.setExpiresInMinutes(1440);
        representation.setShared(false);
        representation.setNonPersistent(false);
        return representation;
    }
}

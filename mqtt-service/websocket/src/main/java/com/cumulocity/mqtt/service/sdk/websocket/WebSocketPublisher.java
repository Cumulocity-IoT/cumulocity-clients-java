package com.cumulocity.mqtt.service.sdk.websocket;

import com.cumulocity.mqtt.service.sdk.MqttServiceException;
import com.cumulocity.mqtt.service.sdk.model.MqttServiceMessage;
import com.cumulocity.mqtt.service.sdk.model.util.AvroMessageConverter;
import com.cumulocity.mqtt.service.sdk.model.util.MessageConverter;
import com.cumulocity.mqtt.service.sdk.publisher.Publisher;
import com.cumulocity.mqtt.service.sdk.publisher.PublisherConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Deprecated(forRemoval = true)
class WebSocketPublisher implements Publisher {

    private final static String SUBSCRIBER = "mqttServicePublisher";
    private final static String WEBSOCKET_URL_PATTERN = "%s/notification2/producer/?token=%s";
    private final static MessageConverter messageConverter = new AvroMessageConverter();

    private final WebSocketConfig config;
    private final PublisherConfig publisherConfig;

    private final AtomicInteger sequence = new AtomicInteger();
    private final TokenSupplier tokenSupplier;

    private WebSocketProducer producer;

    WebSocketPublisher(WebSocketConfig config, PublisherConfig publisherConfig) {
        this.config = config;
        this.publisherConfig = publisherConfig;
        this.tokenSupplier = new TokenSupplier(config.getTokenApi(), publisherConfig.getTopic(), SUBSCRIBER);
    }

    void connect() throws MqttServiceException {
        final String token = tokenSupplier.getToken().getTokenString();
        if (token == null) {
            throw new MqttServiceException(String.format("Token could not be created for topic %s", publisherConfig.getTopic()));
        }
        try {
            final URI uri = new URI(String.format(WEBSOCKET_URL_PATTERN, config.getBaseUrl(), token));
            producer = new WebSocketProducer(uri, publisherConfig);
            producer.connectBlocking(config.getConnectionTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new MqttServiceException("Error connecting to " + config.getBaseUrl(), e);
        }
    }

    @Override
    public void reconnect() throws MqttServiceException {
        if (isConnected()) {
            close();
        }
        connect();
    }

    @Override
    public void publish(MqttServiceMessage message) {
        final byte[] data = messageConverter.encode(message);
        final String publishMessage = sequence.incrementAndGet() + "\n" + Base64.encodeBase64String(data);

        producer.send(publishMessage);
    }

    @Override
    public boolean isConnected() {
        return producer != null && producer.isOpen();
    }

    @Override
    public void close() {
        if (isConnected()) {
            producer.close();
        }
        producer = null;
    }

    private static final class WebSocketProducer extends AbstractWebSocketClient {

        public WebSocketProducer(URI serverUri, PublisherConfig publisherConfig) {
            super(serverUri, publisherConfig.getId(), publisherConfig.getConnectionListener());
        }

        @Override
        public void onMessage(String message) {
            log.debug("Received ack for publish message {}", message);
        }

    }
}

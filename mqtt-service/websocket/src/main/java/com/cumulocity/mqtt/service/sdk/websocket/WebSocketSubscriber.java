package com.cumulocity.mqtt.service.sdk.websocket;

import com.cumulocity.mqtt.service.sdk.MqttServiceException;
import com.cumulocity.mqtt.service.sdk.listener.MessageListener;
import com.cumulocity.mqtt.service.sdk.model.util.AvroMessageConverter;
import com.cumulocity.mqtt.service.sdk.model.util.MessageConverter;
import com.cumulocity.mqtt.service.sdk.subscriber.Subscriber;
import com.cumulocity.mqtt.service.sdk.subscriber.SubscriberConfig;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Deprecated(forRemoval = true)
class WebSocketSubscriber implements Subscriber {

    private final static String WEBSOCKET_URL_PATTERN = "%s/notification2/consumer/?token=%s";
    private final static MessageConverter messageConverter = new AvroMessageConverter();

    private final WebSocketConfig config;
    private final SubscriberConfig subscriberConfig;
    private final TokenSupplier tokenSupplier;

    private WebSocketConsumer consumer;

    WebSocketSubscriber(WebSocketConfig config, SubscriberConfig subscriberConfig) {
        this.config = config;
        this.subscriberConfig = subscriberConfig;
        this.tokenSupplier = new TokenSupplier(config.getTokenApi(), subscriberConfig.getTopic(), subscriberConfig.getSubscriber());
    }

    @Override
    public void subscribe(MessageListener listener) {
        if (consumer != null) {
            return;
        }

        final String token = tokenSupplier.getToken().getTokenString();
        if (token == null) {
            throw new MqttServiceException(String.format("Token could not be created for topic %s", subscriberConfig.getTopic()));
        }
        try {
            final URI uri = new URI(String.format(WEBSOCKET_URL_PATTERN, config.getBaseUrl(), token));
            consumer = new WebSocketConsumer(uri, subscriberConfig, listener);
            consumer.connectBlocking(config.getConnectionTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new MqttServiceException("WebSocket connection could not be established!", e);
        }
    }

    @Override
    public void resubscribe() throws MqttServiceException {
        final MessageListener listener = consumer.messageListener;
        close();
        subscribe(listener);
    }

    @Override
    public void unsubscribe() {
        checkNotNull(consumer, "Subscriber is not connected");
        consumer.send("unsubscribe_subscriber");
    }

    @Override
    public boolean isConnected() {
        return consumer != null && consumer.isOpen();
    }

    @Override
    public void close() {
        if (isConnected()) {
            consumer.close();
        }
        consumer = null;
    }

    private static final class WebSocketConsumer extends AbstractWebSocketClient {

        private final MessageListener messageListener;

        WebSocketConsumer(URI serverUri, SubscriberConfig subscriberConfig, MessageListener messageListener) {
            super(serverUri, subscriberConfig.getId(), subscriberConfig.getConnectionListener());
            this.messageListener = messageListener;
        }

        @Override
        public void onMessage(String message) {
            final WebSocketMessage webSocketMessage = WebSocketMessage.parse(message);

            final Optional<String> ackHeader = webSocketMessage.getAckHeader();
            final byte[] avroPayload = webSocketMessage.getAvroPayload();

            messageListener.onMessage(messageConverter.decode(avroPayload));

            ackHeader.ifPresent(this::send);
        }

    }

}

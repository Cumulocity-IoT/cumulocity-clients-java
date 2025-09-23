package com.cumulocity.mqtt.service.sdk.websocket;

import com.cumulocity.mqtt.service.sdk.MqttServiceApi;
import com.cumulocity.mqtt.service.sdk.MqttServiceApiImpl;
import com.cumulocity.mqtt.service.sdk.MqttServiceException;
import com.cumulocity.mqtt.service.sdk.listener.MessageListener;
import com.cumulocity.mqtt.service.sdk.model.MqttServiceMessage;
import com.cumulocity.mqtt.service.sdk.publisher.Publisher;
import com.cumulocity.mqtt.service.sdk.subscriber.Subscriber;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import lombok.NoArgsConstructor;

import java.net.MalformedURLException;

import static com.google.common.base.Preconditions.checkNotNull;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.isBlank;

@NoArgsConstructor(access = PRIVATE)
@Deprecated(forRemoval = true)
public class WebSocketMqttServiceBuilder {

    private final WebSocketConfig config = new WebSocketConfig();

    /**
     * Creates a builder for {@link MqttServiceApi}.
     *
     * @return the created builder
     */
    public static WebSocketMqttServiceBuilder builder() {
        return new WebSocketMqttServiceBuilder();
    }

    /**
     * @param url the server URI to connect to
     * @return self
     */
    public WebSocketMqttServiceBuilder url(final String url) {
        config.setBaseUrl(url);
        return this;
    }

    /**
     * @param tokenApi see <code>TokenApi</code>
     * @return self
     */
    public WebSocketMqttServiceBuilder tokenApi(final TokenApi tokenApi) {
        config.setTokenApi(tokenApi);
        return this;
    }

    /**
     * Connection timeout in millis second until the websocket connected or failed to do so.
     *
     * @param connectionTimeout the connection timeout in millis
     * @return self
     */
    public WebSocketMqttServiceBuilder connectionTimeout(final long connectionTimeout) {
        config.setConnectionTimeout(connectionTimeout);
        return this;
    }

    /**
     * Constructs a {@link MqttServiceApi} instance and sets it to connect to the specified URI. The
     * client does not attempt to connect automatically. The connection will only be established once you
     * obtain the instance of {@link Publisher} or {@link Subscriber} and invoke
     * {@link Publisher#publish(MqttServiceMessage)} or {@link Subscriber#subscribe(MessageListener)}
     *
     * @return a {@link MqttServiceApi} instance
     */
    public MqttServiceApi build() {
        validateURL(config.getBaseUrl());
        checkNotNull(config.getTokenApi(), "Token API cannot be null");

        return new MqttServiceApiImpl(config -> {
            validateTopic(config.getTopic());
            final WebSocketPublisher publisher = new WebSocketPublisher(this.config, config);
            publisher.connect();
            return publisher;
        }, config -> {
            validateTopic(config.getTopic());
            validateSubscriber(config.getSubscriber());
            return new WebSocketSubscriber(this.config, config);
        });
    }

    private void validateURL(String url) {
        checkNotNull(url, "Server URI has to be provided");

        if (!(url.startsWith("ws://") || url.startsWith("wss://"))) {
            throw new MqttServiceException("Server URI should specify either 'ws://' or 'wss://' protocol", new MalformedURLException());
        }
    }

    private void validateTopic(final String topic) {
        if (isBlank(topic)) {
            throw new MqttServiceException("Topic has to be provided");
        }
    }

    private void validateSubscriber(String subscriber) {
        if (isBlank(subscriber)) {
            throw new MqttServiceException("Subscriber has to be provided");
        }
    }

}

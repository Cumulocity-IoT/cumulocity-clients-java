package com.cumulocity.mqtt.service.sdk;

import com.cumulocity.mqtt.service.sdk.publisher.Publisher;
import com.cumulocity.mqtt.service.sdk.publisher.PublisherConfig;
import com.cumulocity.mqtt.service.sdk.subscriber.Subscriber;
import com.cumulocity.mqtt.service.sdk.subscriber.SubscriberConfig;
import com.cumulocity.mqtt.service.sdk.websocket.WebSocketMqttServiceBuilder;

import java.util.Optional;

/**
 * {@link MqttServiceApi} is used to configure and create instances of {@link Publisher} and {@link Subscriber}.
 *
 * @deprecated This API is deprecated and will be removed in a future release.
 * Use a direct connection to the Messaging Service using the Apache Pulsar protocol instead.
 */
@Deprecated(forRemoval = true)
public interface MqttServiceApi extends AutoCloseable {

    static WebSocketMqttServiceBuilder webSocket() {
        return WebSocketMqttServiceBuilder.builder();
    }

    /**
     * Creates an instance of {@link Publisher} with the configured topic.
     *
     * @param config {@link PublisherConfig}
     * @return the instance of {@link Publisher}
     */
    Publisher buildPublisher(PublisherConfig config) throws MqttServiceException;

    /**
     * Creates an instance of {@link Subscriber} with the configured topic.
     *
     * @param config {@link SubscriberConfig}
     * @return the instance of {@link Subscriber}
     */
    Subscriber buildSubscriber(SubscriberConfig config) throws MqttServiceException;

    /**
     * Returns the instance of {@link Publisher} with the given id.
     *
     * @param id the publisher id
     * @return the instance of {@link Publisher} or empty if not found
     */
    Optional<Publisher> getPublisher(String id);

    /**
     * Returns the instance of {@link Subscriber} with the given id.
     *
     * @param id the subscriber id
     * @return the instance of {@link Subscriber} or empty if not found
     */
    Optional<Subscriber> getSubscriber(String id);

    /**
     * Closes the instance of {@link Publisher} with the given id and stop watching it.
     *
     * @param id the publisher id
     */
    void closePublisher(String id);

    /**
     * Closes the instance of {@link Subscriber} with the given id and stop watching it.
     *
     * @param id the subscriber id
     */
    void closeSubscriber(String id);

    /**
     * Closes all instances of {@link Publisher} and {@link Subscriber}.
     */
    @Override
    void close();

}

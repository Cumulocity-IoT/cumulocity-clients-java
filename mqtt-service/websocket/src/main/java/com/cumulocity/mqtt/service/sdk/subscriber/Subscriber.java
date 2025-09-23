package com.cumulocity.mqtt.service.sdk.subscriber;

import com.cumulocity.mqtt.service.sdk.MqttServiceApi;
import com.cumulocity.mqtt.service.sdk.MqttServiceException;
import com.cumulocity.mqtt.service.sdk.listener.MessageListener;

/**
 * Interface for subscribing to messages from the server.
 * <p>
 * {@link Subscriber} instances are created using {@link MqttServiceApi#buildSubscriber(SubscriberConfig)}
 */
@Deprecated(forRemoval = true)
public interface Subscriber extends AutoCloseable {

    /**
     * Starts this subscriber, using the parameter {@link MessageListener} to handle its received messages
     *
     * @param listener the listener object
     */
    void subscribe(MessageListener listener) throws MqttServiceException;

    /**
     * Resubscribes the subscriber by closing the current connection and subscribing again
     */
    void resubscribe() throws MqttServiceException;

    /**
     * Unsubscribes the subscriber
     */
    void unsubscribe();

    /**
     * Checks client connection status.
     *
     * @return true if the client is connected to the server, false otherwise.
     */
    boolean isConnected();

    @Override
    void close();

}

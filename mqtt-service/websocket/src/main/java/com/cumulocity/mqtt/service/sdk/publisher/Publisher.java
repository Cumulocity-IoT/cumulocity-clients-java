package com.cumulocity.mqtt.service.sdk.publisher;

import com.cumulocity.mqtt.service.sdk.MqttServiceApi;
import com.cumulocity.mqtt.service.sdk.MqttServiceException;
import com.cumulocity.mqtt.service.sdk.model.MqttServiceMessage;

/**
 * Interface for publishing messages to the server.
 * <p>
 * {@link Publisher} instances are created using {@link MqttServiceApi#buildPublisher(PublisherConfig)}.
 */
@Deprecated(forRemoval = true)
public interface Publisher extends AutoCloseable {

    /**
     * Reconnect the publisher by closing existing connection if needed and starting the new connection.
     */
    void reconnect() throws MqttServiceException;

    /**
     * Sends {@link MqttServiceMessage message} to the MQTT Service.
     *
     * @param message The {@link MqttServiceMessage} to be published.
     */
    void publish(MqttServiceMessage message);

    /**
     * Checks client connection status.
     *
     * @return true if the client is connected to the server, false otherwise.
     */
    boolean isConnected();

    @Override
    void close();

}

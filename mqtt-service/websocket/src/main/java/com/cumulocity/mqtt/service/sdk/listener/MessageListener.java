package com.cumulocity.mqtt.service.sdk.listener;

import com.cumulocity.mqtt.service.sdk.model.MqttServiceMessage;
import com.cumulocity.mqtt.service.sdk.subscriber.Subscriber;

/**
 * Users must implement this interface and pass the instance of it to {@link Subscriber#subscribe(MessageListener)}
 * in order to handle MQTT messages received from the server.
 */
@FunctionalInterface
@Deprecated(forRemoval = true)
public interface MessageListener {

    /**
     * Callback for {@link MqttServiceMessage} messages received from the remote host. The messages will be acknowledged if no exception is raised.
     *
     * @param message The {@link MqttServiceMessage} that was received.
     */
    void onMessage(MqttServiceMessage message);

}

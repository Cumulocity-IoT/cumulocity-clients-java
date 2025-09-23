package com.cumulocity.mqtt.service.sdk.model.util;

import com.cumulocity.mqtt.service.sdk.model.MqttServiceMessage;

@Deprecated(forRemoval = true)
public interface MessageConverter {

    byte[] encode(MqttServiceMessage message);

    MqttServiceMessage decode(byte[] bytes);

}

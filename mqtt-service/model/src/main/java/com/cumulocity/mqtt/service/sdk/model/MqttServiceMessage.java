package com.cumulocity.mqtt.service.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT Service message representation containing a payload and associated {@link MqttServiceMetadata metadata}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Deprecated(forRemoval = true)
public class MqttServiceMessage {

    private byte[] payload;

    private MqttServiceMetadata metadata;

    /**
     * @return the payload of this message as a byte array. This will be an empty array if the payload is not present.
     */
    public byte[] getPayload() {
        return this.payload;
    }

    /**
     * @return the metadata of this message.
     */
    public MqttServiceMetadata getMetadata() {
        return this.metadata;
    }

}

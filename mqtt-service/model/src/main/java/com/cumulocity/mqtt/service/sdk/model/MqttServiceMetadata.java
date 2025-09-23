package com.cumulocity.mqtt.service.sdk.model;


import lombok.*;
import org.apache.avro.reflect.Nullable;

import java.util.Map;

/**
 * Represents the Metadata of a {@link MqttServiceMessage}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Deprecated(forRemoval = true)
public class MqttServiceMetadata {

    @Nullable
    private String clientId;

    private int messageId;

    private boolean dupFlag;

    @Nullable
    private Map<String, String> userProperties;

    @Nullable
    private PayloadFormatIndicator payloadFormatIndicator;

    @Nullable
    private String contentType;

    @Nullable
    private byte[] correlationData;

    @Nullable
    private String responseTopic;

    @Nullable
    private String topic;

    /**
     * @return the client id of the MQTT client.
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * @return the message id.
     */
    public int getMessageId() {
        return this.messageId;
    }

    /**
     * The duplicate delivery flag.
     * <p>
     *
     * @return false if this is the first occasion the message is sent to the receiver, true otherwise.
     */
    public boolean isDupFlag() {
        return this.dupFlag;
    }

    /**
     * MQTT 5 property, for MQTT 3 this will be always null.
     *
     * @return the user properties of an MQTT payload.
     */
    public Map<String, String> getUserProperties() {
        return this.userProperties;
    }

    /**
     * MQTT 5 property, for MQTT 3 this will be always null.
     *
     * @return indicator for the payload.
     */
    public PayloadFormatIndicator getPayloadFormatIndicator() {
        return this.payloadFormatIndicator;
    }

    /**
     * MQTT 5 property, for MQTT 3 this will be always null.
     *
     * @return the content type.
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * MQTT 5 property, for MQTT 3 this will be always null.
     *
     * @return the correlation data
     */
    public byte[] getCorrelationData() {
        return this.correlationData;
    }

    /**
     * MQTT 5 property, for MQTT 3 this will be always null.
     *
     * @return the response topic
     */
    public String getResponseTopic() {
        return this.responseTopic;
    }

    /**
     * Retrieves the MQTT topic associated with this metadata.
     * <p>
     * For messages published by an MQTT client, this field contains the name of the topic that the message was published to.
     * <p>
     * For messages published by an SDK client, this field contains the value passed to the MqttMetadata constructor,
     * which is not necessarily the same as the MQTT topic the message was published to.
     *
     * @return the MQTT topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets the MQTT topic.
     * <p>
     * Note: Setting this field does not affect the MQTT topic that a message will actually be published on.
     * The value passed here will be included in the metadata of the message but may not necessarily
     * correspond to the actual MQTT topic the message was published to.
     *
     * @param topic the MQTT topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }
}

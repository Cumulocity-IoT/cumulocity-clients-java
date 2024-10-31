package com.cumulocity.sdk.mqtt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

@Value
@Builder
@AllArgsConstructor
public class LastWillDetails {

    /**
     * The topic to publish to
     */
    @NotNull
    private String topic;

    /**
     * The quality of service to publish the message at (0, 1 or 2)
     */
    @NotNull
    private QoS qoS;

    /**
     * Message content
     */
    @NotNull
    private String message;

    /**
     * Whether or not the message should be retained
     */
    private boolean retained;
}

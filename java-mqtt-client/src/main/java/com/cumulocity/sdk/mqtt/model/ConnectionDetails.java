package com.cumulocity.sdk.mqtt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

@Value
@Builder
@AllArgsConstructor
public class ConnectionDetails {

    /**
     * The url to connect to
     */
    @NotNull
    private String host;

    /**
     * The unique clientId/deviceId to connect with
     */
    @NotNull
    private String clientId;

    /**
     * The username to connect with
     */
    @NotNull
    private String userName;

    /**
     * The password for the user
     */
    @NotNull
    private String password;

    /**
     * Clear state at end of connection or not (durable or non-durable subscriptions),
     * by default value being true.
     */
    @Builder.Default
    private boolean cleanSession = true;

    /**
     * The "last will" message that is specified at connection time and that
     * is executed when the client loses the connection.
     */
    private LastWillDetails lastWill;
}

package com.cumulocity.sdk.client.notification2;

/**
 * How Notifications2 API should respond with ACK message on websocket client
 */
public enum AckMode {
    /**
     * Do not respond at all - ACK will be handled manually by the subscriber
     */
    NONE,
    /**
     * Respond immediate after the message is received and before it's processed by the subscriber
     */
    IMMEDIATE,
    /**
     * Respond after the message is successfully processed by the subscriber
     */
    SYNCHRONOUS
}

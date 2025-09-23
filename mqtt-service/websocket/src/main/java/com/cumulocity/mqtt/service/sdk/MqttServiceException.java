package com.cumulocity.mqtt.service.sdk;

@Deprecated(forRemoval = true)
public class MqttServiceException extends RuntimeException {

    public MqttServiceException(String message) {
        super(message);
    }

    public MqttServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}

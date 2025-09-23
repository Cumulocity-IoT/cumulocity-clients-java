package com.cumulocity.mqtt.service.sdk.model;

/**
 * Indicator for the payload.
 */
@Deprecated(forRemoval = true)
public enum PayloadFormatIndicator {

    /**
     * The format of the bytes of the payload is not known.
     */
    UNSPECIFIED,
    /**
     * The format of the bytes of the payload is UTF-8.
     */
    UTF_8
}

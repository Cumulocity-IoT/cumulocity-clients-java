/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customencoders.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.*;

@NoArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
public class EncoderResult {
    private String encodedCommand;
    private Map<String, String> properties;

    private String message;

    private boolean success = true;

    public final EncoderResult setAsFailed(String message) {
        success = false;
        this.message = message;
        return this;
    }

    public static EncoderResult empty() {
        return new EncoderResult();
    }
}

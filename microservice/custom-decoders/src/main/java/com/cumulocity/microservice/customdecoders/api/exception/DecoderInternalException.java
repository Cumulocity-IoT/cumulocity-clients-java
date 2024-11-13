/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customdecoders.api.exception;

import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;

public class DecoderInternalException extends DecoderServiceException {

    public DecoderInternalException(Throwable throwable, String message, DecoderResult result) {
        super(throwable, message, result);
    }
}

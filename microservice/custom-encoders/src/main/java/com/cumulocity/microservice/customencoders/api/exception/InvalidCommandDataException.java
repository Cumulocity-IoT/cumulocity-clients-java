/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customencoders.api.exception;


import com.cumulocity.microservice.customencoders.api.model.EncoderResult;

public class InvalidCommandDataException extends EncoderServiceException {

    public InvalidCommandDataException(Throwable throwable, String message, EncoderResult result) {
        super(throwable, message, result);
    }
}

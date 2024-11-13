/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.payload.exception;

public class PayloadDecodingFailedException extends Exception {

    public PayloadDecodingFailedException(String message) {
        super(message);
    }
    public PayloadDecodingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

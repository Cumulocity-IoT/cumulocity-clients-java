/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.exception;

public class InputDataValidationException extends LpwanServiceException {

    public InputDataValidationException(String message) {
        super(message);
    }

    public InputDataValidationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

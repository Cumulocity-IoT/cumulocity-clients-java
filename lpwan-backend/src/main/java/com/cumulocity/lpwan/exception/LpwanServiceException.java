/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.exception;

import lombok.Getter;

public class LpwanServiceException extends Exception {

    @Getter
    private String url;

    public LpwanServiceException(String message) {
        super(message);
    }

    public LpwanServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public LpwanServiceException(String message, String url) {
        super(message);
        this.url = url;
    }
}

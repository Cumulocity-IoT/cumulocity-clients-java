/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.tenant.option;

public class DecryptFailedException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -4997180499970932425L;

    public DecryptFailedException(String message) {
        super(message);
    }

    public DecryptFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

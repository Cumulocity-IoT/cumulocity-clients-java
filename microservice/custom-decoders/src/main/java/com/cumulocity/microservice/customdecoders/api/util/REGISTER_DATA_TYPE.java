/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customdecoders.api.util;

public enum REGISTER_DATA_TYPE {
    Integer, Float;

    static REGISTER_DATA_TYPE safeValueOf(String v) {
        if(v == null || "".equals(v)) {
            return null;
        }
        return valueOf(v);
    }
}

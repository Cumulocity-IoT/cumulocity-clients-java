/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.payload.uplink.model;

import org.joda.time.DateTime;

public abstract class UplinkMessage {

    public abstract String getPayloadHex();
    public abstract String getExternalId();
    public abstract DateTime getDateTime();

    public Integer getFport() {
        return null;
    }
}

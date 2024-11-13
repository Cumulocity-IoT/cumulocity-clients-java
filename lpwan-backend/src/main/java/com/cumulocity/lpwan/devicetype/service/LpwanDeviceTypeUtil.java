/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.lpwan.devicetype.service;

import c8y.LpwanDevice;
import com.cumulocity.model.idtype.GId;

public class LpwanDeviceTypeUtil {

    private static final String pathPrefix = "inventory/managedObjects/";

    public static void setTypePath(LpwanDevice lpwanDevice, String id) {
        lpwanDevice.setType(typeFor(id));
    }

    public static final String typeFor(String id) {
        return pathPrefix + id;
    }

    public static GId getTypeId(LpwanDevice lpwanDevice) {
        String type = lpwanDevice.getType();
        if (type.startsWith(pathPrefix)) {
            String id = type.substring(pathPrefix.length());
            return new GId(id);
        }
        return null;
    }
}

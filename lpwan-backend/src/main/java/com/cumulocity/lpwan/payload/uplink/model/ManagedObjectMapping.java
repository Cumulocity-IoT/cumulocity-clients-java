/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.payload.uplink.model;

import lombok.Data;

@Data
public class ManagedObjectMapping {

    private String fragmentType;
    
    private String innerType;

    
}

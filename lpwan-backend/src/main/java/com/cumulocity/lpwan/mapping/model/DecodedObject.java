/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.mapping.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DecodedObject {
    
    private Map<String, Object> fields = new HashMap<>();

    public void putValue(Object value) {
        fields.put("value", value);
    }
    
    public void putUnit(Object unit) {
        fields.put("unit", unit);
    }
    
    public Object getValue() {
        return fields.get("value");
    }
    
    public Object getUnit() {
        return fields.get("unit");
    }
    
    public Object getFields() {
        if (getUnit() == null) {
            return getValue();
        } else {
            return fields;
        }
    }

}

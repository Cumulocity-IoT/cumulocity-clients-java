/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.mapping.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class EventFragment {

    private String text;
    
    private String type;
    
    private String fragmentType;
    
    private Map<String, Object> innerObject = new HashMap<>();
    
    private Object innerField;
    
    public void putFragmentValue(String key, DecodedObject decodedObject) {
        innerObject.put(key, decodedObject.getFields());
    }
    
    public Object getFragmentValue(String key) {
        return innerObject.get(key);
    }
    
    public void putFragmentValue(DecodedObject decodedObject) {
        this.innerField = decodedObject.getFields();
    }
}
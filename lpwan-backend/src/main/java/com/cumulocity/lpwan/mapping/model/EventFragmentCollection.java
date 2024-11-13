/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.mapping.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Data;

@Data
public class EventFragmentCollection {
    
    private Map<String, EventFragment> eventMappingsByFragmentType = new HashMap<String, EventFragment>();
    
    public void put(String key, EventFragment value) {
        eventMappingsByFragmentType.put(key, value);
    }
    
    public EventFragment get(String key) {
        return eventMappingsByFragmentType.get(key);
    }
    
    public Set<Map.Entry<String, EventFragment>> entrySet() {
        return eventMappingsByFragmentType.entrySet();
    }
    
}

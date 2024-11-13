/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.mapping.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.Singular;

@Data
public class ManagedObjectFragmentCollection {

    @Singular
    private Map<String, ManagedObjectFragment> managedObjectMappingsByType = new HashMap<String, ManagedObjectFragment>();
    
    public void put(String key, ManagedObjectFragment value) {
        managedObjectMappingsByType.put(key, value);
    }
    
    public ManagedObjectFragment get(String key) {
        return managedObjectMappingsByType.get(key);
    }
    
    public Set<Map.Entry<String, ManagedObjectFragment>> entrySet() {
        return managedObjectMappingsByType.entrySet();
    }
}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.mapping.model;

import lombok.Data;

@Data
public class MappingCollections {
    
    private MeasurementFragmentCollection measurementFragments;
    private EventFragmentCollection eventFragments;
    private EventMappingCollection eventMappings;
    private ManagedObjectFragmentCollection managedObjectFragments;
    private AlarmMappingCollection alarmMappings;
    
    public MappingCollections() {
        measurementFragments = new MeasurementFragmentCollection();
        eventFragments = new EventFragmentCollection();
        eventMappings = new EventMappingCollection();
        managedObjectFragments = new ManagedObjectFragmentCollection();
        alarmMappings = new AlarmMappingCollection();
    }
    
}

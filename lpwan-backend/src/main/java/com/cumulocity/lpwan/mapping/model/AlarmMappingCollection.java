/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.mapping.model;

import java.util.ArrayList;
import java.util.List;

import com.cumulocity.lpwan.payload.uplink.model.AlarmMapping;

public class AlarmMappingCollection {

    private List<AlarmMapping> activateAlarms = new ArrayList<>();
    private List<AlarmMapping> clearAlarms = new ArrayList<>();
    
    public void addToActivateAlarms(AlarmMapping alarmMapping) {
        activateAlarms.add(alarmMapping);
    }
    
    public void addToClearAlarms(AlarmMapping alarmMapping) {
        clearAlarms.add(alarmMapping);
    }
    
    public List<AlarmMapping> getActivateAlarms() {
        return activateAlarms;
    }
    
    public List<AlarmMapping> getClearAlarms() {
        return clearAlarms;
    }
}

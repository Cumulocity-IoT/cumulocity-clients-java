/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customdecoders.api.model;

import com.cumulocity.microservice.customdecoders.api.util.ObjectUtils;
import com.cumulocity.model.event.CumulocityAlarmStatuses;
import com.cumulocity.rest.representation.BaseResourceRepresentation;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.svenson.JSONTypeHint;

import java.io.Serializable;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.*;

@NoArgsConstructor
@Setter
@JsonInclude(Include.NON_NULL)
public class DecoderResult extends BaseResourceRepresentation implements Serializable {

    private List<AlarmRepresentation> internalServiceAlarms;

    private List<EventRepresentation> internalServiceEvents;

    private List<AlarmRepresentation> alarms;

    private Map<String, List<String>> alarmTypesToUpdate;

    private List<EventRepresentation> events;

    private List<MeasurementDto> measurements;

    private List<DataFragmentUpdate> dataFragments;


    @Getter
    private String message;

    @Getter
    private boolean success = true;

    static {
        // Registering the Joda module to serialize/deserialize the org.joda.time.DateTime
        new ObjectMapper().registerModule(new JodaModule());
    }

    public static DecoderResult empty() {
        return new DecoderResult();
    }

    public final DecoderResult setAsFailed(String message) {
        success = false;
        this.message = message;
        clearParsedData();
        // Clean incomplete parsed data and returning internal alarms and events
        alarms = internalServiceAlarms;
        events = internalServiceEvents;
        return this;
    }

    public void addAlarm(AlarmRepresentation alarmRepresentation, boolean internal) {
        if(ObjectUtils.isNull(alarms)) {
            alarms = new ArrayList<>();
        }
        alarms.add(alarmRepresentation);
        if(internal) {
            addInternalAlarm(alarmRepresentation);
        }
    }

    public void addAlarms(Collection<AlarmRepresentation> alarmRepresentations) {
        if(ObjectUtils.isNull(alarms)) {
            alarms = new ArrayList<>();
        }
        alarms.addAll(alarmRepresentations);
    }

    public void addAlarmTypesToClear(String... alarmTypes){
        addAlarmTypesToUpdate(CumulocityAlarmStatuses.CLEARED, alarmTypes);
    }

    public void addAlarmTypesToAcknowledge(String... alarmTypes){
        addAlarmTypesToUpdate(CumulocityAlarmStatuses.ACKNOWLEDGED, alarmTypes);
    }

    public void addAlarmTypesToUpdate(CumulocityAlarmStatuses status, String... alarmTypes){
        if(ObjectUtils.isNull(alarmTypes) || ObjectUtils.isEmpty(alarmTypes) || ObjectUtils.isNull(status)){
            return;
        }

        if(ObjectUtils.isNull(alarmTypesToUpdate)){
            alarmTypesToUpdate = new HashMap<>();
        }

        if(!alarmTypesToUpdate.containsKey(status.name())) {
            alarmTypesToUpdate.put(status.name(), new ArrayList<>());
        }

        Collections.addAll(alarmTypesToUpdate.get(status.name()), alarmTypes);
    }

    public void addEvent(EventRepresentation eventRepresentation, boolean internal) {
        if(ObjectUtils.isNull(events)) {
            events = new ArrayList<>();
        }
        events.add(eventRepresentation);
        if(internal) {
            addInternalEvent(eventRepresentation);
        }
    }

    public void addMeasurement(MeasurementDto measurementDto) {
        if(ObjectUtils.isNull(measurements)) {
            measurements = new ArrayList<>();
        }
        measurements.add(measurementDto);
    }

    public void addMeasurements(Collection<MeasurementDto> measurementDtoCollection) {
        if(ObjectUtils.isNull(measurements)) {
            measurements = new ArrayList<>();
        }
        measurements.addAll(measurementDtoCollection);
    }

    public void addDataFragment(DataFragmentUpdate dataFragmentUpdate) {
        if(ObjectUtils.isNull(dataFragments)) {
            dataFragments = new ArrayList<>();
        }
        dataFragments.add(dataFragmentUpdate);
    }

    private void addInternalAlarm(AlarmRepresentation alarmRepresentation) {
        if(ObjectUtils.isNull(internalServiceAlarms)) {
            internalServiceAlarms = new ArrayList<>();
        }
        internalServiceAlarms.add(alarmRepresentation);
    }

    private void addInternalEvent(EventRepresentation eventRepresentation) {
        if(ObjectUtils.isNull(internalServiceEvents)) {
            internalServiceEvents = new ArrayList<>();
        }
        internalServiceEvents.add(eventRepresentation);
    }

    private void clearParsedData() {
        if(!ObjectUtils.isNull(alarms)) {
            alarms.clear();
        }
        if(!ObjectUtils.isNull(alarmTypesToUpdate)) {
            alarmTypesToUpdate.clear();
        }
        if(!ObjectUtils.isNull(events)) {
            events.clear();
        }
        if(!ObjectUtils.isNull(measurements)) {
            measurements.clear();
        }
        if(!ObjectUtils.isNull(dataFragments)) {
            dataFragments.clear();
        }
    }

    @JSONTypeHint(AlarmRepresentation.class)
    public List<AlarmRepresentation> getAlarms() {
        return alarms;
    }

    @JSONTypeHint(EventRepresentation.class)
    public List<EventRepresentation> getEvents() {
        return events;
    }

    @JSONTypeHint(MeasurementDto.class)
    public List<MeasurementDto> getMeasurements() {
        return measurements;
    }

    @JSONTypeHint(DataFragmentUpdate.class)
    public List<DataFragmentUpdate> getDataFragments() {
        return dataFragments;
    }

    public Map<String, List<String>> getAlarmTypesToUpdate() {
        return alarmTypesToUpdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecoderResult that = (DecoderResult) o;
        return success == that.success &&
                Objects.equals(internalServiceAlarms, that.internalServiceAlarms) &&
                Objects.equals(internalServiceEvents, that.internalServiceEvents) &&
                Objects.equals(alarms, that.alarms) &&
                Objects.equals(alarmTypesToUpdate, that.alarmTypesToUpdate) &&
                Objects.equals(events, that.events) &&
                Objects.equals(measurements, that.measurements) &&
                Objects.equals(dataFragments, that.dataFragments) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalServiceAlarms, internalServiceEvents, alarms, alarmTypesToUpdate, events, measurements, dataFragments, message, success);
    }
}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.payload.service;

import com.cumulocity.lpwan.devicetype.model.UplinkConfiguration;
import com.cumulocity.lpwan.mapping.model.*;
import com.cumulocity.lpwan.payload.exception.PayloadDecodingFailedException;
import com.cumulocity.lpwan.payload.uplink.model.AlarmMapping;
import com.cumulocity.lpwan.payload.uplink.model.EventMapping;
import com.cumulocity.lpwan.payload.uplink.model.ManagedObjectMapping;
import com.cumulocity.lpwan.payload.uplink.model.MeasurementMapping;
import com.cumulocity.microservice.customdecoders.api.model.DataFragmentUpdate;
import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;
import com.cumulocity.microservice.customdecoders.api.model.MeasurementDto;
import com.cumulocity.microservice.customdecoders.api.model.MeasurementValueDto;
import com.cumulocity.model.event.CumulocityAlarmStatuses;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.measurement.MeasurementValue;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.rest.representation.measurement.MeasurementCollectionRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.alarm.AlarmCollection;
import com.cumulocity.sdk.client.alarm.AlarmFilter;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.google.common.collect.FluentIterable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.cumulocity.model.event.CumulocityAlarmStatuses.ACTIVE;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class PayloadMappingService {

    @Autowired
    private MeasurementApi measurementApi;
    @Autowired
    private AlarmApi alarmApi;
    @Autowired
    private EventApi eventApi;
    @Autowired
    private InventoryApi inventoryApi;


    public void addMappingsToCollection(MappingCollections mappingCollections, DecodedObject decodedObject,
                                        UplinkConfiguration uplinkConfiguration) {
        if (uplinkConfiguration.containsMeasurementMapping()) {
            addMeasurementFragment(mappingCollections.getMeasurementFragments(), decodedObject, uplinkConfiguration.getMeasurementMapping());
        }
        if (uplinkConfiguration.containsAlarmMapping()) {
            addAlarmMapping(mappingCollections.getAlarmMappings(), decodedObject, uplinkConfiguration.getAlarmMapping());
        }
        if (uplinkConfiguration.containsEventMapping()) {
            EventMapping eventMapping = uplinkConfiguration.getEventMapping();

            if (StringUtils.isBlank(eventMapping.getFragmentType())) {
                addEventMapping(mappingCollections.getEventMappings(), eventMapping);
            } else {
                addEventFragmentObject(mappingCollections.getEventFragments(), decodedObject, uplinkConfiguration.getEventMapping());
            }
        }
        if (uplinkConfiguration.containsManagedObjectMapping()) {
            addManagedObjectFragment(mappingCollections.getManagedObjectFragments(), decodedObject,
                    uplinkConfiguration.getManagedObjectMapping());
        }

    }

    private void addManagedObjectFragment(ManagedObjectFragmentCollection managedObjectFragmentCollection, DecodedObject decodedObject,
                                          ManagedObjectMapping managedObjectConf) {
        String fragmentType = managedObjectConf.getFragmentType();
        ManagedObjectFragment managedObjectFragment = managedObjectFragmentCollection.get(fragmentType);
        if (managedObjectFragment == null) {
            managedObjectFragment = new ManagedObjectFragment();
            managedObjectFragment.setFragmentType(fragmentType);
        }

        if (StringUtils.isNotBlank(managedObjectConf.getInnerType())) {
            managedObjectFragment.putFragmentValue(managedObjectConf.getInnerType(), decodedObject);
        } else {
            managedObjectFragment.putFragmentValue(decodedObject);
        }

        managedObjectFragmentCollection.put(fragmentType, managedObjectFragment);
    }

    private void addEventFragmentObject(EventFragmentCollection eventFragmentCollection, DecodedObject decodedObject, EventMapping eventConf) {
        String fragmentType = eventConf.getFragmentType();
        EventFragment eventFragment = eventFragmentCollection.get(fragmentType);
        if (eventFragment == null) {
            eventFragment = new EventFragment();
            eventFragment.setText(eventConf.getText());
            eventFragment.setType(eventConf.getType());
            eventFragment.setFragmentType(eventConf.getFragmentType());

        }

        if (StringUtils.isNotBlank(eventConf.getInnerType())) {
            eventFragment.putFragmentValue(eventConf.getInnerType(), decodedObject);
        } else {
            eventFragment.putFragmentValue(decodedObject);
        }
        eventFragmentCollection.put(fragmentType, eventFragment);

    }

    private void addEventMapping(EventMappingCollection eventMappingCollection, EventMapping eventConf) {
        eventMappingCollection.add(eventConf);
    }

    private void addAlarmMapping(AlarmMappingCollection alarmMappingCollection, DecodedObject decodedObject, AlarmMapping alarmConf) {
        Double decodedValue = (Double) decodedObject.getValue();
        if (decodedValue.intValue() == 0) {
            alarmMappingCollection.addToClearAlarms(alarmConf);
        } else {
            alarmMappingCollection.addToActivateAlarms(alarmConf);
        }
    }

    private void addMeasurementFragment(MeasurementFragmentCollection measurementFragmentCollection, DecodedObject decodedObject,
                                        MeasurementMapping measurementConf) {
        String type = measurementConf.getType();
        MeasurementFragment measurementFragment = measurementFragmentCollection.get(type);
        if (measurementFragment == null) {
            measurementFragment = new MeasurementFragment();
            measurementFragment.setType(type);
        }
        measurementFragment.putFragmentValue(measurementConf.getSeries(), decodedObject);
        measurementFragmentCollection.put(type, measurementFragment);
    }

    public void executeMappings(MappingCollections mappingCollections, ManagedObjectRepresentation source, DateTime time) {
        createMeasurements(mappingCollections.getMeasurementFragments(), source, time);
        createEvents(mappingCollections.getEventMappings(), source, time);
        createEventsWithFragments(mappingCollections.getEventFragments(), source, time);
        executeAlarmMappings(mappingCollections.getAlarmMappings(), source, time);
        updateManagedObjects(mappingCollections.getManagedObjectFragments(), source);
    }

    private void createMeasurements(MeasurementFragmentCollection measurementFragmentCollection, ManagedObjectRepresentation source,
                                    DateTime time) {
        for (Map.Entry<String, MeasurementFragment> entry : measurementFragmentCollection.entrySet()) {
            createMeasurement(entry.getValue(), source, time);
        }

    }

    private void createEvents(EventMappingCollection eventMappingCollection, ManagedObjectRepresentation source, DateTime time) {
        for (EventMapping eventMapping : eventMappingCollection.getCollection()) {
            createEvent(eventMapping, source, time);
        }

    }

    private void createEventsWithFragments(EventFragmentCollection eventFragmentCollection, ManagedObjectRepresentation source, DateTime time) {
        for (Map.Entry<String, EventFragment> entry : eventFragmentCollection.entrySet()) {
            createEvent(entry.getValue(), source, time);
        }

    }

    private void executeAlarmMappings(AlarmMappingCollection alarmMappingCollection, ManagedObjectRepresentation source, DateTime time) {
        for (AlarmMapping alarmMapping : alarmMappingCollection.getActivateAlarms()) {
            createAlarm(alarmMapping, source, time);
        }
        for (AlarmMapping alarmMapping : alarmMappingCollection.getClearAlarms()) {
            clearAlarms(alarmMapping, source, time);
        }

    }

    private void updateManagedObjects(ManagedObjectFragmentCollection managedObjectFragmentCollection,
                                      ManagedObjectRepresentation source) {
        for (Map.Entry<String, ManagedObjectFragment> entry : managedObjectFragmentCollection.entrySet()) {
            updateManagedObject(entry.getValue(), source);
        }

    }

    private void createEvent(EventMapping eventMapping, ManagedObjectRepresentation source, DateTime time) {
        EventRepresentation event = new EventRepresentation();
        event.setText(eventMapping.getText());
        event.setType(eventMapping.getType());
        event.setSource(source);
        event.setDateTime(time);
        eventApi.create(event);
    }

    private void createEvent(EventFragment eventFragment, ManagedObjectRepresentation source, DateTime time) {
        EventRepresentation event = new EventRepresentation();
        event.setText(eventFragment.getText());
        event.setType(eventFragment.getType());

        if (eventFragment.getInnerField() != null) {
            event.setProperty(eventFragment.getFragmentType(), eventFragment.getInnerField());
        } else if (eventFragment.getInnerObject().size() > 0) {
            event.setProperty(eventFragment.getFragmentType(), eventFragment.getInnerObject());
        }
        event.setSource(source);
        event.setDateTime(time);
        eventApi.create(event);
    }

    private void createAlarm(AlarmMapping alarmMapping, ManagedObjectRepresentation source, DateTime time) {
        AlarmRepresentation alarm = new AlarmRepresentation();
        alarm.setText(alarmMapping.getText());
        alarm.setType(alarmMapping.getType());
        alarm.setStatus(CumulocityAlarmStatuses.ACTIVE.name());
        alarm.setSeverity(alarmMapping.getSeverity());
        alarm.setSource(source);
        alarm.setDateTime(time);
        alarmApi.create(alarm);
    }

    private void clearAlarms(AlarmMapping alarmMapping, ManagedObjectRepresentation source, DateTime time) {
        AlarmFilter filter = new AlarmFilter().bySource(source.getId()).byType(alarmMapping.getType()).byStatus(CumulocityAlarmStatuses.ACTIVE);
        Iterable<AlarmRepresentation> alarms = alarmApi.getAlarmsByFilter(filter).get(2000).allPages();

        for (AlarmRepresentation alarm : alarms) {
            AlarmRepresentation alarmRepr = new AlarmRepresentation();
            alarmRepr.setId(alarm.getId());
            alarmRepr.setStatus(CumulocityAlarmStatuses.CLEARED.name());
            alarmApi.update(alarmRepr);
        }
    }

    private void createMeasurement(MeasurementFragment measurementFragment, ManagedObjectRepresentation source, DateTime time) {

        MeasurementRepresentation measurement = new MeasurementRepresentation();
        measurement.setType(measurementFragment.getType());
        measurement.setSource(source);
        measurement.setDateTime(time);
        measurement.setProperty(measurementFragment.getType(), measurementFragment.getSeriesObject());

        measurementApi.create(measurement);
    }

    private void updateManagedObject(ManagedObjectFragment managedObjectFragment, ManagedObjectRepresentation source) {

        ManagedObjectRepresentation toUpdate = new ManagedObjectRepresentation();
        toUpdate.setId(source.getId());

        if (managedObjectFragment.getInnerField() != null) {
            toUpdate.setProperty(managedObjectFragment.getFragmentType(), managedObjectFragment.getInnerField());
        } else if (managedObjectFragment.getInnerObject().size() > 0) {
            toUpdate.setProperty(managedObjectFragment.getFragmentType(), managedObjectFragment.getInnerObject());
        }

        inventoryApi.update(toUpdate);
    }

    public void handleCodecServiceResponse(DecoderResult decoderResult, ManagedObjectRepresentation source, String deviceEui) throws PayloadDecodingFailedException {
        if (Objects.isNull(decoderResult)) {
            log.error("Error decoding payload for device EUI '{}'. Returned a null output. Skipping the decoding of the payload part.", deviceEui);
            return;
        }

        if(!decoderResult.isSuccess()) {
            log.error("Error decoding payload for device EUI '{}'. Skipping the decoding of the payload part. \nMessage: {} \nDecoder Result: {}", deviceEui, decoderResult.getMessage(), decoderResult);
            // Continue processing the Decoder Result by processing any alarms or events the decoder's implementer intended to create.
        }
        else {
            log.debug("Handling the decoder response. DecoderResult: \n{}", decoderResult);
        }

        //Create Events
        List<EventRepresentation> eventsToCreate = decoderResult.getEvents();
        if (Objects.nonNull(eventsToCreate) && !eventsToCreate.isEmpty()) {
            for (EventRepresentation event : eventsToCreate) {
                try {
                    eventApi.create(event);
                } catch (SDKException e) {
                    throw new PayloadDecodingFailedException(String.format("Unable to create event for device EUI '%s'", deviceEui), e);
                }
            }
        }

        //Update Alarm Status
        Map<String, List<String>> alarmTypesToUpdate = decoderResult.getAlarmTypesToUpdate();
        if (Objects.nonNull(alarmTypesToUpdate) && !alarmTypesToUpdate.isEmpty()) {
            for (String alarmStatus : alarmTypesToUpdate.keySet()) {
                Iterable<AlarmRepresentation> alarmsMaybe = findAlarmsByTypeAndStatus(source.getId(), alarmTypesToUpdate.get(alarmStatus));
                for (AlarmRepresentation alarm : alarmsMaybe) {
                    alarm.setStatus(alarmStatus);
                    try {
                        alarmApi.update(alarm);
                    } catch (SDKException e) {
                        throw new PayloadDecodingFailedException(String.format("Unable to update alarm with status '%s' for device EUI '%s'", alarmStatus, deviceEui), e);
                    }
                }
            }
        }

        //Create Alarms
        List<AlarmRepresentation> alarmsToCreate = decoderResult.getAlarms();
        if (Objects.nonNull(alarmsToCreate) && !alarmsToCreate.isEmpty()) {
            for (AlarmRepresentation alarm : alarmsToCreate) {
                try {
                    alarmApi.create(alarm);
                } catch (SDKException e) {
                    throw new PayloadDecodingFailedException(String.format("Unable to create alarm for device EUI '%s'", deviceEui), e);
                }
            }
        }

        //Create Measurements
        List<MeasurementDto> measurementDtoList = decoderResult.getMeasurements();
        if (Objects.nonNull(measurementDtoList) && !measurementDtoList.isEmpty()) {
            List<MeasurementRepresentation> measurementsToCreate = new ArrayList<>(measurementDtoList.size());
            for (MeasurementDto oneMeasurement : measurementDtoList) {
                measurementsToCreate.add(createMeasurementFromDto(oneMeasurement, source));
            }
            try {
                MeasurementCollectionRepresentation measurementCollection = new MeasurementCollectionRepresentation();
                measurementCollection.setMeasurements(measurementsToCreate);
                measurementApi.createBulkWithoutResponse(measurementCollection);
            } catch (SDKException e) {
                throw new PayloadDecodingFailedException(String.format("Unable to create measurements for device EUI '%s'", deviceEui), e);
            }
        }

        //Update device managed object
        List<DataFragmentUpdate> dataFragments = decoderResult.getDataFragments();
        if (Objects.nonNull(dataFragments) && !dataFragments.isEmpty()) {
            HashMap<String, Object> attributes = new HashMap<>();
            for (DataFragmentUpdate dataFragmentUpdate : dataFragments) {
                String path = dataFragmentUpdate.getKey();
                if (Strings.isBlank(path)) {
                    continue; // Skip processing this DataFragmentUpdate
                }
                Object value = dataFragmentUpdate.getValueAsObject();
                if (value == null) {
                    value = dataFragmentUpdate.getValue();
                }

                generateNestedMap(path, value, attributes);
            }

            ManagedObjectRepresentation sourceMoToUpdate = ManagedObjects.asManagedObject(source.getId());
            sourceMoToUpdate.setAttrs(attributes);
            try {
                inventoryApi.update(sourceMoToUpdate);
            } catch (SDKException e) {
                throw new PayloadDecodingFailedException(String.format("Unable to update the device with id '%s' and device EUI '%s'", source.getId().getValue(), deviceEui), e);
            }
        }
    }

    void generateNestedMap(String path, Object value, Map<String, Object> attributes) {
        path = path.trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        final int indexOfSlash = path.indexOf('/');
        if (indexOfSlash < 0) {
            path = path.trim();
            Object existingValue = attributes.get(path);
            if (Objects.isNull(existingValue) || !Map.class.isAssignableFrom(existingValue.getClass())) {
                attributes.put(path, value);
            } else {
                ((Map<String, Object>) existingValue).put("value", value);
            }
        } else {
            String newPath = path.split("/")[0].trim();
            Object existingValue = attributes.get(newPath);
            Map<String, Object> existingAttributes;
            if (Objects.isNull(existingValue)) {
                existingAttributes = new HashMap<>();
                attributes.put(newPath, existingAttributes);
            } else if (!Map.class.isAssignableFrom(existingValue.getClass())) {
                existingAttributes = new HashMap<>();
                existingAttributes.put("value", existingValue);
                attributes.put(newPath, existingAttributes);
            } else {
                existingAttributes = (Map<String, Object>) existingValue;
            }

            generateNestedMap(path.substring(indexOfSlash + 1), value, existingAttributes);
        }
    }

    Iterable<AlarmRepresentation> findAlarmsByTypeAndStatus(GId source, List<String> alarmTypes) {
        try {
            if(!CollectionUtils.isEmpty(alarmTypes)) {
                AlarmFilter filter = new AlarmFilter().bySource(source).byType(String.join(",", alarmTypes)).byStatus(ACTIVE);
                AlarmCollection alarms = alarmApi.getAlarmsByFilter(filter);
                return alarms.get().allPages();
            }
        } catch (SDKException e) {
            // This exception is caught to only log and return an empty alarms collection.
            log.debug("Couldn't find any ACTIVE Alarms with type '{}' on source '{}'", String.join(",", alarmTypes), source.getValue());
        }

        return FluentIterable.of();
    }

    MeasurementRepresentation createMeasurementFromDto(MeasurementDto dto, ManagedObjectRepresentation sourceDevice) {
        MeasurementRepresentation measurement = new MeasurementRepresentation();
        measurement.setSource(sourceDevice);
        //copy defined set of properties
        String[] fragmentsToCopyFromSourceDevice = dto.getFragmentsToCopyFromSourceDevice();
        if (Objects.nonNull(fragmentsToCopyFromSourceDevice) && fragmentsToCopyFromSourceDevice.length > 0) {
            for (String fragmentToInclude : dto.getFragmentsToCopyFromSourceDevice()) {
                if (sourceDevice.hasProperty(fragmentToInclude)) {
                    String propertyName = dto.getDeviceFragmentPrefix() + fragmentToInclude;
                    measurement.setProperty(propertyName, sourceDevice.getProperty(fragmentToInclude));
                }
            }
        }

        //copy device name
        if (dto.isIncludeDeviceName() && sourceDevice.getName() != null) {
            measurement.setProperty(dto.getDeviceNameFragment(), sourceDevice.getName());
        }

        measurement.setType(dto.getType());
        measurement.setDateTime(dto.getTime());

        // set series
        ManagedObjectRepresentation series = new ManagedObjectRepresentation();

        for (MeasurementValueDto value : dto.getValues()) {
            MeasurementValue mv = new MeasurementValue();
            mv.setUnit(value.getUnit());
            mv.setValue(value.getValue());
            series.setProperty(value.getSeriesName(), mv);
        }

        measurement.setProperty(dto.getSeries(), series);

        // add additional properties
        Map<String, String> additionalProperties = dto.getAdditionalProperties();
        if (Objects.nonNull(additionalProperties) && !additionalProperties.isEmpty()) {
            for (String key : additionalProperties.keySet()) {
                measurement.setProperty(key, additionalProperties.get(key));
            }
        }

        return measurement;
    }
}

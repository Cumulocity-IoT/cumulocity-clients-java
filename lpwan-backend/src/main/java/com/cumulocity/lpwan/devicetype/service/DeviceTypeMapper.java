/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.devicetype.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cumulocity.lpwan.devicetype.model.DeviceType;
import com.cumulocity.lpwan.devicetype.model.MessageTypes;
import com.cumulocity.lpwan.devicetype.model.UplinkConfiguration;
import com.cumulocity.lpwan.mapping.model.MessageTypeMapping;
import com.cumulocity.lpwan.payload.uplink.model.MessageIdConfiguration;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;

@Component
public class DeviceTypeMapper {

    // Jackson 2 default for legacy device types: unknown fragments are ignored.
    // FAIL_ON_UNKNOWN_PROPERTIES has to stay disabled, as it was on the Spring Boot Jackson 2 bean used before.
    private static final ObjectMapper JSON_MAPPER = JsonMapper.builderWithJackson2Defaults()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public DeviceType convertManagedObjectToDeviceType(ManagedObjectRepresentation managedObject) {
        DeviceType deviceTypeObject = JSON_MAPPER.convertValue(managedObject.getAttrs(), DeviceType.class);

        deviceTypeObject.setType(managedObject.getType());
        deviceTypeObject.setName(managedObject.getName());

        if (deviceTypeObject.getMessageTypes() == null) {
            deviceTypeObject.setMessageTypes(new MessageTypes());
        }

        if (deviceTypeObject.getUplinkConfigurations() == null) {
            deviceTypeObject.setUplinkConfigurations(Lists.<UplinkConfiguration> newArrayList());
        }

        if (deviceTypeObject.getMessageIdConfiguration() == null) {
            deviceTypeObject.setMessageIdConfiguration(new MessageIdConfiguration());
        }

        TypeFactory typeFactory = JSON_MAPPER.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, MessageTypeMapping.class);
        HashMap<String, MessageTypeMapping> map = JSON_MAPPER.convertValue(managedObject.get("c8y_MessageTypes"), mapType);

        if (map == null) {
            Map<String, MessageTypeMapping> emptyMap = new HashMap<>();
            deviceTypeObject.getMessageTypes().setMessageTypeMappings(emptyMap);
        } else {
            deviceTypeObject.getMessageTypes().setMessageTypeMappings(map);
        }

        return deviceTypeObject;
    }
}

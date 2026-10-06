package com.cumulocity.lpwan.devicetype.service;

import com.cumulocity.lpwan.devicetype.model.DeviceType;
import com.cumulocity.lpwan.devicetype.model.UplinkConfiguration;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeviceTypeMapperTest {

    private DeviceTypeMapper deviceTypeMapper = new DeviceTypeMapper();

    @Test
    public void shouldAcceptNullForPrimitivesAndIgnoreUnknownFragments() {
        ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();

        Map<String, Object> uplinkConfiguration = new HashMap<>();
        uplinkConfiguration.put("messageTypeId", 1);
        uplinkConfiguration.put("littleEndian", null);
        uplinkConfiguration.put("signed", null);
        uplinkConfiguration.put("bcd", null);

        managedObject.setProperty("c8y_Registers", List.of(uplinkConfiguration));
        managedObject.setProperty("c8y_IsDeviceType", new HashMap<>());

        DeviceType deviceType = deviceTypeMapper.convertManagedObjectToDeviceType(managedObject);

        assertEquals(1, deviceType.getUplinkConfigurations().size());
        UplinkConfiguration configuration = deviceType.getUplinkConfigurations().get(0);
        assertEquals(1, configuration.getMessageTypeId());
        assertFalse(configuration.isLittleEndian());
        assertFalse(configuration.isSigned());
        assertFalse(configuration.isBcd());
    }

    @Test
    public void shouldInitializeDeviceTypeObjectFields() {
        ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();
        DeviceType deviceType = deviceTypeMapper.convertManagedObjectToDeviceType(managedObject);

        assertNotNull(deviceType.getMessageIdConfiguration());
        assertNotNull(deviceType.getMessageTypes());
        assertNotNull(deviceType.getMessageTypes().getMessageTypeMappings());
        assertNotNull(deviceType.getUplinkConfigurations());
    }

    @Test
    public void shouldInitializeWithMessageTypeMappings() {
        String messageTypeId = "1";
        ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();

        Map<String, Integer[]> registerMappings = new HashMap<>();
        Integer[] registerIndexes = new Integer[]{1};
        registerMappings.put("c8y_Registers", registerIndexes);

        Map<String, Map<String, Integer[]>> messageTypeMappings = new HashMap<>();
        messageTypeMappings.put(messageTypeId, registerMappings);

        managedObject.setProperty("c8y_MessageTypes", messageTypeMappings);

        DeviceType deviceType = deviceTypeMapper.convertManagedObjectToDeviceType(managedObject);

        assertNotNull(deviceType.getMessageTypes());
        assertNotNull(deviceType.getMessageTypes().getMessageTypeMappings());
        assertNotNull(deviceType.getMessageTypes().getMappingIndexesByMessageType(messageTypeId));
        assertEquals(deviceType.getMessageTypes().getMappingIndexesByMessageType(messageTypeId).getRegisterIndexes().length, 1);
    }
}

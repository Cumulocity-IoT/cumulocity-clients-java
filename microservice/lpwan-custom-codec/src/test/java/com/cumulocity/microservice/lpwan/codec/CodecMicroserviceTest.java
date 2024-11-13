/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.lpwan.codec.model.DeviceCommand;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;
import com.cumulocity.microservice.lpwan.codec.model.LpwanCodecDetails;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.microservice.subscription.repository.application.CurrentApplicationApi;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import com.cumulocity.sdk.client.inventory.ManagedObjectCollection;
import com.cumulocity.sdk.client.inventory.PagedManagedObjectCollectionRepresentation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cumulocity.microservice.lpwan.codec.CodecMicroservice.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class CodecMicroserviceTest {
    @Mock
    private ContextService<Credentials> contextService;

    @Mock
    private ApplicationApi applicationApi;

    @Mock
    private InventoryApi inventoryApi;

    @Mock
    private IdentityApi identityApi;

    @Mock
    Codec codec;

    @Mock
    private PlatformProperties platformProperties;

    @Mock
    private Callable<String> mockCallable;

    @Mock
    private InventoryFilter mockInventoryFilter;

    private DeviceInfo deviceInfo_1 = new DeviceInfo("Manufacturer_1", "Model_1");
    private String deviceInfo_1_deviceTypeName = String.format(DEVICE_TYPE_NAME_FORMAT, deviceInfo_1.getDeviceManufacturer(), deviceInfo_1.getDeviceModel());
    private DeviceInfo deviceInfo_2 = new DeviceInfo("Manufacturer_2", "Model_2");
    private String deviceInfo_2_deviceTypeName = String.format(DEVICE_TYPE_NAME_FORMAT, deviceInfo_2.getDeviceManufacturer(), deviceInfo_2.getDeviceModel());

    private DeviceInfo deviceInfo_invalid = new DeviceInfo(null, "Model_1");

    private MicroserviceCredentials credentials = new MicroserviceCredentials("tenant", "username", "password", null, null, null, "appKey");
    private MicroserviceSubscriptionAddedEvent microserviceSubscriptionAddedEvent = new MicroserviceSubscriptionAddedEvent(credentials);

    @Captor
    private ArgumentCaptor<ID> idCaptor;

    @Captor
    private ArgumentCaptor<GId> gIdCaptor;

    @Captor
    private ArgumentCaptor<ExternalIDRepresentation> externalIDRepresentationCaptor;

    @Captor
    private ArgumentCaptor<ManagedObjectRepresentation> managedObjectRepresentationCaptor;

    @InjectMocks
    CodecMicroservice codecMicroservice;


    @Before
    public void setup() {
        reset(contextService, identityApi, inventoryApi);
    }

    @Test
    public void doRegisterDeviceTypes_BothCreateAndUpdate_Success() throws Exception {
        setupIdentityApi(deviceInfo_1, deviceInfo_1_deviceTypeName, false, false);
        setupInventoryApi(deviceInfo_1, deviceInfo_1_deviceTypeName,false, false);

        setupIdentityApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, false);
        setupInventoryApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, false);

        setup_codec_with_2_valid_devices();

        invokeRegisterDeviceTypes(codecMicroservice);

        verify(identityApi, times(2)).getExternalId(idCaptor.capture());
        List<ID> allIds = idCaptor.getAllValues();
        ID deviceInfo_1_ID = allIds.get(0);
        ID deviceInfo_2_ID = allIds.get(1);

        assertEquals(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceInfo_1_ID.getType());
        assertEquals(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceInfo_2_ID.getType());

        assertTrue(deviceInfo_1_ID.getValue().equals(deviceInfo_1_deviceTypeName) || deviceInfo_1_ID.getValue().equals(deviceInfo_2_deviceTypeName));
        assertTrue(deviceInfo_2_ID.getValue().equals(deviceInfo_1_deviceTypeName) || deviceInfo_2_ID.getValue().equals(deviceInfo_2_deviceTypeName));

        verify(identityApi).create(externalIDRepresentationCaptor.capture());
        ExternalIDRepresentation deviceInfo_1_externalId = externalIDRepresentationCaptor.getValue();
        assertEquals(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceInfo_1_externalId.getType());
        assertEquals(deviceInfo_1_deviceTypeName, deviceInfo_1_externalId.getExternalId());

        verify(inventoryApi).create(managedObjectRepresentationCaptor.capture());
        List<ManagedObjectRepresentation> allMOs = managedObjectRepresentationCaptor.getAllValues();
        ManagedObjectRepresentation deviceInfo_1_DeviceType_MO = allMOs.get(0);
        assertEquals(deviceInfo_1_deviceTypeName, deviceInfo_1_DeviceType_MO.getName());
        assertEquals(String.format(DEVICE_TYPE_DESCRIPTION_FORMAT, deviceInfo_1.getDeviceModel(), deviceInfo_1.getDeviceManufacturer()), deviceInfo_1_DeviceType_MO.get(DESCRIPTION));
        assertEquals(Collections.EMPTY_MAP, deviceInfo_1_DeviceType_MO.get(C8Y_IS_DEVICE_TYPE));
        assertEquals(C8Y_LPWAN_DEVICE_TYPE, deviceInfo_1_DeviceType_MO.getType());
        assertEquals(LPWAN_FIELDBUS_TYPE, deviceInfo_1_DeviceType_MO.get(FIELDBUS_TYPE));

        DeviceInfo deviceInfo_1_supportedDevice = new DeviceInfo(deviceInfo_1.getDeviceManufacturer(), deviceInfo_1.getDeviceModel());
        LpwanCodecDetails deviceInfo_1_LpwanCodecDetails = new LpwanCodecDetails("testServiceContextPath", deviceInfo_1_supportedDevice);
        assertEquals(deviceInfo_1_LpwanCodecDetails.getAttributes(), deviceInfo_1_DeviceType_MO.get(C8Y_LPWAN_CODEC_DETAILS));

        verify(inventoryApi).update(managedObjectRepresentationCaptor.capture());
        ManagedObjectRepresentation deviceInfo_2_DeviceType_MO = managedObjectRepresentationCaptor.getValue();
        assertEquals(deviceInfo_2_deviceTypeName + "_ID", deviceInfo_2_DeviceType_MO.getId().getValue());

        DeviceInfo deviceInfo_2_supportedDevice = new DeviceInfo(deviceInfo_2.getDeviceManufacturer(), deviceInfo_2.getDeviceModel());
        LpwanCodecDetails deviceInfo_2_LpwanCodecDetails = new LpwanCodecDetails("testServiceContextPath", deviceInfo_2_supportedDevice);
        assertEquals(deviceInfo_2_LpwanCodecDetails.getAttributes(), deviceInfo_2_DeviceType_MO.get(C8Y_LPWAN_CODEC_DETAILS));
    }

    @Test
    public void doRegisterDeviceTypes_AndCreatePredefinedCommandTemplates_BothCreateAndUpdate_Success() throws Exception {
        DeviceCommand deviceCommand1 = new DeviceCommand("set config", "TestCategory", "{\"set config\":{\"breakpoint\":\"false\",\"selfadapt\":\"true\",\"oneoff\":\"false\",\"alreport\":\"false\",\"pos\":\"0\",\"hb\":\"10\"}}");
        DeviceCommand deviceCommand2 = new DeviceCommand("position request", "TestCategory", "{\"request pos\":{\"latitude\":\"10.35\",\"selfadapt\":\"-1.36\"");
        Set<DeviceCommand> supportedCommands = new HashSet<>();
        supportedCommands.addAll(Arrays.asList(deviceCommand1, deviceCommand2));

        DeviceInfo deviceInfo_1_withSupportedCommands = new DeviceInfo("Manufacturer_1", "Model_1", supportedCommands);
        String deviceInfo_1_deviceTypeName_withSupportedCommands = String.format(DEVICE_TYPE_NAME_FORMAT, deviceInfo_1_withSupportedCommands.getDeviceManufacturer(), deviceInfo_1_withSupportedCommands.getDeviceModel());
        DeviceInfo deviceInfo_2_withSupportedCommands = new DeviceInfo("Manufacturer_2", "Model_2", supportedCommands);
        String deviceInfo_2_deviceTypeName_withSupportedCommands = String.format(DEVICE_TYPE_NAME_FORMAT, deviceInfo_2_withSupportedCommands.getDeviceManufacturer(), deviceInfo_2_withSupportedCommands.getDeviceModel());

        ManagedObjectRepresentation predefinedCommand1 = new ManagedObjectRepresentation();
        predefinedCommand1.setId(GId.asGId(1988));
        predefinedCommand1.setName(deviceCommand2.getName());
        predefinedCommand1.setType(C8Y_DEVICE_SHELL_TEMPLATE);
        predefinedCommand1.set(new String[] {deviceInfo_2_deviceTypeName_withSupportedCommands}, DEVICE_TYPE);
        predefinedCommand1.set(deviceCommand2.getCommand(), COMMAND);
        predefinedCommand1.set(deviceCommand2.getCategory(), CATEGORY);

        Iterable<ManagedObjectRepresentation> allPredefinedCommands = Arrays.asList(predefinedCommand1);

        //when(inventoryApi.getManagedObjectsByFilter(any(InventoryFilter.class).byType(anyString()))).thenReturn(mock(ManagedObjectCollection.class).get().allPages());

        setupIdentityApi(deviceInfo_1_withSupportedCommands, deviceInfo_1_deviceTypeName_withSupportedCommands, false, false);
        setupInventoryApi(deviceInfo_1_withSupportedCommands, deviceInfo_1_deviceTypeName_withSupportedCommands,false, false);

        setupIdentityApi(deviceInfo_2_withSupportedCommands, deviceInfo_2_deviceTypeName_withSupportedCommands, true, false);
        setupInventoryApi(deviceInfo_2_withSupportedCommands, deviceInfo_2_deviceTypeName_withSupportedCommands, true, false);

        ManagedObjectCollection mockManagedObjectCollection = mock(ManagedObjectCollection.class);
        PagedManagedObjectCollectionRepresentation pagedManagedObjectCollectionRepresentation = mock(PagedManagedObjectCollectionRepresentation.class);

        when(pagedManagedObjectCollectionRepresentation.allPages()).thenReturn(allPredefinedCommands);
        when(mockManagedObjectCollection.get()).thenReturn(pagedManagedObjectCollectionRepresentation);
        when(inventoryApi.getManagedObjectsByFilter(any(InventoryFilter.class))).thenReturn(mockManagedObjectCollection);

        when(contextService.callWithinContext(eq(credentials),any(Callable.class))).thenReturn("testServiceContextPath");
        when(codec.supportsDevices()).thenReturn(Stream.of(new DeviceInfo[] {deviceInfo_1_withSupportedCommands, deviceInfo_2_withSupportedCommands})
                .collect(Collectors.toCollection(HashSet::new)));

        invokeRegisterDeviceTypes(codecMicroservice);

        verify(identityApi, times(2)).getExternalId(idCaptor.capture());
        List<ID> allIds = idCaptor.getAllValues();
        ID deviceInfo_1_ID = allIds.get(0);
        ID deviceInfo_2_ID = allIds.get(1);

        assertEquals(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceInfo_1_ID.getType());
        assertEquals(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceInfo_2_ID.getType());

        assertTrue(deviceInfo_1_ID.getValue().equals(deviceInfo_1_deviceTypeName_withSupportedCommands) || deviceInfo_1_ID.getValue().equals(deviceInfo_2_deviceTypeName_withSupportedCommands));
        assertTrue(deviceInfo_2_ID.getValue().equals(deviceInfo_1_deviceTypeName_withSupportedCommands) || deviceInfo_2_ID.getValue().equals(deviceInfo_2_deviceTypeName_withSupportedCommands));

        verify(identityApi).create(externalIDRepresentationCaptor.capture());
        ExternalIDRepresentation deviceInfo_1_externalId = externalIDRepresentationCaptor.getValue();
        assertEquals(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceInfo_1_externalId.getType());
        assertEquals(deviceInfo_1_deviceTypeName_withSupportedCommands, deviceInfo_1_externalId.getExternalId());

        verify(inventoryApi, times(5)).create(managedObjectRepresentationCaptor.capture());
        List<ManagedObjectRepresentation> allMOs = managedObjectRepresentationCaptor.getAllValues();
        ManagedObjectRepresentation deviceInfo_1_DeviceType_MO = allMOs.get(0);
        assertEquals(deviceInfo_1_deviceTypeName_withSupportedCommands, deviceInfo_1_DeviceType_MO.getName());
        assertEquals(String.format(DEVICE_TYPE_DESCRIPTION_FORMAT, deviceInfo_1_withSupportedCommands.getDeviceModel(), deviceInfo_1_withSupportedCommands.getDeviceManufacturer()), deviceInfo_1_DeviceType_MO.get(DESCRIPTION));
        assertEquals(Collections.EMPTY_MAP, deviceInfo_1_DeviceType_MO.get(C8Y_IS_DEVICE_TYPE));
        assertEquals(C8Y_LPWAN_DEVICE_TYPE, deviceInfo_1_DeviceType_MO.getType());
        assertEquals(LPWAN_FIELDBUS_TYPE, deviceInfo_1_DeviceType_MO.get(FIELDBUS_TYPE));

        DeviceInfo deviceInfo_1_supportedDevice = new DeviceInfo(deviceInfo_1_withSupportedCommands.getDeviceManufacturer(), deviceInfo_1_withSupportedCommands.getDeviceModel(), deviceInfo_1_withSupportedCommands.getSupportedCommands());
        LpwanCodecDetails deviceInfo_1_LpwanCodecDetails = new LpwanCodecDetails("testServiceContextPath", deviceInfo_1_supportedDevice);
        assertEquals(deviceInfo_1_LpwanCodecDetails.getAttributes(), deviceInfo_1_DeviceType_MO.get(C8Y_LPWAN_CODEC_DETAILS));

        verify(inventoryApi).update(managedObjectRepresentationCaptor.capture());
        ManagedObjectRepresentation deviceInfo_2_DeviceType_MO = managedObjectRepresentationCaptor.getValue();
        assertEquals(deviceInfo_2_deviceTypeName_withSupportedCommands + "_ID", deviceInfo_2_DeviceType_MO.getId().getValue());

        DeviceInfo deviceInfo_2_supportedDevice = new DeviceInfo(deviceInfo_2_withSupportedCommands.getDeviceManufacturer(), deviceInfo_2_withSupportedCommands.getDeviceModel(), deviceInfo_2_withSupportedCommands.getSupportedCommands());
        LpwanCodecDetails deviceInfo_2_LpwanCodecDetails = new LpwanCodecDetails("testServiceContextPath", deviceInfo_2_supportedDevice);
        assertEquals(deviceInfo_2_LpwanCodecDetails.getAttributes(), deviceInfo_2_DeviceType_MO.get(C8Y_LPWAN_CODEC_DETAILS));

        // Verify deletion of predefined commands
        verify(inventoryApi, times(2)).delete(gIdCaptor.capture());
        List<GId> gidList = gIdCaptor.getAllValues();
        assertEquals(GId.asGId(1988), gidList.get(0));
        assertEquals(GId.asGId(1988), gidList.get(1));

        String[] deliveryTypes = new String[] { "Default" };

        // verify creation of predefined templates for every operation
        // For device 1
        ManagedObjectRepresentation predefCommand1_device1 = allMOs.get(1);
        assertEquals(C8Y_DEVICE_SHELL_TEMPLATE, predefCommand1_device1.getType());
        assertEquals(deviceCommand2.getName(), predefCommand1_device1.getName());
        assertEquals(deviceCommand2.getCommand(), predefCommand1_device1.get(COMMAND));
        assertEquals(deviceCommand2.getCategory(), predefCommand1_device1.get(CATEGORY));
        assertArrayEquals(deliveryTypes, (String[])predefCommand1_device1.get(DELIVERY_TYPES));

        ManagedObjectRepresentation predefCommand2_device1 = allMOs.get(2);
        assertEquals(C8Y_DEVICE_SHELL_TEMPLATE, predefCommand2_device1.getType());
        assertEquals(deviceCommand1.getName(), predefCommand2_device1.getName());
        assertEquals(deviceCommand1.getCommand(), predefCommand2_device1.get(COMMAND));
        assertEquals(deviceCommand1.getCategory(), predefCommand2_device1.get(CATEGORY));
        assertArrayEquals(deliveryTypes, (String[])predefCommand2_device1.get(DELIVERY_TYPES));

        // For device 2
        ManagedObjectRepresentation predefCommand1_device2 = allMOs.get(3);
        assertEquals(C8Y_DEVICE_SHELL_TEMPLATE, predefCommand1_device2.getType());
        assertEquals(deviceCommand2.getName(), predefCommand1_device2.getName());
        assertEquals(deviceCommand2.getCommand(), predefCommand1_device2.get(COMMAND));
        assertEquals(deviceCommand2.getCategory(), predefCommand1_device2.get(CATEGORY));
        assertArrayEquals(deliveryTypes, (String[])predefCommand1_device2.get(DELIVERY_TYPES));

        ManagedObjectRepresentation predefCommand2_device2 = allMOs.get(4);
        assertEquals(C8Y_DEVICE_SHELL_TEMPLATE, predefCommand2_device2.getType());
        assertEquals(deviceCommand1.getName(), predefCommand2_device2.getName());
        assertEquals(deviceCommand1.getCommand(), predefCommand2_device2.get(COMMAND));
        assertEquals(deviceCommand1.getCategory(), predefCommand2_device2.get(CATEGORY));
        assertArrayEquals(deliveryTypes, (String[])predefCommand2_device2.get(DELIVERY_TYPES));
    }

    @Test
    public void doRegisterDeviceTypes_BothCreateAndUpdate_InventoryApiThrowsException_Failure() throws Exception {
        setupIdentityApi(deviceInfo_1, deviceInfo_1_deviceTypeName, false, false);
        setupInventoryApi(deviceInfo_1, deviceInfo_1_deviceTypeName,false, true);

        setupIdentityApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, false);
        setupInventoryApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, true);

        setup_codec_with_2_valid_devices();
        invokeRegisterDeviceTypes(codecMicroservice);

        verify(identityApi, times(2)).getExternalId(idCaptor.capture());

        verify(identityApi, never()).create(externalIDRepresentationCaptor.capture());

        verify(inventoryApi).create(managedObjectRepresentationCaptor.capture());

        verify(inventoryApi).update(managedObjectRepresentationCaptor.capture());
    }

    @Test
    public void doRegisterDeviceTypes_BothCreateAndUpdate_IdentityApiThrowsException_Failure() throws Exception {
        setupIdentityApi(deviceInfo_1, deviceInfo_1_deviceTypeName, false, true);
        setupInventoryApi(deviceInfo_1, deviceInfo_1_deviceTypeName,false, false);

        setupIdentityApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, true);
        setupInventoryApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, false);

        setup_codec_with_2_valid_devices();
        invokeRegisterDeviceTypes(codecMicroservice);

        verify(identityApi, times(2)).getExternalId(idCaptor.capture());

        verify(identityApi).create(externalIDRepresentationCaptor.capture());

        verify(inventoryApi).create(managedObjectRepresentationCaptor.capture());

        verify(inventoryApi).update(managedObjectRepresentationCaptor.capture());
    }

    @Test
    public void doRegisterDeviceTypes_BothCreateAndUpdate_InvalidDeviceInfo_Failure() throws Exception {
        setupIdentityApi(deviceInfo_1, deviceInfo_1_deviceTypeName, false, false);
        setupInventoryApi(deviceInfo_1, deviceInfo_1_deviceTypeName, false, false);

        setupIdentityApi(deviceInfo_invalid, "deviceInfo_invalid_deviceTypeName", false, false);
        setupInventoryApi(deviceInfo_invalid, "deviceInfo_invalid_deviceTypeName",false, false);

        setupIdentityApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, false);
        setupInventoryApi(deviceInfo_2, deviceInfo_2_deviceTypeName, true, false);

        setup_codec_with_invalidDeviceInfo();
        invokeRegisterDeviceTypes(codecMicroservice);

        verify(identityApi, times(2)).getExternalId(idCaptor.capture());

        verify(identityApi).create(externalIDRepresentationCaptor.capture());

        verify(inventoryApi).create(managedObjectRepresentationCaptor.capture());

        verify(inventoryApi).update(managedObjectRepresentationCaptor.capture());
    }

    private void setup_codec_with_2_valid_devices() {
//        when(codec.getMicroserviceContextPath()).thenReturn("testServiceContextPath");
        when(contextService.callWithinContext(eq(credentials),any(Callable.class))).thenReturn("testServiceContextPath");
        when(codec.supportsDevices()).thenReturn(Stream.of(new DeviceInfo[] {deviceInfo_1, deviceInfo_2})
                .collect(Collectors.toCollection(HashSet::new)));
    }

    private void setup_codec_with_invalidContextPath() {
//        when(codec.getMicroserviceContextPath()).thenReturn(null);
        when(contextService.callWithinContext(eq(credentials),any(Callable.class))).thenReturn(null);
        when(codec.supportsDevices()).thenReturn(Stream.of(new DeviceInfo[] {deviceInfo_1, deviceInfo_2})
                .collect(Collectors.toCollection(HashSet::new)));
    }

    private void setup_codec_with_invalidDeviceInfo() {
//        when(codec.getMicroserviceContextPath()).thenReturn("testServiceContextPath");
        when(contextService.callWithinContext(eq(credentials),any(Callable.class))).thenReturn("testServiceContextPath");
        when(codec.supportsDevices()).thenReturn(Stream.of(new DeviceInfo[] {deviceInfo_1, deviceInfo_invalid, deviceInfo_2})
                .collect(Collectors.toCollection(HashSet::new)));
    }

    private void invokeRegisterDeviceTypes(CodecMicroservice codecMicroservice) throws Exception {

        CurrentApplicationApi currentApplicationApi = mock(CurrentApplicationApi.class);
        ApplicationRepresentation applicationRepresentation = mock(ApplicationRepresentation.class);
        when(platformProperties.getMicroserviceBoostrapUser()).thenReturn(credentials);
        when(applicationApi.currentApplication()).thenReturn(currentApplicationApi);
        when(currentApplicationApi.get()).thenReturn(applicationRepresentation);
        when(applicationRepresentation.getContextPath()).thenReturn("testServiceContextPath");

//        when(contextService.callWithinContext(eq(credentials),any(Callable.class))).thenReturn("testServiceContextPath");
        codecMicroservice.registerDeviceTypes(microserviceSubscriptionAddedEvent);

        ArgumentCaptor<Callable> callableArgumentCaptor = ArgumentCaptor.forClass(Callable.class);
        verify(contextService).callWithinContext(eq(credentials), callableArgumentCaptor.capture());
        callableArgumentCaptor.getValue().call();

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(contextService).runWithinContext(eq(credentials), taskCaptor.capture());
        taskCaptor.getValue().run();
    }

    private void setupIdentityApi(DeviceInfo deviceInfo, String deviceTypeName, boolean isDeviceTypeExist, boolean throwException) {
        ID id = new ID(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceTypeName);

        ExternalIDRepresentation externalIDRepresentation = new ExternalIDRepresentation();
        externalIDRepresentation.setExternalId(deviceTypeName);
        externalIDRepresentation.setType(C8Y_SMART_REST_DEVICE_IDENTIFIER);
        externalIDRepresentation.setManagedObject(ManagedObjects.asManagedObject(GId.asGId(deviceTypeName + "_ID")));

        // Setup for IdentityApi#getExternalId() method
        if(!isDeviceTypeExist) {
            when(identityApi.getExternalId(eq(id))).thenThrow(new SDKException("Simulated error in IdentityApi"));
        }
        else {
            when(identityApi.getExternalId(eq(id))).thenReturn(externalIDRepresentation);
        }

        // Setup for IdentityApi#create() methods
        if(!isDeviceTypeExist) {
            if(!throwException) {
                when(identityApi.create(any(ExternalIDRepresentation.class))).thenReturn(null);
            }
            else {
                when(identityApi.create(any(ExternalIDRepresentation.class))).thenThrow(new SDKException("Simulated error in IdentityApi"));
            }
        }
    }

    private void setupInventoryApi(DeviceInfo deviceInfo, String deviceTypeName, boolean isDeviceTypeExist, boolean throwException) {
        if(!isDeviceTypeExist) {
            if(!throwException) {
                when(inventoryApi.create(any(ManagedObjectRepresentation.class))).thenReturn(ManagedObjects.asManagedObject(GId.asGId(deviceTypeName + "_ID")));
            }
            else {
                when(inventoryApi.create(any(ManagedObjectRepresentation.class))).thenThrow(new SDKException("Simulated error in InventoryApi"));
            }
        }
        else {
            if(!throwException) {
                when(inventoryApi.update(any(ManagedObjectRepresentation.class))).thenReturn(ManagedObjects.asManagedObject(GId.asGId(deviceTypeName + "_ID")));
            }
            else {
                when(inventoryApi.update(any(ManagedObjectRepresentation.class))).thenThrow(new SDKException("Simulated error in InventoryApi"));
            }
        }
    }
}
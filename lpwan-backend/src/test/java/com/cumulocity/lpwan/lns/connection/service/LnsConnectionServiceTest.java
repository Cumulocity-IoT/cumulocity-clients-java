/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.lns.connection.service;

import c8y.LpwanDevice;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cumulocity.lpwan.exception.InputDataValidationException;
import com.cumulocity.lpwan.exception.LpwanServiceException;
import com.cumulocity.lpwan.lns.connection.event.LnsConnectionUpdateEvent;
import com.cumulocity.lpwan.lns.connection.model.LnsConnection;
import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.platform.repository.LpwanRepository;
import com.cumulocity.lpwan.sample.connection.model.SampleConnection;
import com.cumulocity.lpwan.util.LpwanCommonService;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import com.cumulocity.sdk.client.inventory.ManagedObjectCollection;
import com.cumulocity.sdk.client.inventory.PagedManagedObjectCollectionRepresentation;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.cumulocity.lpwan.platform.repository.LpwanRepository.ALARM_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class LnsConnectionServiceTest {

    @Mock
    private TenantOptionApi tenantOptionApi;

    @Mock
    private InventoryApi inventoryApi;

    @Mock
    private RestConnector restConnector;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Spy
    private CsvService csvService;

    @Spy
    TaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

    @Captor
    ArgumentCaptor<Instant> instantArgumentCaptor;

    @Mock
    private MicroserviceSubscriptionsService subscriptionsService;

    @Mock
    private ContextService<MicroserviceCredentials> contextService;

    @Mock
    private LpwanRepository lpwanRepository;

    @Mock
    private LpwanCommonService lpwanCommonService;

    @InjectMocks
    private LnsConnectionService lnsConnectionService;

    @Captor
    private ArgumentCaptor<OptionRepresentation> optionRepresentationCaptor;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final MapType mapType = JSON_MAPPER.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, LnsConnection.class);

    // Valid JSON String representing LNS Connections map stored in the tenant options
    //    {
    //        "SampleConnection-1": {
    //                "name": "SampleConnection-1",
    //                "description": "Description for SampleConnection-1",
    //                "user": "user-1",
    //                "password": "password-1"
    //        },
    //        "SampleConnection-2": {
    //                "name": "SampleConnection-2",
    //                "description": "Description for SampleConnection-2",
    //                "user": "user-2",
    //                "password": "password-3"
    //        },
    //        "SampleConnection-3": {
    //                "name": "SampleConnection-3",
    //                "description": "Description for SampleConnection-3",
    //                "user": "user-3",
    //                "password": "password-3"
    //        }
    //    }
    private final String VALID_LNS_CONNECTIONS_MAP_JSON = "{\"sampleconnection-1\":{\"name\":\"sampleconnection-1\",\"description\":\"Description for SampleConnection-1\",\"user\":\"user-1\",\"password\":\"password-1\"},\"sampleconnection-2\":{\"name\":\"sampleconnection-2\",\"description\":\"Description for SampleConnection-2\",\"user\":\"user-2\",\"password\":\"password-3\"},\"sampleconnection-3\":{\"name\":\"sampleconnection-3\",\"description\":\"Description for SampleConnection-3\",\"user\":\"user-3\",\"password\":\"password-3\"}}";
    private Map<String, LnsConnection> VALID_LNS_CONNECTIONS_MAP;
    private final String EMPTY_LNS_CONNECTIONS_MAP_JSON = "{}";

    @Before
    public void setup() throws JsonProcessingException {
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("sample", SampleConnection.class);

        VALID_LNS_CONNECTIONS_MAP = JSON_MAPPER.readerWithView(LnsConnection.InternalView.class)
                                             .forType(mapType)
                                             .readValue(VALID_LNS_CONNECTIONS_MAP_JSON);
    }

    @Test
    public void doLoadLnsConnectionFromTenantOptions_getOption_returns_VALID_lnsConnections_map() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        Map<String, LnsConnection> lnsConnectionsMap = ReflectionTestUtils.invokeMethod(lnsConnectionService, "loadLnsConnectionsFromTenantOptions", lnsConnectionsOptionKey);

        compare(VALID_LNS_CONNECTIONS_MAP, lnsConnectionsMap);
    }

    @Test
    public void doLoadLnsConnectionFromTenantOptions_getOption_returns_EMPTY_lnsConnections_map() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                EMPTY_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        Map<String, LnsConnection> lnsConnectionsMap = ReflectionTestUtils.invokeMethod(lnsConnectionService, "loadLnsConnectionsFromTenantOptions", lnsConnectionsOptionKey);

        compare(new ConcurrentHashMap<>(), lnsConnectionsMap);
    }

    @Test
    public void doLoadLnsConnectionFromTenantOptions_getOption_returns_NULL_lnsConnections_String() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                null);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        Map<String, LnsConnection> lnsConnectionsMap = ReflectionTestUtils.invokeMethod(lnsConnectionService, "loadLnsConnectionsFromTenantOptions", lnsConnectionsOptionKey);

        compare(new ConcurrentHashMap<>(), lnsConnectionsMap);
    }

    @Test
    public void doLoadLnsConnectionFromTenantOptions_getOption_returns_BLANK_lnsConnections_String() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                " ");

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        Map<String, LnsConnection> lnsConnectionsMap = ReflectionTestUtils.invokeMethod(lnsConnectionService, "loadLnsConnectionsFromTenantOptions", lnsConnectionsOptionKey);

        compare(new ConcurrentHashMap<>(), lnsConnectionsMap);
    }

    @Test
    public void doLoadLnsConnectionFromTenantOptions_getOption_throws_httpStatus_NOT_FOUND_exception() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenThrow(new SDKException(HttpStatus.NOT_FOUND.value(), "NOT FOUND"));

        Map<String, LnsConnection> lnsConnectionsMap = ReflectionTestUtils.invokeMethod(lnsConnectionService, "loadLnsConnectionsFromTenantOptions", lnsConnectionsOptionKey);

        compare(new ConcurrentHashMap<>(), lnsConnectionsMap);
    }

    @Test
    public void doLoadLnsConnectionFromTenantOptions_getOption_throws_some_unexpected_exception() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenThrow(new SDKException("SOME UNEXPECTED ERROR"));

        // Since we use ReflectionTestUtils to invoke the private method, any exception thrown by the target is wrapped in UndeclaredThrowableException.
        // In this case LpwanServiceException thrown by the target method is returned wrapped with UndeclaredThrowableException
        UndeclaredThrowableException undeclaredThrowableException = assertThrows(UndeclaredThrowableException.class, () -> ReflectionTestUtils.invokeMethod(lnsConnectionService, "loadLnsConnectionsFromTenantOptions", lnsConnectionsOptionKey));

        assertEquals(String.format("Error while fetching the tenant option with key '%s'.", lnsConnectionsOptionKey), undeclaredThrowableException.getCause().getMessage());
    }

    @Test
    public void doLoadLnsConnectionFromTenantOptions_JSON_MAPPER_throws_JsonProcessingException() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                "INVALID JSON");

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        // Since we use ReflectionTestUtils to invoke the private method, any exception thrown by the target is wrapped in UndeclaredThrowableException.
        // In this case LpwanServiceException thrown by the target method is returned wrapped with UndeclaredThrowableException
        UndeclaredThrowableException undeclaredThrowableException = assertThrows(UndeclaredThrowableException.class, () -> ReflectionTestUtils.invokeMethod(lnsConnectionService, "loadLnsConnectionsFromTenantOptions", lnsConnectionsOptionKey));

        assertEquals(String.format("Error unmarshalling the below JSON string containing LNS connection map stored as a tenant option with key '%s'. \n%s", lnsConnectionsOptionKey, "INVALID JSON"), undeclaredThrowableException.getCause().getMessage());
    }

    @Test
    public void doFlushCache_EMPTY_lnsConnections_map() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                null);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(eq(lnsConnectionsOptionRepresentation))).thenReturn(lnsConnectionsOptionRepresentation);

        ReflectionTestUtils.invokeMethod(lnsConnectionService, "flushCache");

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), EMPTY_LNS_CONNECTIONS_MAP_JSON), optionRepresentationArgument);
    }

    @Test
    public void doFlushCache_VALID_lnsConnections_map() throws Exception {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(lnsConnectionsOptionRepresentation);

        ReflectionTestUtils.invokeMethod(lnsConnectionService, "flushCache");

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(lnsConnectionsOptionKey.getCategory(), optionRepresentationArgument.getCategory());
        assertEquals(lnsConnectionsOptionKey.getKey(), optionRepresentationArgument.getKey());
        new JsonExpectationsHelper().assertJsonEqual(VALID_LNS_CONNECTIONS_MAP_JSON, optionRepresentationArgument.getValue(), true);
    }

    @Test
    public void doFlushCache_getLnsConnections_throws_LpwanServiceException() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                "INVALID JSON");

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        // Since we use ReflectionTestUtils to invoke the private method, any exception thrown by the target is wrapped in UndeclaredThrowableException.
        // In this case LpwanServiceException thrown by the target method is returned wrapped with UndeclaredThrowableException
        UndeclaredThrowableException undeclaredThrowableException = assertThrows(UndeclaredThrowableException.class, () -> ReflectionTestUtils.invokeMethod(lnsConnectionService, "flushCache"));

        assertEquals(String.format("Unexpected error occurred while accessing the cached LNS connections map with key '%s'.", lnsConnectionsOptionKey), undeclaredThrowableException.getCause().getMessage());
    }

    @Test
    public void doFlushCache_tenantOptionApi_throws_SDKException() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(
                lnsConnectionsOptionKey.getCategory(),
                lnsConnectionsOptionKey.getKey(),
                EMPTY_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(eq(lnsConnectionsOptionRepresentation))).thenThrow(new SDKException("Error while saving."));

        // Since we use ReflectionTestUtils to invoke the private method, any exception thrown by the target is wrapped in UndeclaredThrowableException.
        // In this case LpwanServiceException thrown by the target method is returned wrapped with UndeclaredThrowableException
        UndeclaredThrowableException undeclaredThrowableException = assertThrows(UndeclaredThrowableException.class, () -> ReflectionTestUtils.invokeMethod(lnsConnectionService, "flushCache"));

        assertEquals(String.format("Error saving the below LNS connection map as a tenant option with key '%s'. \n%s", lnsConnectionsOptionKey, EMPTY_LNS_CONNECTIONS_MAP_JSON), undeclaredThrowableException.getCause().getMessage());
    }

    @Test
    public void doGetByName_with_existing_name() throws LpwanServiceException {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        String name = "SampleConnection-1";
        LnsConnection sampleConnection_1 = lnsConnectionService.getByName(name);

        compare(VALID_LNS_CONNECTIONS_MAP.get(name.toLowerCase()), sampleConnection_1);
    }

    @Test
    public void doGetByName_with_non_existing_name() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        String name = "SOME_NAME";
        InputDataValidationException notFoundException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.getByName(name));
        assertEquals(String.format("LNS connection named '%s' doesn't exist.", name.toLowerCase()), notFoundException.getMessage());
    }

    @Test
    public void doGetByName_with_no_connections_in_tenant_options() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), EMPTY_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        String name = "SOME_NAME";
        InputDataValidationException notFoundException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.getByName(name));
        assertEquals(String.format("LNS connection named '%s' doesn't exist.", name.toLowerCase()), notFoundException.getMessage());
    }

    @Test
    public void doGetByName_inputValidation_null_name() {
        InputDataValidationException inputDataValidationException = Assert.assertThrows(InputDataValidationException.class, () -> lnsConnectionService.getByName(null));
        assertEquals("LNS connection name can't be null or blank.", inputDataValidationException.getMessage());
    }

    @Test
    public void doGetByName_inputValidation_blank_name() {
        InputDataValidationException inputDataValidationException = Assert.assertThrows(InputDataValidationException.class, () -> lnsConnectionService.getByName(""));
        assertEquals("LNS connection name can't be null or blank.", inputDataValidationException.getMessage());
    }

    @Test
    public void doGetAll_non_empty() throws LpwanServiceException {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        Collection<LnsConnection> allConnections = lnsConnectionService.getAll();

        compare(VALID_LNS_CONNECTIONS_MAP, allConnections);
    }

    @Test
    public void doGetAll_empty() throws LpwanServiceException {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), EMPTY_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        Collection<LnsConnection> allConnections = lnsConnectionService.getAll();

        compare(new ConcurrentHashMap<>(), allConnections);
    }

    @Test
    public void doCreate_valid_connection() throws Exception {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        LnsConnection connectionToCreate = SampleConnection.builder()
                                        .name("Sample Connection Name")
                                        .description("Sample Connection Description")
                                        .user("USER NAME")
                                        .password("**********")
                                        .build();
        connectionToCreate.setName("Sample Connection Name");
        LnsConnection createdConnection = lnsConnectionService.create(connectionToCreate);

        compare(connectionToCreate, createdConnection);

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(lnsConnectionsOptionKey.getCategory(), optionRepresentationArgument.getCategory());
        assertEquals(lnsConnectionsOptionKey.getKey(), optionRepresentationArgument.getKey());


        VALID_LNS_CONNECTIONS_MAP.put(connectionToCreate.getName(), connectionToCreate);
        Map<String, LnsConnection> actualMapSaved = JSON_MAPPER.readerWithView(LnsConnection.InternalView.class)
                                                            .forType(mapType)
                                                            .readValue(optionRepresentationArgument.getValue());
        compare(VALID_LNS_CONNECTIONS_MAP, actualMapSaved);
    }

    @Test
    public void doCreate_null_connection() {
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.create(null));
        assertEquals("New LNS connection can't be null.", inputDataValidationException.getMessage());
    }

    @Test
    public void doCreate_invalid_connection() {
        LnsConnection invalidConnectionToCreate = SampleConnection.builder()
                                                .name("Sample Connection Name")
                                                .description("Sample Connection Description")
                                                .user(null)
                                                .password("**********")
                                                .build();

        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.create(invalidConnectionToCreate));
        assertEquals("SampleConnection is missing mandatory fields: 'user'", inputDataValidationException.getMessage());
    }

    @Test
    public void doCreate_duplicate_connection() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        LnsConnection duplicateConnectionToCreate = SampleConnection.builder()
                .name("SampleConnection-1")
                .description("Sample Connection Description")
                .user("USER NAME")
                .password("**********")
                .build();
        duplicateConnectionToCreate.setName("SampleConnection-1");
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.create(duplicateConnectionToCreate));
        assertEquals(String.format("LNS connection named '%s' already exists.", duplicateConnectionToCreate.getName()), inputDataValidationException.getMessage());
    }

    @Test
    public void doUpdate_with_valid_lnsConnection_with_new_password() throws Exception {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        LnsConnection connectionToUpdate = SampleConnection.builder()
                                        .name("SampleConnection-1")
                                        .description("Description for SampleConnection-1 (UPDATED)")
                                        .user("user-1 (UPDATED)")
                                        .password("password-1 (UPDATED)")
                                        .build();
        connectionToUpdate.setName("SampleConnection-1");
        LnsConnection updatedConnection = lnsConnectionService.update(connectionToUpdate.getName(), connectionToUpdate);

        compare(connectionToUpdate, updatedConnection);

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(lnsConnectionsOptionKey.getCategory(), optionRepresentationArgument.getCategory());
        assertEquals(lnsConnectionsOptionKey.getKey(), optionRepresentationArgument.getKey());


        VALID_LNS_CONNECTIONS_MAP.put(connectionToUpdate.getName(), connectionToUpdate);
        Map<String, LnsConnection> actualMapSaved = JSON_MAPPER.readerWithView(LnsConnection.InternalView.class)
                .forType(mapType)
                .readValue(optionRepresentationArgument.getValue());
        compare(VALID_LNS_CONNECTIONS_MAP, actualMapSaved);
    }

    @Test
    public void doUpdate_with_valid_lnsConnection_with_old_password() throws Exception {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        SampleConnection connectionToUpdate = SampleConnection.builder()
                .name("SampleConnection-1")
                .description("Description for SampleConnection-1 (UPDATED)")
                .user("user-1 (UPDATED)")
                .password(null) // Password is passed as null, so the old password is kept
                .build();
        connectionToUpdate.setName("SampleConnection-1");
        LnsConnection updatedConnection = lnsConnectionService.update(connectionToUpdate.getName(), connectionToUpdate);

        connectionToUpdate.setPassword(((SampleConnection)VALID_LNS_CONNECTIONS_MAP.get(connectionToUpdate.getName())).getPassword()); // Initialize the password with the existing connection's password
        compare(connectionToUpdate, updatedConnection);

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(lnsConnectionsOptionKey.getCategory(), optionRepresentationArgument.getCategory());
        assertEquals(lnsConnectionsOptionKey.getKey(), optionRepresentationArgument.getKey());


        VALID_LNS_CONNECTIONS_MAP.put(connectionToUpdate.getName(), connectionToUpdate);
        Map<String, LnsConnection> actualMapSaved = JSON_MAPPER.readerWithView(LnsConnection.InternalView.class)
                .forType(mapType)
                .readValue(optionRepresentationArgument.getValue());
        compare(VALID_LNS_CONNECTIONS_MAP, actualMapSaved);
    }

    @Test
    public void doUpdate_rename_lnsConnection() throws Exception {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        String existingLnsConnectionName = "SampleConnection-1";
        LnsConnection connectionToUpdate = SampleConnection.builder()
                .name("SampleConnection-1 (UPDATED)")
                .description("Description for SampleConnection-1 (UPDATED)")
                .user("user-1 (UPDATED)")
                .password("password-1 (UPDATED)")
                .build();
        mockInventoryReturnsWithDevice(null, null, false);
        LnsConnection updatedConnection = lnsConnectionService.update(existingLnsConnectionName, connectionToUpdate);

        compare(connectionToUpdate, updatedConnection);

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(lnsConnectionsOptionKey.getCategory(), optionRepresentationArgument.getCategory());
        assertEquals(lnsConnectionsOptionKey.getKey(), optionRepresentationArgument.getKey());


        VALID_LNS_CONNECTIONS_MAP.remove(existingLnsConnectionName.toLowerCase());
        VALID_LNS_CONNECTIONS_MAP.put(connectionToUpdate.getName(), connectionToUpdate);
        Map<String, LnsConnection> actualMapSaved = JSON_MAPPER.readerWithView(LnsConnection.InternalView.class)
                .forType(mapType)
                .readValue(optionRepresentationArgument.getValue());
        compare(VALID_LNS_CONNECTIONS_MAP, actualMapSaved);
        verify(applicationEventPublisher).publishEvent(any(LnsConnectionUpdateEvent.class));
    }

    @Test
    public void doUpdate_rename_lnsConnection_with_old_password() throws Exception {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        String existingLnsConnectionName = "SampleConnection-1";
        SampleConnection connectionToUpdate = SampleConnection.builder()
                .name("SampleConnection-1 (UPDATED)")
                .description("Description for SampleConnection-1 (UPDATED)")
                .user("user-1 (UPDATED)")
                .password(null) // Password is passed as null, so the old password is kept
                .build();
        mockInventoryReturnsWithDevice(null, null, false);
        LnsConnection updatedConnection = lnsConnectionService.update(existingLnsConnectionName, connectionToUpdate);

        connectionToUpdate.setPassword(((SampleConnection)VALID_LNS_CONNECTIONS_MAP.get(existingLnsConnectionName.toLowerCase())).getPassword()); // Initialize the password with the existing connection's password
        compare(connectionToUpdate, updatedConnection);

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(lnsConnectionsOptionKey.getCategory(), optionRepresentationArgument.getCategory());
        assertEquals(lnsConnectionsOptionKey.getKey(), optionRepresentationArgument.getKey());


        VALID_LNS_CONNECTIONS_MAP.remove(existingLnsConnectionName.toLowerCase());
        VALID_LNS_CONNECTIONS_MAP.put(connectionToUpdate.getName(), connectionToUpdate);
        Map<String, LnsConnection> actualMapSaved = JSON_MAPPER.readerWithView(LnsConnection.InternalView.class)
                .forType(mapType)
                .readValue(optionRepresentationArgument.getValue());
        compare(VALID_LNS_CONNECTIONS_MAP, actualMapSaved);
    }

    @Test
    public void ShouldThrowExceptionOnUpdateWhenDevicesAreAssociated() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        PlatformParameters platformParameters = new PlatformParameters();
        platformParameters.setHost("http://localhost:9090");
        when(restConnector.getPlatformParameters()).thenReturn(platformParameters);

        String existingLnsConnectionName = "SampleConnection-1";
        SampleConnection connectionToUpdate = SampleConnection.builder()
                .name("SampleConnection-1 (UPDATED)")
                .description("Description for SampleConnection-1 (UPDATED)")
                .user("user-1 (UPDATED)")
                .password(null) // Password is passed as null, so the old password is kept
                .build();
        connectionToUpdate.setName("SampleConnection-1 (UPDATED)");

        mockInventoryReturnsWithDevice(existingLnsConnectionName, new GId("12345"), true);
        try {
            lnsConnectionService.update(existingLnsConnectionName, connectionToUpdate);
        } catch (LpwanServiceException e) {
            String url = "/lns-connection/" + existingLnsConnectionName.toLowerCase() + "/device";
            String errorMessage = String.format("Can not update the LNS connection with name '%s' as it's associated with '%s' device(s).",
                    existingLnsConnectionName.toLowerCase(), 1);
            assertTrue(e.getMessage().contains(errorMessage));
            assertEquals(url, e.getUrl());
        }

        verify(tenantOptionApi,never()).save(optionRepresentationCaptor.capture());
    }

    @Test
    public void doUpdate_with_null_existingLnsConnectionName() {
        LnsConnection connectionToUpdate = SampleConnection.builder()
                                            .name("SampleConnection-1 (UPDATED)")
                                            .description("Description for SampleConnection-1 (UPDATED)")
                                            .user("user-1 (UPDATED)")
                                            .password(null)
                                            .build();
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.update(null, connectionToUpdate));
        assertEquals("Existing LNS connection name can't be null or blank.", inputDataValidationException.getMessage());
    }

    @Test
    public void doUpdate_with_blank_existingLnsConnectionName() {
        LnsConnection connectionToUpdate = SampleConnection.builder()
                                            .name("SampleConnection-1 (UPDATED)")
                                            .description("Description for SampleConnection-1 (UPDATED)")
                                            .user("user-1 (UPDATED)")
                                            .password(null)
                                            .build();
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.update("  ", connectionToUpdate));
        assertEquals("Existing LNS connection name can't be null or blank.", inputDataValidationException.getMessage());
    }

    @Test
    public void doUpdate_with_nonExisting_existingLnsConnectionName() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        String nonExistingConnectionNameToUpdate = "SampleConnection-5";
        LnsConnection connectionToUpdate = SampleConnection.builder()
                .name("SampleConnection-5 (UPDATED)")
                .description("Description for SampleConnection-5 (UPDATED)")
                .user("user-5 (UPDATED)")
                .password("password-5 (UPDATED)")
                .build();

        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.update(nonExistingConnectionNameToUpdate, connectionToUpdate));
        assertEquals(String.format("LNS connection named '%s' doesn't exist.", nonExistingConnectionNameToUpdate.toLowerCase()), inputDataValidationException.getMessage());
    }

    @Test
    public void doUpdate_with_null_lnsConnectionToUpdate() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        String existingConnectionNameToUpdate = "SampleConnection-1";

        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.update(existingConnectionNameToUpdate, null));
        assertEquals("LNS connection to update can't be null.", inputDataValidationException.getMessage());
    }

    @Test
    public void doUpdate_with_invalid_lnsConnectionToUpdate() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        String nonExistingConnectionNameToUpdate = "SampleConnection-1";
        LnsConnection invalidConnectionToUpdate = SampleConnection.builder()
                .name("SampleConnection-1 (UPDATED)")
                .description("Description for SampleConnection-1 (UPDATED)")
                .user(null) // Invalid as user is a mandatory field
                .password("password-5 (UPDATED)")
                .build();
        invalidConnectionToUpdate.setName("SampleConnection-1 (UPDATED)");
        mockInventoryReturnsWithDevice("SampleConnection-1 (UPDATED)", GId.asGId("12345"), false);
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.update(nonExistingConnectionNameToUpdate, invalidConnectionToUpdate));
        assertEquals("SampleConnection is missing mandatory fields: 'user'", inputDataValidationException.getMessage());
    }

    @Test
    public void doUpdate_with_updated_connection_name_already_present() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        String existingConnectionNameToUpdate = "SampleConnection-1";
        LnsConnection connectionToUpdate = SampleConnection.builder()
                .name("SampleConnection-2")
                .description("Description for SampleConnection-2 (UPDATED)")
                .user("user-2 (UPDATED)")
                .password("password-2 (UPDATED)")
                .build();
        connectionToUpdate.setName("SampleConnection-2"); // Already existing connection
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.update(existingConnectionNameToUpdate, connectionToUpdate));
        assertEquals(String.format("LNS connection named '%s' already exists.", connectionToUpdate.getName()), inputDataValidationException.getMessage());
    }

    @Test
    public void doDelete_with_existing_connectionName() throws LpwanServiceException, JsonProcessingException {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        String connectionNameToDelete = "SampleConnection-1";

        mockInventoryReturnsWithDevice(null, null, false);
        lnsConnectionService.delete(connectionNameToDelete);

        verify(tenantOptionApi).save(optionRepresentationCaptor.capture());
        OptionRepresentation optionRepresentationArgument = optionRepresentationCaptor.getValue();

        assertEquals(lnsConnectionsOptionKey.getCategory(), optionRepresentationArgument.getCategory());
        assertEquals(lnsConnectionsOptionKey.getKey(), optionRepresentationArgument.getKey());


        VALID_LNS_CONNECTIONS_MAP.remove(connectionNameToDelete.toLowerCase());
        Map<String, LnsConnection> actualMapSaved = JSON_MAPPER.readerWithView(LnsConnection.InternalView.class)
                                                    .forType(mapType)
                                                    .readValue(optionRepresentationArgument.getValue());
        compare(VALID_LNS_CONNECTIONS_MAP, actualMapSaved);
    }

    @Test
    public void ShouldThrowExceptionOnDeleteWhenDevicesAreAssociated() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        PlatformParameters platformParameters = new PlatformParameters();
        platformParameters.setHost("http://localhost:9090");
        when(restConnector.getPlatformParameters()).thenReturn(platformParameters);

        String connectionNameToDelete = "SampleConnection-1";

        mockInventoryReturnsWithDevice(connectionNameToDelete, new GId("12345"), true);
        try {
            lnsConnectionService.delete(connectionNameToDelete);
        } catch (LpwanServiceException e) {
            String url = "/lns-connection/" + connectionNameToDelete.toLowerCase() + "/device";
            String errorMessage = String.format("Can not delete the LNS connection with name '%s' as it's associated with '%s' device(s).",
                    connectionNameToDelete.toLowerCase(), 1);
            assertTrue(e.getMessage().contains(errorMessage));
            assertEquals(url, e.getUrl());
        }

        verify(tenantOptionApi,never()).save(optionRepresentationCaptor.capture());
    }

    @Test
    public void doDelete_with_null_connectionName() {
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.delete(null));
        assertEquals("LNS connection name to delete can't be null or blank.", inputDataValidationException.getMessage());
    }

    @Test
    public void doDelete_with_blank_connectionName() {
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.delete("  "));
        assertEquals("LNS connection name to delete can't be null or blank.", inputDataValidationException.getMessage());
    }

    @Test
    public void doDelete_with_nonExisting_connectionName() {
        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);

        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);
        when(tenantOptionApi.save(any())).thenReturn(null);

        String noExistingConnectionNameToDelete = "SampleConnection-5";
        InputDataValidationException inputDataValidationException = assertThrows(InputDataValidationException.class, () -> lnsConnectionService.delete(noExistingConnectionNameToDelete));
        assertEquals(String.format("LNS connection named '%s' doesn't exist.", noExistingConnectionNameToDelete.toLowerCase()), inputDataValidationException.getMessage());
    }

    @Test
    public void getDataForCsv() throws LpwanServiceException, IOException {
        mockInventoryReturnsWithDevice("dummyLnsConnection", new GId("12345"), true);
        InputStreamResource inputStreamResource = lnsConnectionService.getDeviceManagedObjectsInCsv("dummyLnsConnection");
        assertNotNull(inputStreamResource.getInputStream());
    }

    @Test
    public void shouldMigrateOldDevices() {
        String defaultConnection = "default connection";
        List<ManagedObjectRepresentation> moList = getManagedObjectRepresentations(null, new GId("12345"), true);
        when(lpwanCommonService.getDeviceMosByProvider("Sigfox")).thenReturn(moList);
        lnsConnectionService.migrateOldDevices("Sigfox", defaultConnection);
        ArgumentCaptor<ManagedObjectRepresentation> argumentCaptor = ArgumentCaptor.forClass(ManagedObjectRepresentation.class);
        verify(inventoryApi).update(argumentCaptor.capture());
        ManagedObjectRepresentation updatedDeviceMo = argumentCaptor.getValue();
        LpwanDevice lpwanDevice = updatedDeviceMo.get(LpwanDevice.class);
        assertEquals(defaultConnection, lpwanDevice.getLnsConnectionName());
    }

    @Test
    public void shouldNotMigrateWhenNoDevicesAreAvailable() {
        String defaultConnection = "default connection";
        mockInventoryReturnsWithDevice(null, new GId("12345"), false);
        lnsConnectionService.migrateOldDevices("sigfox", defaultConnection);
        ArgumentCaptor<ManagedObjectRepresentation> argumentCaptor = ArgumentCaptor.forClass(ManagedObjectRepresentation.class);
        verify(inventoryApi, never()).update(argumentCaptor.capture());
    }

    @Test
    public void shouldCheckHealthStatus(){
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(LnsConnectionService.class);
        listAppender.start();
        logger.addAppender(listAppender);

        MicroserviceCredentials microserviceCredentials = new MicroserviceCredentials("aTenant", "aServiceUser", "aPassword", null, null, null, null);
        Collection<MicroserviceCredentials> currentSubscriptions = Lists.newArrayList(microserviceCredentials);
        when(subscriptionsService.getCredentials("aTenant")).thenReturn(Optional.of(microserviceCredentials));
        when(contextService.getContext()).thenReturn(microserviceCredentials);

        OptionPK lnsConnectionsOptionKey = new OptionPK("sample", "credentials.lns.connections.map");
        OptionRepresentation lnsConnectionsOptionRepresentation = OptionRepresentation.asOptionRepresentation(lnsConnectionsOptionKey.getCategory(), lnsConnectionsOptionKey.getKey(), VALID_LNS_CONNECTIONS_MAP_JSON);
        when(tenantOptionApi.getOption(eq(lnsConnectionsOptionKey))).thenReturn(lnsConnectionsOptionRepresentation);

        GId source = GId.asGId("12345");
        when(lpwanRepository.findOrCreateSource()).thenReturn(source);

        ((ThreadPoolTaskScheduler) taskScheduler).initialize();

        lnsConnectionService.checkHealth();

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(contextService, timeout(5000L)).runWithinContext(eq(microserviceCredentials),runnableArgumentCaptor.capture());
        runnableArgumentCaptor.getValue().run();
        verify(lpwanRepository).clearAlarm(eq(source),eq(ALARM_TYPE));
        InOrder inOrder = inOrder(taskScheduler);
        inOrder.verify(taskScheduler, timeout(5000L)).schedule(any(LnsConnectionService.ScheduledHealthCheckTaskExecutor.class), instantArgumentCaptor.capture());
        Assert.assertTrue(instantArgumentCaptor.getValue().getEpochSecond() <= Instant.now().getEpochSecond());
        inOrder.verify(taskScheduler, timeout(5000L)).schedule(any(LnsConnectionService.ScheduledHealthCheckTaskExecutor.class), instantArgumentCaptor.capture());
        Assert.assertTrue(instantArgumentCaptor.getValue().getEpochSecond() <= Instant.now().plus(Duration.ofHours(1)).getEpochSecond());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        String infoMessage = String.format("All the connections in the tenant '%s' are up", microserviceCredentials.getTenant());
        assertEquals(infoMessage, logsList.get(1).getFormattedMessage());
    }


    private void compare(Map<String, LnsConnection> expected, Collection<LnsConnection> actual) {
        assertEquals(expected.size(), actual.size());
        Map<String, LnsConnection> actualMap = new ConcurrentHashMap<>(actual.size());

        for (LnsConnection oneLnsConnection: actual) {
            actualMap.put(oneLnsConnection.getName(), oneLnsConnection);
        }

        compare(expected, actualMap);
    }

    private void compare(Map<String, LnsConnection> expected, Map<String, LnsConnection> actual) {
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.size(), actual.size());

        for (Map.Entry<String, LnsConnection> oneExpectedEntry : expected.entrySet()) {
            assertTrue(actual.containsKey(oneExpectedEntry.getKey()));
            compare(oneExpectedEntry.getValue(), actual.get(oneExpectedEntry.getKey()));
        }
    }

    private void compare(LnsConnection expected, LnsConnection actual) {
        assertNotNull(actual);

        assertTrue(expected.getClass().isAssignableFrom(actual.getClass()));
        assertEquals(actual.getClass(), SampleConnection.class);

        SampleConnection expectedTestLnsConnection = (SampleConnection) expected;
        SampleConnection actualTestLnsConnection = (SampleConnection) actual;

        assertEquals(expectedTestLnsConnection.getName().toLowerCase(), actualTestLnsConnection.getName());
        assertEquals(expectedTestLnsConnection.getDescription(), actualTestLnsConnection.getDescription());
        assertEquals(expectedTestLnsConnection.getUser(), actualTestLnsConnection.getUser());
        assertEquals(expectedTestLnsConnection.getPassword(), actualTestLnsConnection.getPassword());
    }

    private void mockInventoryReturnsWithDevice(String lnsConnectionName, GId gId, boolean isDeviceAssociated) {
        List<ManagedObjectRepresentation> moList = getManagedObjectRepresentations(lnsConnectionName, gId, isDeviceAssociated);

        ManagedObjectCollection managedObjectCollection = mock(ManagedObjectCollection.class);
        PagedManagedObjectCollectionRepresentation paged = mock(PagedManagedObjectCollectionRepresentation.class);
        when(managedObjectCollection.get()).thenReturn(paged);
        when(paged.allPages()).thenReturn(moList);

        when(inventoryApi.getManagedObjectsByFilter(any(InventoryFilter.class))).
                thenReturn(managedObjectCollection);
    }

    private static List<ManagedObjectRepresentation> getManagedObjectRepresentations(String lnsConnectionName, GId gId, boolean isDeviceAssociated) {
        List<ManagedObjectRepresentation> moList = new ArrayList<>();
        if(isDeviceAssociated) {
            ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();
            managedObject.setName("Dummy_LPWAN_Device");
            managedObject.setType("type");
            LpwanDevice lpwanDevice = new LpwanDevice();
            lpwanDevice.setLnsConnectionName(lnsConnectionName);
            managedObject.set(lpwanDevice);
            managedObject.setId(gId);
            moList.add(managedObject);
        }
        return moList;
    }
}
package com.cumulocity.lpwan.util;

import c8y.LpwanDevice;
import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.smaple.connection.model.SampleConnection;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import com.cumulocity.sdk.client.inventory.ManagedObjectCollection;
import com.cumulocity.sdk.client.inventory.PagedManagedObjectCollectionRepresentation;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.reactivestreams.Publisher;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import c8y.Agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@RunWith(SpringJUnit4ClassRunner.class)
public class LpwanCommonServiceTest {

    @Mock
    ContextService<Credentials> contextService;

    @Mock
    PlatformProperties platformProperties;

    @Mock
    ApplicationApi applicationApi;

    @Mock
    InventoryApi inventoryApi;

    @InjectMocks
    LpwanCommonService lpwanCommonService;

    @Test
    public void shouldMigrateOldDeviceWithNewAgentFragment() {
        mockInventoryReturnsWithDevice("lns-connection-1", GId.asGId("12345"));
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("SampleConnection", SampleConnection.class);
        lpwanCommonService.migrateOldDeviceWithNewAgentFragment();
        ArgumentCaptor<ManagedObjectRepresentation> deviceMoCaptor =  ArgumentCaptor.forClass(ManagedObjectRepresentation.class);
        verify(inventoryApi).update(deviceMoCaptor.capture());
        ManagedObjectRepresentation deviceMoToBeUpdated = deviceMoCaptor.getValue();
        assertEquals(GId.asGId("12345"), deviceMoToBeUpdated.getId());
        Agent agent = deviceMoToBeUpdated.get(Agent.class);
        assertEquals(LpwanCommonService.MAINTAINER, agent.getMaintainer());
        assertEquals("SampleConnection", agent.getName());
    }

    @Test
    public void shouldPrepareAgentFragment() throws Exception {
        MicroserviceCredentials microserviceCredentials = new MicroserviceCredentials("aTenant", "aServiceUser", "aPassword", null, null, null, null);
        when(platformProperties.getMicroserviceBoostrapUser()).thenReturn(microserviceCredentials);
        ArgumentCaptor<Callable> callableArgumentCaptor = ArgumentCaptor.forClass(Callable.class);
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("SampleConnection", SampleConnection.class);
        Agent agent = lpwanCommonService.prepareAgentFragment();
        verify(contextService).callWithinContext(eq(microserviceCredentials),callableArgumentCaptor.capture());
        callableArgumentCaptor.getValue().call();
        verify(applicationApi).currentApplication();
        assertEquals(LpwanCommonService.MAINTAINER, agent.getMaintainer());
        assertEquals("SampleConnection", agent.getName());
    }

    private void mockInventoryReturnsWithDevice(String lnsConnectionName, GId gId) {
        List<ManagedObjectRepresentation> moList = new ArrayList<>();
        ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();
        managedObject.setName("Dummy_LPWAN_Device");
        managedObject.setType("type");
        LpwanDevice lpwanDevice = new LpwanDevice();
        lpwanDevice.setLnsConnectionName(lnsConnectionName);
        managedObject.set(lpwanDevice);
        managedObject.setId(gId);
        moList.add(managedObject);

        ManagedObjectCollection managedObjectCollection = mock(ManagedObjectCollection.class);
        PagedManagedObjectCollectionRepresentation paged = mock(PagedManagedObjectCollectionRepresentation.class);
        when(managedObjectCollection.get()).thenReturn(paged);
        when(paged.allPages()).thenReturn(moList);

        when(inventoryApi.getManagedObjectsByFilter(any(InventoryFilter.class))).
                thenReturn(managedObjectCollection);
    }
}
package com.cumulocity.lpwan.util;

import c8y.Agent;
import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.lns.connection.model.LpwanDeviceFilter;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Callable;

@Component
@Slf4j
public class LpwanCommonService {

    public static final String MAINTAINER = "Cumulocity GmbH";

    @Autowired
    InventoryApi inventoryApi;

    @Autowired
    ApplicationApi applicationApi;

    @Autowired
    ContextService<Credentials> contextService;

    @Autowired
    PlatformProperties platformProperties;

    public void migrateOldDeviceWithNewAgentFragment(String version) {
        Iterable<ManagedObjectRepresentation> managedObjectRepresentations = getDeviceMosByProvider(LnsConnectionDeserializer.getRegisteredAgentName());
        if (managedObjectRepresentations == null) return;

        for (ManagedObjectRepresentation deviceMo : managedObjectRepresentations) {
            Agent agentFromMo = deviceMo.get(Agent.class);
            if (Objects.isNull(agentFromMo)) {
                ManagedObjectRepresentation deviceMoToBeUpdated = ManagedObjects.asManagedObject(deviceMo.getId());
                deviceMoToBeUpdated.set(prepareAgentFragment(version));
                try {
                    inventoryApi.update(deviceMoToBeUpdated);
                    log.info("The device with the Managed object Id '{}' is updated with fragment 'c8y_Agent'", deviceMo.getId());
                } catch (SDKException e) {
                    String message = String.format("Error in updating the device with managed object id '%s", deviceMoToBeUpdated.getId());
                    log.error(message, e);
                }
            }
        }
    }

    public void migrateOldDeviceWithNewAgentFragment() {
        migrateOldDeviceWithNewAgentFragment(getVersion());
    }

    public Iterable<ManagedObjectRepresentation> getDeviceMosByProvider(String lpwanProviderName) {
        InventoryFilter inventoryFilter = LpwanDeviceFilter.byServiceProvider(lpwanProviderName);
        Iterable<ManagedObjectRepresentation> managedObjectRepresentations = null;
        try {
            managedObjectRepresentations = inventoryApi.getManagedObjectsByFilter(inventoryFilter).get().allPages();
        } catch (SDKException e) {
            String message = String.format("Error in getting device managed objects from the service provider '%s'", lpwanProviderName);
            log.error(message, e);
            return null;
        }
        return managedObjectRepresentations;
    }

    public Agent prepareAgentFragment(String version) {
        Agent agent = new Agent();
        agent.setName(LnsConnectionDeserializer.getRegisteredAgentName());
        agent.setVersion(version);
        agent.setMaintainer(MAINTAINER);

        return agent;
    }

    public Agent prepareAgentFragment() {
        return prepareAgentFragment(getVersion());
    }

    public String getVersion() {
        return contextService.callWithinContext(platformProperties.getMicroserviceBoostrapUser(), new Callable<String>() {
            @Override
            public String call() {
                String version = null;
                try {
                    ApplicationRepresentation representation = applicationApi.currentApplication().get();
                    version = representation.getManifest().get("version").toString();
                    log.info("Agent version : {}", version);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

                return version;
            }
        });
    }
}

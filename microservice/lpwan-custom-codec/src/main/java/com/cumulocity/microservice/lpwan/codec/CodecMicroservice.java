/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.lpwan.codec.model.DeviceCommand;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;
import com.cumulocity.microservice.lpwan.codec.model.LpwanCodecDetails;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.model.ID;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.sdk.client.ParamSource;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import com.cumulocity.sdk.client.inventory.ManagedObjectCollection;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The <b>CodecMicroservice</b> class mainly registers a device type when the codec microservice is subscribed.
 */
@Slf4j
@ComponentScan
public class CodecMicroservice {

    static final String DEVICE_TYPE_DESCRIPTION_FORMAT = "Device protocol that supports device model '%s' manufactured by '%s'";
    static final String C8Y_SMART_REST_DEVICE_IDENTIFIER = "c8y_SmartRestDeviceIdentifier";
    static final String C8Y_LPWAN_CODEC_DETAILS = "c8y_LpwanCodecDetails";
    static final String FIELDBUS_TYPE = "fieldbusType";
    static final String DESCRIPTION = "description";
    static final String C8Y_IS_DEVICE_TYPE = "c8y_IsDeviceType";
    static final String C8Y_LPWAN_DEVICE_TYPE = "c8y_LpwanDeviceType";
    static final String LPWAN_FIELDBUS_TYPE = "lpwan";
    static final String DEVICE_TYPE_NAME_FORMAT = "%s : %s";
    static final String C8Y_DEVICE_SHELL_TEMPLATE = "c8y_DeviceShellTemplate";
    static final String DEVICE_TYPE = "deviceType";
    static final String COMMAND = "command";
    static final String CATEGORY = "category";
    static final String DELIVERY_TYPES = "deliveryTypes";

    @Autowired
    private ContextService<Credentials> contextService;

    @Autowired
    private PlatformProperties platformProperties;

    @Autowired
    private ApplicationApi applicationApi;

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private IdentityApi identityApi;

    @Autowired
    private Codec codec;

    private volatile String contextPath;

    /**
     * This method should create a device type and the pre-defined command templates for the supported commands when the codec microservice subscribed.
     *
     * @param event the microservice subscription notification.
     */
    @EventListener
    void registerDeviceTypes(MicroserviceSubscriptionAddedEvent event) {
        if (Objects.isNull(contextPath)) {
            contextPath =
                    contextService.callWithinContext(platformProperties.getMicroserviceBoostrapUser(),
                            () -> {
                                try {
                                    return applicationApi.currentApplication().get().getContextPath();
                                } catch (Exception e) {
                                    log.warn("Error while determining the microservice context path. Defaulting to the application name.", e);
                                    return platformProperties.getApplicationName();
                                }
                            });
        }

        contextService.runWithinContext(event.getCredentials(),
                () -> {
                    Set<DeviceInfo> supportedDevices = codec.supportsDevices();
                    for (DeviceInfo supportedDevice : supportedDevices) {
                        ManagedObjectRepresentation deviceTypeMo = registerDeviceType(supportedDevice);

                        if(Objects.nonNull(deviceTypeMo)) {
                            createPredefinedCommandTemplates(deviceTypeMo, supportedDevice.getSupportedCommands());
                        }
                    }
                });
    }

    private ManagedObjectRepresentation registerDeviceType(DeviceInfo deviceInfo) {
        try {
            deviceInfo.validate();
        } catch (IllegalArgumentException e) {
            log.error("Device manufacturer and model are mandatory fields in the supported device. Skipping the Device Type creation.", e);
            return null;
        }

        String supportedDeviceTypeName = formDeviceTypeName(deviceInfo);
        LpwanCodecDetails lpwanCodecDetails = new LpwanCodecDetails(contextPath, deviceInfo);
        Optional<ExternalIDRepresentation> deviceType = isDeviceTypeExists(deviceInfo);
        ManagedObjectRepresentation deviceTypeMo = null;
        if (!deviceType.isPresent()) {
            log.info("Creating device type '{}' on codec microservice subscription", supportedDeviceTypeName);

            String description = String.format(DEVICE_TYPE_DESCRIPTION_FORMAT, deviceInfo.getDeviceModel(), deviceInfo.getDeviceManufacturer());

            deviceTypeMo = new ManagedObjectRepresentation();
            deviceTypeMo.setName(supportedDeviceTypeName);
            deviceTypeMo.set(description, DESCRIPTION);
            deviceTypeMo.set(Collections.EMPTY_MAP, C8Y_IS_DEVICE_TYPE);
            deviceTypeMo.setType(C8Y_LPWAN_DEVICE_TYPE);
            deviceTypeMo.set(LPWAN_FIELDBUS_TYPE, FIELDBUS_TYPE);
            deviceTypeMo.set(lpwanCodecDetails.getAttributes(), C8Y_LPWAN_CODEC_DETAILS);
            try {
                deviceTypeMo = inventoryApi.create(deviceTypeMo);

                //Register the External Id
                ExternalIDRepresentation deviceTypeExternalId = new ExternalIDRepresentation();
                deviceTypeExternalId.setExternalId(supportedDeviceTypeName);
                deviceTypeExternalId.setType(C8Y_SMART_REST_DEVICE_IDENTIFIER);
                deviceTypeExternalId.setManagedObject(deviceTypeMo);
                try {
                    identityApi.create(deviceTypeExternalId);
                } catch (Exception e) {
                    log.error("Unable create External Id for device type with name '{}'", supportedDeviceTypeName, e);
                    return null;
                }
            } catch (Exception e) {
                log.error("Unable create device type with name '{}'", supportedDeviceTypeName, e);
                return null;
            }

            log.info("Created device type '{}' on codec microservice subscription", supportedDeviceTypeName);
        } else {
            log.info("Updating the device type with name '{}' as it already exists", supportedDeviceTypeName);

            deviceTypeMo = ManagedObjects.asManagedObject(deviceType.get().getManagedObject().getId());
            deviceTypeMo.set(lpwanCodecDetails.getAttributes(), C8Y_LPWAN_CODEC_DETAILS);
            try {
                deviceTypeMo = inventoryApi.update(deviceTypeMo);
            } catch (Exception e) {
                log.error("Unable update device type with name '{}'", supportedDeviceTypeName, e);
                return null;
            }

            log.info("Updated the device type with name '{}' as it already exists", supportedDeviceTypeName);
        }

        return deviceTypeMo;
    }

    private Optional<ExternalIDRepresentation> isDeviceTypeExists(DeviceInfo deviceInfo) {
        try {
            return Optional.of(identityApi.getExternalId(new ID(C8Y_SMART_REST_DEVICE_IDENTIFIER, formDeviceTypeName(deviceInfo))));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * This method forms the device type name.
     *
     * @param deviceInfo carries the device manufacturer name and the device model.
     * @return string device type name
     */
    String formDeviceTypeName(DeviceInfo deviceInfo) {
        return String.format(DEVICE_TYPE_NAME_FORMAT, deviceInfo.getDeviceManufacturer(), deviceInfo.getDeviceModel());
    }

    private void createPredefinedCommandTemplates(ManagedObjectRepresentation deviceTypeMo, Set<DeviceCommand> supportedCommands) {
        try {
            log.info("Creating predefined command templates for the device type '{}' on codec microservice subscription", deviceTypeMo.getName());
            // Fetch all the pre-existing autogenerated commands registered for this deviceType
            InventoryFilter inventoryFilter = PredefinedCommandFilter.of("c8y_DeviceShellTemplate", "deviceType", deviceTypeMo.getName());
            ManagedObjectCollection managedObjectCollection = inventoryApi.getManagedObjectsByFilter(inventoryFilter);
            Iterable<ManagedObjectRepresentation> allPredefinedCommands = null;
            if(Objects.nonNull(managedObjectCollection)) {
                allPredefinedCommands = managedObjectCollection.get().allPages();
            }

            // Delete all the pre-existing autogenerated commands
            if(Objects.nonNull(allPredefinedCommands)) {
                for (ManagedObjectRepresentation predefinedCommand : allPredefinedCommands) {
                    inventoryApi.delete(predefinedCommand.getId());
                    log.debug("Deleted existing predefined command template '{}' for the device type '{}'", predefinedCommand.getName(), deviceTypeMo.getName());
                }
            }

            if(Objects.nonNull(supportedCommands)) {
                // For every supported operation, create a predefined operation template
                for (DeviceCommand oneSupportedCommand : supportedCommands) {
                    ManagedObjectRepresentation predefinedCommand = new ManagedObjectRepresentation();
                    predefinedCommand.setName(oneSupportedCommand.getName());
                    predefinedCommand.setType(C8Y_DEVICE_SHELL_TEMPLATE);
                    predefinedCommand.set(new String[]{deviceTypeMo.getName()}, DEVICE_TYPE);
                    predefinedCommand.set(oneSupportedCommand.getCommand(), COMMAND);
                    predefinedCommand.set(oneSupportedCommand.getCategory(), CATEGORY);
                    predefinedCommand.set(new String[] { "Default" }, DELIVERY_TYPES);

                    try {
                        inventoryApi.create(predefinedCommand);
                        log.info("Created predefined command template '{}' for device type '{}'", predefinedCommand.getName(), deviceTypeMo.getName());
                    } catch (Exception e) {
                        // This exception is caught to only log the error.
                        log.error("Failed to create the predefined command named '{}', for the device type '{}'", oneSupportedCommand.getName(), deviceTypeMo.getName());
                    }
                }
            }
        } catch (Exception e) {
            // This exception is caught to only log the error.
            log.error("Failed to create the predefined commands for the device type '{}'", deviceTypeMo.getName());
        }
    }

//    /**
//     * This method should delete a device type upon unsubscribing the codec microservice.
//     *
//     * @param event
//     */
//    @EventListener
//    void unregisterDeviceTypes(MicroserviceSubscriptionRemovedEvent event) {
//        log.info("Deleting device types as codec microservice is unsubscribed.");
//        Set<DeviceInfo> supportedDevices = supportsDevices();
//        Optional<MicroserviceCredentials> credentials = subscriptions.getCredentials(event.getTenant());
//        for (DeviceInfo supportedDevice : supportedDevices) {
//            contextService.runWithinContext(credentials.get(),
//                    () -> {
//                        String deviceTypeName = supportedDevice.getDeviceTypeName();
//                        try {
//                            GId deviceTypeId = identityApi.getExternalId(new ID(C8Y_SMART_REST_DEVICE_IDENTIFIER, deviceTypeName)).getManagedObject().getId();
//                            inventoryApi.delete(deviceTypeId);
//                        } catch (Exception e) {
//                            log.error("Unable to delete device type with name '{}'", deviceTypeName, e);
//                        }
//                    });
//        }
//    }
}

@EqualsAndHashCode(callSuper = false)
class PredefinedCommandFilter extends InventoryFilter {

    @ParamSource
    private String query;

    private PredefinedCommandFilter(String type, String fragmentName, String fragmentValue) {
        query = "$filter=" +
                String.format("'%s' eq '%s'", "type", type) +
                String.format(" and has('%s')", fragmentName) +
                String.format(" and '%s' eq '%s'", fragmentName, fragmentValue);
    }

    public static PredefinedCommandFilter of(String type, String fragmentName, String fragmentValue) {
        return new PredefinedCommandFilter(type, fragmentName, fragmentValue);
    }
}

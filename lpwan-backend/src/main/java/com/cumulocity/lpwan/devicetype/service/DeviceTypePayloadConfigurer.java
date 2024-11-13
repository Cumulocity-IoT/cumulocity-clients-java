/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.devicetype.service;

import c8y.LpwanDevice;
import c8y.TypeExternalId;
import com.cumulocity.lpwan.devicetype.model.DeviceType;
import com.cumulocity.lpwan.payload.exception.DeviceTypeObjectNotFoundException;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DeviceTypePayloadConfigurer {

    static final String C8Y_SMART_REST_DEVICE_IDENTIFIER = "c8y_SmartRestDeviceIdentifier";

    protected final IdentityApi identityApi;

    protected final InventoryApi inventoryApi;

    protected final DeviceTypeMapper deviceTypeMapper;

    private LoadingCache<ImmutablePair<String, GId>, Optional<DeviceType>> deviceTypes = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<ImmutablePair<String, GId>, Optional<DeviceType>>() {
                        @Override
                        public Optional<DeviceType> load(ImmutablePair<String, GId> tenantAnddeviceTypePair) throws Exception {
                            ManagedObjectRepresentation deviceTypeObject = inventoryApi.get(tenantAnddeviceTypePair.getValue());
                            if (deviceTypeObject == null) {
                                return Optional.absent();
                            }
                            return Optional.fromNullable(deviceTypeMapper.convertManagedObjectToDeviceType(deviceTypeObject));
                        }
                    }
            );

    public DeviceTypePayloadConfigurer(IdentityApi identityApi, InventoryApi inventoryApi, DeviceTypeMapper deviceTypeMapper) {
        this.identityApi = identityApi;
        this.inventoryApi = inventoryApi;
        this.deviceTypeMapper = deviceTypeMapper;
    }

    public DeviceType getDeviceTypeObject(String tenantId, ManagedObjectRepresentation device) throws DeviceTypeObjectNotFoundException {
        try {
            LpwanDevice lpwanDevice = device.get(LpwanDevice.class);
            if (lpwanDevice == null) {
                throw new DeviceTypeObjectNotFoundException("LpwanDevice fragment is missing for device " + device.getId());
            } else if (StringUtils.isBlank(lpwanDevice.getType())) {
                throw new DeviceTypeObjectNotFoundException("LpwanDevice.type field is missing for device " + device.getId());
            } else {
                GId deviceTypeId = LpwanDeviceTypeUtil.getTypeId(lpwanDevice);

                if (Objects.nonNull(lpwanDevice.getTypeExternalId()) && Objects.nonNull(lpwanDevice.getTypeExternalId().getType()) && Objects.nonNull(lpwanDevice.getTypeExternalId().getExternalId())) {
                    ExternalIDRepresentation externalIDRepresentation = identityApi.getExternalId(new ID(lpwanDevice.getTypeExternalId().getType(), lpwanDevice.getTypeExternalId().getExternalId()));
                    if (Objects.nonNull(externalIDRepresentation)) {
                        deviceTypeId = externalIDRepresentation.getManagedObject().getId();

                        // Updating the device with the device type MO Id.
                        if(!deviceTypeId.getValue().equals(LpwanDeviceTypeUtil.getTypeId(lpwanDevice).getValue())){
                            ManagedObjectRepresentation deviceMoToUpdate = ManagedObjects.asManagedObject(device.getId());
                            LpwanDeviceTypeUtil.setTypePath(lpwanDevice, deviceTypeId.getValue());
                            deviceMoToUpdate.set(lpwanDevice);
                            inventoryApi.update(deviceMoToUpdate);
                            log.info("Updated the device fragment 'c8y_LpwanDevice' having the Managed object ID '{}' with the new device type Managed object ID '{}'", device.getId().getValue(), deviceTypeId.getValue());
                        }
                    }
                } else {
                    // Updating the devices without the typeExternalId fragment.
                    Optional<DeviceType> deviceType = deviceTypes.get(new ImmutablePair<>(tenantId, deviceTypeId));
                    if (deviceType.isPresent()) {
                        TypeExternalId typeExternalId = new TypeExternalId();
                        typeExternalId.setExternalId(deviceType.get().getName());
                        typeExternalId.setType(C8Y_SMART_REST_DEVICE_IDENTIFIER);
                        lpwanDevice.setTypeExternalId(typeExternalId);

                        ManagedObjectRepresentation deviceMoToUpdate = ManagedObjects.asManagedObject(device.getId());
                        deviceMoToUpdate.set(lpwanDevice);
                        inventoryApi.update(deviceMoToUpdate);
                        log.info("Updated the device fragment 'c8y_LpwanDevice' having the Managed object ID '{}' with the external id information.", device.getId().getValue());
                    }
                }

                // Along with updating the inventory we're also updating the in memory cached device.
                device.set(lpwanDevice);

                if (Objects.isNull(deviceTypeId)) {
                    throw new DeviceTypeObjectNotFoundException("Device type id could not be parsed" + lpwanDevice.getType());
                }
                ImmutablePair<String, GId> tenantAndDeviceTypePair = new ImmutablePair<>(tenantId, deviceTypeId);
                Optional<DeviceType> deviceType = deviceTypes.get(tenantAndDeviceTypePair);
                if (deviceType.isPresent()) {
                    return deviceType.get();
                } else {
                    throw new DeviceTypeObjectNotFoundException("Device type managed object not found for " + deviceTypeId);
                }
            }
        } catch (ExecutionException e) {
            log.error(Throwables.getStackTraceAsString(e));
            throw new DeviceTypeObjectNotFoundException("Error retrieving device type");
        }
    }

    public void refreshDeviceTypeCache(String tenantId, LpwanDevice lpwanDevice) {
        GId deviceTypeId = LpwanDeviceTypeUtil.getTypeId(lpwanDevice);
        ImmutablePair<String, GId> tenantAndDeviceTypePair = new ImmutablePair<>(tenantId, deviceTypeId);
        deviceTypes.refresh(tenantAndDeviceTypePair);
    }
}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.*;

/**
 * The <b>DeviceInfo</b> class uniquely represents one device with the device manufacturer name, the device model.
 *
 */
@NoArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DeviceInfo {
    public static final String DEVICE_MANUFACTURER = "deviceManufacturer";
    public static final String DEVICE_MODEL = "deviceModel";

    static final String SUPPORTED_DEVICE_COMMANDS = "supportedDeviceCommands";

    @NotBlank
    @EqualsAndHashCode.Include
    private String deviceManufacturer;

    @NotBlank
    @EqualsAndHashCode.Include
    private String deviceModel;

    @Null
    @JsonProperty("supportedDeviceCommands")
    private Set<DeviceCommand> supportedCommands;

    /**
     * Instantiates a new DeviceInfo.
     * @param deviceManufacturer represents the name of the device manufacturer.
     * @param deviceModel        represents the name of the device model.
     */
    public DeviceInfo(@NotBlank String deviceManufacturer, @NotBlank String deviceModel) {
        this(deviceManufacturer, deviceModel, null);
    }

    public DeviceInfo(@NotBlank String deviceManufacturer, @NotBlank String deviceModel, @Null Set<DeviceCommand> supportedCommands) {
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
        this.supportedCommands = supportedCommands;
    }

    public DeviceInfo(@NotNull Map<String, String> properties) {
        this(properties.get(DEVICE_MANUFACTURER), properties.get(DEVICE_MODEL));
    }

    /**
     * This method returns the manufacturer, model and the supported commands for a device
     *
     * @return Map The resultant map consisting of the attributes of the device
     */
    public Map<String, Object> getAttributes() {
        Map<String,Object> attributes = new HashMap<>(3);
        attributes.put(DEVICE_MANUFACTURER, deviceManufacturer);
        attributes.put(DEVICE_MODEL, deviceModel);
        if(Objects.nonNull(supportedCommands)) {
            List<Map<String, Object>> supportedCommandsAttributesList = new ArrayList<>();
            attributes.put(SUPPORTED_DEVICE_COMMANDS, supportedCommandsAttributesList);
            for(DeviceCommand supportedCommand: supportedCommands) {
                supportedCommandsAttributesList.add(supportedCommand.getAttributes());
            }
        }
        return attributes;
    }
    /**
     * This method validates the object field.
     *
     * @throws IllegalArgumentException if the field marked with <b>@NotBlank</b> are either null or blank.
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/IllegalArgumentException.html">IllegalArgumentException</a>
     */
    public void validate() {
        List<String> missingParameters = new ArrayList<>(3);

        if (Strings.isNullOrEmpty(deviceManufacturer)) {
            missingParameters.add("'manufacturer'");
        }

        if (Strings.isNullOrEmpty(deviceModel)) {
            missingParameters.add("'model'");
        }

        if(Objects.nonNull(supportedCommands) && !supportedCommands.isEmpty()) {
            for(DeviceCommand supportedCommand: supportedCommands) {
                try {
                    supportedCommand.validate();
                } catch (IllegalArgumentException e) {
                    missingParameters.add("'name, category and/or command'");
                }
            }
        }

        if(!missingParameters.isEmpty()) {
            throw new IllegalArgumentException("DeviceInfo is missing mandatory parameters: " + String.join(", ", missingParameters));
        }
    }
}

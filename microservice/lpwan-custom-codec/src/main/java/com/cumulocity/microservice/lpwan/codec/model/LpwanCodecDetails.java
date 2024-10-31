/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec.model;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;

/**
 * The <b>LpwanCodecDetails</b> class represents the fragment details that is added in the device type managed object which is created on the codec microservice subscription.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LpwanCodecDetails {

    static final String CODEC_SERVICE_CONTEXT_PATH = "codecServiceContextPath";
    static final String SUPPORTED_DEVICE = "supportedDevice";

    @NotBlank
    private String codecServiceContextPath;

    @NotNull
    private DeviceInfo supportedDevice;

    /**
     * This method returns information about the <b>deviceManufacturer</b>, <b>deviceModel</b> and <b>codecServiceContextPath</b> that are added to the <b>c8y_LpwanCodecDetails</b> fragment.
     *
     * @return Map the attributes
     */
    public Map<String, Object> getAttributes() {
        Map<String,Object> attributes = new HashMap<>(3);
        attributes.put(CODEC_SERVICE_CONTEXT_PATH, codecServiceContextPath);
        attributes.put(SUPPORTED_DEVICE, supportedDevice.getAttributes());

        return attributes;
    }

    /**
     * This method validates the object fields.
     *
     * @throws IllegalArgumentException if the field marked with <b>@NotBlank</b> are either null or blank.
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/IllegalArgumentException.html">IllegalArgumentException</a>
     */
    public void validate() {
        List<String> missingParameters = new ArrayList<>(2);

        if (Strings.isNullOrEmpty(codecServiceContextPath)) {
            missingParameters.add("'codecServiceContextPath'");
        }

        if(Objects.isNull(supportedDevice) ) {
            missingParameters.add("'supportedDevices'");
        } else {
            try {
                supportedDevice.validate();
            } catch (IllegalArgumentException e) {
                missingParameters.add("'manufacturer, model and/or supportedCommands'");
            }
        }

        if(!missingParameters.isEmpty()) {
            throw new IllegalArgumentException("LpwanCodecDetails is missing mandatory parameters: " + String.join(", ", missingParameters));
        }
    }
}

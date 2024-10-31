/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec.model;

import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.*;


@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class DeviceCommand {
    static final String NAME = "name";
    static final String CATEGORY = "category";

    @NotBlank
    @EqualsAndHashCode.Include
    private String name;

    @NotBlank
    private String category;

    @Null
    private String command;

    /**
     * Instantiates a new DeviceCommand
     *
     * @param name represents the name of the device command
     * @param category represents the category of the device command
     * @param command represents the command text of the device command
     */
    public DeviceCommand(@NotBlank String name, @NotBlank String category, @Null String command) {
        this.name = name;
        this.category = category;
        this.command = command;
    }

    /**
     * This method returns the name, category, command and the delivery types associated with the Device Command
     *
     * @return Map The resultant map containing the attributes associated with the Device Command
     */
    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(NAME, name);
        attributes.put(CATEGORY, category);
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

        if (Strings.isNullOrEmpty(name)) {
            missingParameters.add("'name'");
        }

        if (Strings.isNullOrEmpty(category)) {
            missingParameters.add("'category'");
        }

        if(!missingParameters.isEmpty()) {
            throw new IllegalArgumentException("DeviceCommand is missing mandatory parameters: " + String.join(", ", missingParameters));
        }
    }
}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec.encoder.model;

import com.cumulocity.microservice.customencoders.api.model.EncoderResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.*;

@NoArgsConstructor
public class LpwanEncoderResult extends EncoderResult {
    public static final String FPORT_KEY = "fport";

    public LpwanEncoderResult(@NotBlank String encodedCommand,
                                 @Null Integer fport) {

        setEncodedCommand(encodedCommand);
        setFport(fport);
        validate();
    }

    /**
     * Gets f-port.
     *
     * @return the f-port
     */
    @JsonIgnore
    public @Null Integer getFport() {
        String fportstring = getPropertiesMap().get(FPORT_KEY);
        if (!Strings.isNullOrEmpty(fportstring)) {
            return Integer.valueOf(fportstring);
        }

        return null;
    }

    private void setFport(@Null Integer fport) {
        if (Objects.nonNull(fport)) {
            getPropertiesMap().put(FPORT_KEY, fport.toString());
        }
    }

    private @NotNull Map<String, String> getPropertiesMap() {
        Map<String, String> properties = super.getProperties();
        if (Objects.isNull(properties)) {
            properties = new HashMap<>();
            super.setProperties(properties);
        }

        return properties;
    }

    /**
     * This method validates the object fields.
     *
     * @throws IllegalArgumentException if the field marked with <b>@NotNull</b> or <b>@NotBlank</b> are either null or blank.
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/IllegalArgumentException.html">IllegalArgumentException</a>
     */
    private void validate() {
        List<String> missingParameters = new ArrayList<>();

        if (Strings.isNullOrEmpty(getEncodedCommand())) {
            missingParameters.add("'encodedCommand'");
        }

        if (!missingParameters.isEmpty()) {
            throw new IllegalArgumentException("LpwanEncoderResult is missing mandatory fields: " + String.join(", ", missingParameters));
        }
    }
}

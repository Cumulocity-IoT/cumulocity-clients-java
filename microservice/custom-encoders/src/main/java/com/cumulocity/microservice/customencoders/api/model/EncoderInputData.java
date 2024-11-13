/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customencoders.api.model;

import com.cumulocity.rest.representation.BaseResourceRepresentation;
import lombok.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;


@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class EncoderInputData extends BaseResourceRepresentation implements Serializable {

    private String commandName;

    private String commandData;

    private String sourceDeviceId;

    private Map<String, String> args;

    private ExecutionEventStatus status = ExecutionEventStatus.PENDING;

    public static EncoderInputData fromMap(Map<String, Object> map) {
        EncoderInputData data = new EncoderInputData();
        data.setArgs((Map<String, String>) map.get("args"));
        String status = (String) map.get("status");
        if (status != null) {
            data.setStatus(ExecutionEventStatus.valueOf(status));
        }
        return data;
    }
}

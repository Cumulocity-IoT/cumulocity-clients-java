/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customdecoders.api.model;

import com.cumulocity.rest.representation.BaseResourceRepresentation;
import lombok.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class DecoderInputData extends BaseResourceRepresentation implements Serializable {
    private String serviceKey;

    private String value;

    private Map<String, String> args;

    private String sourceDeviceId;

    private ExecutionEventStatus status = ExecutionEventStatus.PENDING;

    public static DecoderInputData fromMap(Map<String, Object> map) {
        DecoderInputData data = new DecoderInputData();
        data.setServiceKey((String) map.get("serviceKey"));
        data.setValue((String) map.get("value"));
        data.setArgs((Map<String, String>) map.get("args"));
        String status = (String) map.get("status");
        if (status != null) {
            data.setStatus(ExecutionEventStatus.valueOf(status));
        }
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecoderInputData that = (DecoderInputData) o;
        return Objects.equals(serviceKey, that.serviceKey) &&
                Objects.equals(value, that.value) &&
                Objects.equals(args, that.args) &&
                Objects.equals(sourceDeviceId, that.sourceDeviceId) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceKey, value, args, sourceDeviceId, status);
    }
}

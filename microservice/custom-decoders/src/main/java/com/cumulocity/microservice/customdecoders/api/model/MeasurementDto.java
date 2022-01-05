/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2020-2022 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.cumulocity.microservice.customdecoders.api.model;

import com.cumulocity.model.DateTimeConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.time.DateTime;
import org.svenson.JSONTypeHint;
import org.svenson.converter.JSONConverter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class MeasurementDto implements Serializable {
    private String type;
    private String series;
    private DateTime time;
    private String[] fragmentsToCopyFromSourceDevice;
    private String deviceFragmentPrefix;
    private String deviceNameFragment;
    private boolean includeDeviceName;
    private Map<String, String> additionalProperties = new HashMap<>();
    private List<MeasurementValueDto> values = new ArrayList<>();

    @JSONTypeHint(MeasurementValueDto.class)
    public List<MeasurementValueDto> getValues() {
        return values;
    }

    @JSONConverter(type = DateTimeConverter.class)
    public DateTime getTime() {
        return time;
    }
}

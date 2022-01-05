/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2020-2022 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */


package com.cumulocity.lpwan.devicetype.model;

import com.cumulocity.lpwan.payload.uplink.model.MessageIdConfiguration;
import com.cumulocity.microservice.lpwan.codec.model.LpwanCodecDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
public class DeviceType {
    
    private String type;
    private String name;

    @Singular
    @JsonProperty("c8y_Registers")
    private List<UplinkConfiguration> uplinkConfigurations;
    
    @JsonProperty("c8y_MessageIdConfiguration")
    private MessageIdConfiguration messageIdConfiguration;
    
    @JsonProperty("c8y_MessageTypes")
    private MessageTypes messageTypes;

    @JsonProperty("c8y_LpwanCodecDetails")
    private LpwanCodecDetails lpwanCodecDetails;

    @JsonProperty("fieldbusType")
    private String fieldbusType;
}

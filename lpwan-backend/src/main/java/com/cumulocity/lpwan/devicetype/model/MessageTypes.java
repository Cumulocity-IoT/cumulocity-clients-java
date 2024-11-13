/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.devicetype.model;

import java.util.Map;

import com.cumulocity.lpwan.mapping.model.MessageTypeMapping;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageTypes {

    Map<String, MessageTypeMapping> messageTypeMappings;
    
    public MessageTypeMapping getMappingIndexesByMessageType(String messageTypeId) {
        return messageTypeMappings.get(messageTypeId);
    }
}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.lns.connection.model;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class LnsConnectionDeserializer extends StdDeserializer<LnsConnection> {

    private static Class<? extends LnsConnection> lnsConnectionClass;
    private static String agentName;

    protected LnsConnectionDeserializer() {
        super(LnsConnection.class);
    }

    public static void registerLnsConnectionConcreteClass(String agentName, Class<? extends LnsConnection> lnsConnectionClass) {
        com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer.agentName = agentName;
        com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer.lnsConnectionClass = lnsConnectionClass;
    }

    public static String getRegisteredAgentName() {
        return agentName;
    }

    /**
     * Unlike the Jackson 2 {@code jsonParser.getCodec().treeToValue(...)} this replaced, the bind
     * below runs on the calling context and therefore honours an active {@code @JsonView}, where
     * the codec always used the mapper's view-less default config. No SDK path is affected - the
     * only view-filtered read is against InternalView, which covers every view - and Spring reads
     * a request body view-filtered only when {@code @JsonView} sits on the {@code @RequestBody}
     * parameter itself, which no SDK controller does.
     */
    @Override
    public LnsConnection deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        return deserializationContext.readTreeAsValue(deserializationContext.readTree(jsonParser), lnsConnectionClass);
    }
}

/*
 * Copyright (c) 2026 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.lns.connection.service;

import com.cumulocity.lpwan.lns.connection.model.LnsConnection;
import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.ValueDeserializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The LNS connection map is persisted as a tenant option through a view-filtered writer and read
 * back through a view-filtered reader. Jackson 3 flipped MapperFeature.DEFAULT_VIEW_INCLUSION to
 * false and JsonMapper.builderWithJackson2Defaults() does not restore it, so without that feature
 * an implementation's property that carries no @JsonView is dropped on write - permanently wiping
 * it from the tenant option - and read back as null.
 */
class LnsConnectionViewInclusionTest {

    /** Implementations are told to annotate every property, but nothing enforces it. */
    @JsonDeserialize(using = ValueDeserializer.None.class)
    public static class PartiallyAnnotatedConnection extends LnsConnection {

        @JsonView(LnsConnection.InternalView.class)
        private String password;

        private String unannotated;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUnannotated() {
            return unannotated;
        }

        public void setUnannotated(String unannotated) {
            this.unannotated = unannotated;
        }

        @Override
        protected void validate() {
        }

        @Override
        protected void update(LnsConnection lnsConnectionWithNewData) {
        }

        @Override
        public boolean checkConnectionStatus() {
            return true;
        }
    }

    private ObjectMapper jsonMapper;

    @BeforeEach
    void setUp() {
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("sample", PartiallyAnnotatedConnection.class);
        // the very mapper the service persists the tenant option with
        jsonMapper = (ObjectMapper) ReflectionTestUtils.getField(LnsConnectionService.class, "JSON_MAPPER");
    }

    @Test
    void everyPropertyOfAnImplementationSurvivesTheViewFilteredRoundTrip() {
        PartiallyAnnotatedConnection connection = new PartiallyAnnotatedConnection();
        connection.setName("connection name");
        connection.setDescription("a description");
        connection.setPassword("s3cr3t");
        connection.setUnannotated("must not be lost");

        Map<String, LnsConnection> connections = new ConcurrentHashMap<>();
        connections.put(connection.getName(), connection);

        String persisted = jsonMapper
                .writerWithView(LnsConnection.InternalView.class)
                .writeValueAsString(connections);

        assertThat(persisted).contains("must not be lost");

        MapType mapType = jsonMapper.getTypeFactory()
                .constructMapType(ConcurrentHashMap.class, String.class, LnsConnection.class);
        Map<String, LnsConnection> restored = jsonMapper
                .readerWithView(LnsConnection.InternalView.class)
                .forType(mapType)
                .readValue(persisted);

        PartiallyAnnotatedConnection actual =
                (PartiallyAnnotatedConnection) restored.get(connection.getName());
        assertThat(actual.getName()).isEqualTo("connection name");
        assertThat(actual.getDescription()).isEqualTo("a description");
        assertThat(actual.getPassword()).isEqualTo("s3cr3t");
        assertThat(actual.getUnannotated()).isEqualTo("must not be lost");
    }
}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.lns.connection.model;

import com.cumulocity.lpwan.exception.InputDataValidationException;
import com.cumulocity.lpwan.sample.connection.model.SampleConnection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonExpectationsHelper;

import static com.cumulocity.lpwan.sample.connection.model.SampleConnection.*;
import static org.junit.jupiter.api.Assertions.*;


class LnsConnectionTest {

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    @Test
    void doValidateLnsConnection_valid_description_and_password_null() throws InputDataValidationException {
        assertTrue(VALID_SAMPLE_CONNECTION.isValid());
    }

    @Test
    void doValidateLnsConnection_valid_name_special_characters() throws InputDataValidationException {
        assertTrue(VALID_SAMPLE_CONNECTION_SPECIAL_CHARACTERS_1.isValid());
        assertTrue(VALID_SAMPLE_CONNECTION_SPECIAL_CHARACTERS_2.isValid());
    }

    @Test
    void doValidateLnsConnection_invalid_name_null() {
        LnsConnection invalid_connection = SampleConnection.builder()
                .name(null)
                .description(null)
                .user("USER NAME")
                .password("**********")
                .build();

        InputDataValidationException inputDataValidationException = Assert.assertThrows(InputDataValidationException.class, invalid_connection::isValid);
        assertEquals("SampleConnection is missing mandatory fields: 'name'", inputDataValidationException.getMessage());
    }

    @Test
    void doValidateLnsConnection_invalid_name_blank() {
        LnsConnection invalid_connection = SampleConnection.builder()
                                            .name("   ")
                                            .description(null)
                                            .user("USER NAME")
                                            .password("**********")
                                            .build();

        InputDataValidationException inputDataValidationException = Assert.assertThrows(InputDataValidationException.class, invalid_connection::isValid);
        assertEquals("SampleConnection is missing mandatory fields: 'name'", inputDataValidationException.getMessage());
    }

    @Test
    void doValidateLnsConnection_invalid_name_special_characters() {
        LnsConnection invalid_connection = SampleConnection.builder()
                .name("null null !@#$%^*::&'\"")
                .description(null)
                .user("USER NAME")
                .password("**********")
                .build();

        InputDataValidationException inputDataValidationException = Assert.assertThrows(InputDataValidationException.class, invalid_connection::isValid);
        assertEquals("SampleConnection has restricted special characters %, ;, /, &, #, ?, \", ', \\, *", inputDataValidationException.getMessage());

        LnsConnection invalid_connection_2 = SampleConnection.builder()
                .name("a!#c&d~^(76*5$) '")
                .description(null)
                .user("USER NAME")
                .password("**********")
                .build();

        InputDataValidationException inputDataValidationException_2 = Assert.assertThrows(InputDataValidationException.class, invalid_connection_2::isValid);
        assertEquals("SampleConnection has restricted special characters %, ;, /, &, #, ?, \", ', \\, *", inputDataValidationException_2.getMessage());

        LnsConnection invalid_connection_3 = SampleConnection.builder()
                .name("!@#rtw")
                .description(null)
                .user("USER NAME")
                .password("**********")
                .build();

        InputDataValidationException inputDataValidationException_3 = Assert.assertThrows(InputDataValidationException.class, invalid_connection_3::isValid);
        assertEquals("SampleConnection has restricted special characters %, ;, /, &, #, ?, \", ', \\, *", inputDataValidationException_3.getMessage());

        LnsConnection invalid_connection_4 = SampleConnection.builder()
                .name("rtw#$")
                .description(null)
                .user("USER NAME")
                .password("**********")
                .build();

        InputDataValidationException inputDataValidationException_4 = Assert.assertThrows(InputDataValidationException.class, invalid_connection_4::isValid);
        assertEquals("SampleConnection has restricted special characters %, ;, /, &, #, ?, \", ', \\, *", inputDataValidationException_4.getMessage());
    }

    @Test
    void doValidateLnsConnection_invalid_user_null() {
        LnsConnection invalid_connection = SampleConnection.builder()
                                                .name("Sample Connection Name")
                                                .description("Sample Connection Description")
                                                .user(null)
                                                .password("**********")
                                                .build();

        InputDataValidationException inputDataValidationException = Assert.assertThrows(InputDataValidationException.class, invalid_connection::isValid);
        assertEquals("SampleConnection is missing mandatory fields: 'user'", inputDataValidationException.getMessage());
    }

    @Test
    void doValidateLnsConnection_invalid_user_blank() {
        LnsConnection invalid_connection = SampleConnection.builder()
                                            .name("Sample Connection Name")
                                            .description("Sample Connection Description")
                                            .user("    ")
                                            .password("**********")
                                            .build();

        InputDataValidationException inputDataValidationException = Assert.assertThrows(InputDataValidationException.class, invalid_connection::isValid);
        assertEquals("SampleConnection is missing mandatory fields: 'user'", inputDataValidationException.getMessage());
    }

    @Test
    void doUpdateLnsConnection_with_non_null_password() {
        SampleConnection existingLnsConnection = SampleConnection.builder()
                                                    .name("Sample Connection Name")
                                                    .description("Sample Connection Description")
                                                    .user("USER NAME")
                                                    .password("**********")
                                                    .build();

        SampleConnection lnsConnectionWithUpdatedContents = SampleConnection.builder()
                                                                .name("Sample Connection Name (NEW)")
                                                                .description("Sample Connection Description (NEW)")
                                                                .user("USER NAME (NEW)")
                                                                .password("********** (NEW)")
                                                                .build();

        existingLnsConnection.initializeWith(lnsConnectionWithUpdatedContents);

        compare(lnsConnectionWithUpdatedContents, existingLnsConnection);
    }

    @Test
    void doUpdateLnsConnection_with_null_password() {
        SampleConnection existingLnsConnection = SampleConnection.builder()
                                                    .name("Sample Connection Name")
                                                    .description("Sample Connection Description")
                                                    .user("USER NAME")
                                                    .password("**********")
                                                    .build();

        SampleConnection lnsConnectionWithUpdatedContents = SampleConnection.builder()
                                                                .name("Sample Connection Name (NEW)")
                                                                .description("Sample Connection Description (NEW)")
                                                                .user("USER NAME (NEW)")
                                                                .password(null)
                                                                .build();

        existingLnsConnection.initializeWith(lnsConnectionWithUpdatedContents);

        compare(SampleConnection.builder()
                        .name(lnsConnectionWithUpdatedContents.getName())
                        .description(lnsConnectionWithUpdatedContents.getDescription())
                        .user(lnsConnectionWithUpdatedContents.getUser())
                        .password(existingLnsConnection.getPassword())
                        .build()
                , existingLnsConnection);
    }

    @Test
    void doUpdateLnsConnection_with_blank_password() {
        SampleConnection existingLnsConnection = SampleConnection.builder()
                                                        .name("Sample Connection Name")
                                                        .description("Sample Connection Description")
                                                        .user("USER NAME")
                                                        .password("**********")
                                                        .build();

        SampleConnection lnsConnectionWithUpdatedContents = SampleConnection.builder()
                                                                .name("Sample Connection Name (NEW)")
                                                                .description("Sample Connection Description (NEW)")
                                                                .user("USER NAME (NEW)")
                                                                .password("   ")
                                                                .build();

        existingLnsConnection.initializeWith(lnsConnectionWithUpdatedContents);


        compare(SampleConnection.builder()
                        .name(lnsConnectionWithUpdatedContents.getName())
                        .description(lnsConnectionWithUpdatedContents.getDescription())
                        .user(lnsConnectionWithUpdatedContents.getUser())
                        .password(existingLnsConnection.getPassword())
                        .build()
                , existingLnsConnection);
    }

    @Test
    void doDeSerializeLnsConnection_with_valueType_as_LnsConnection_insteadof_SampleConnection() throws JsonProcessingException {
        com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer.registerLnsConnectionConcreteClass("sample", SampleConnection.class);

        LnsConnection lnsConnection = jsonObjectMapper.readValue(VALID_SAMPLE_CONNECTION_JSON_INTERNAL_VIEW, LnsConnection.class);

        compare(VALID_SAMPLE_CONNECTION, lnsConnection);
    }

    @Test
    void doSerializeLnsConnection_with_PublicView_must_EXCLUDE_Password() throws Exception {
        String sampleConnectionSerializedWithPublicView = jsonObjectMapper.writerWithView(LnsConnection.PublicView.class).writeValueAsString(VALID_SAMPLE_CONNECTION);

        new JsonExpectationsHelper().assertJsonEqual(VALID_SAMPLE_CONNECTION_JSON_PUBLIC_VIEW, sampleConnectionSerializedWithPublicView, true);
    }

    @Test
    void doSerializeLnsConnection_with_InternalView_must_INCLUDE_Password() throws Exception {
        String sampleConnectionSerializedWithPublicView = jsonObjectMapper.writerWithView(LnsConnection.InternalView.class).writeValueAsString(VALID_SAMPLE_CONNECTION);

        new JsonExpectationsHelper().assertJsonEqual(VALID_SAMPLE_CONNECTION_JSON_INTERNAL_VIEW, sampleConnectionSerializedWithPublicView, true);
    }

    private void compare(SampleConnection expected, LnsConnection actual) {
        assertTrue(expected.getClass().isAssignableFrom(actual.getClass()));

        SampleConnection actualTestLnsConnection = (SampleConnection) actual;

        assertEquals(expected.getName().toLowerCase(), actualTestLnsConnection.getName());
        assertEquals(expected.getDescription(), actualTestLnsConnection.getDescription());
        assertEquals(expected.getUser(), actualTestLnsConnection.getUser());
        assertEquals(expected.getPassword(), actualTestLnsConnection.getPassword());
    }
}
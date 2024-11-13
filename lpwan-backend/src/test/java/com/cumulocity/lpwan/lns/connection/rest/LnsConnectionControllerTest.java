/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.lns.connection.rest;

import com.cumulocity.lpwan.lns.connection.model.LnsConnection;
import com.cumulocity.lpwan.lns.connection.model.LnsConnectionDeserializer;
import com.cumulocity.lpwan.lns.connection.service.LnsConnectionService;
import com.cumulocity.lpwan.sample.connection.model.SampleConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.cumulocity.lpwan.sample.connection.model.SampleConnection.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LnsConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableWebMvc
public class LnsConnectionControllerTest {
    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LnsConnectionService lnsConnectionService;

    @Captor
    private ArgumentCaptor<LnsConnection> lnsConnectionArgumentCaptor;


    @Before
    public void setup() {
        LnsConnectionDeserializer.registerLnsConnectionConcreteClass("sample", SampleConnection.class);
    }

    @Test
    public void doCall_getAll_and_ensure_only_publicView_is_returned_to_client() throws Exception {
        SampleConnection testLnsConnection_1 = SampleConnection.builder()
                .name("TEST-LNS-1")
                .description("Test LNS 1")
                .user("user-1")
                .password("password!!!")
                .build();

        SampleConnection testLnsConnection_2 = SampleConnection.builder()
                .name("TEST-LNS-1")
                .description("Test LNS 1")
                .user("user-1")
                .password("password!!!")
                .build();

        List<LnsConnection> lnsConnections = Arrays.asList(new LnsConnection[] {testLnsConnection_1, testLnsConnection_2});
        when(lnsConnectionService.getAll()).thenReturn(lnsConnections);

        this.mockMvc.perform(get("/lns-connection"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(jsonObjectMapper.writerWithView(LnsConnection.PublicView.class).writeValueAsString(lnsConnections), true));

        verify(lnsConnectionService).getAll();
    }

    @Test
    public void doCall_getAll_return_empty_list() throws Exception {
        List<LnsConnection> emptyLnsConnectionList = Collections.emptyList();
        when(lnsConnectionService.getAll()).thenReturn(emptyLnsConnectionList);

        this.mockMvc.perform(get("/lns-connection"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));

        verify(lnsConnectionService).getAll();
    }

    @Test
    public void doCall_getByName_and_ensure_only_publicView_is_returned_to_client() throws Exception {
        when(lnsConnectionService.getByName(eq(VALID_SAMPLE_CONNECTION.getName()))).thenReturn(VALID_SAMPLE_CONNECTION);

        this.mockMvc.perform(get("/lns-connection/" + VALID_SAMPLE_CONNECTION.getName()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(VALID_SAMPLE_CONNECTION_JSON_PUBLIC_VIEW, true));

        verify(lnsConnectionService).getByName(eq(VALID_SAMPLE_CONNECTION.getName()));
    }

    @Test
    public void doCall_create_and_ensure_only_publicView_is_returned_to_client() throws Exception {
        when(lnsConnectionService.create(any())).thenReturn(VALID_SAMPLE_CONNECTION);

        this.mockMvc.perform(post("/lns-connection")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(VALID_SAMPLE_CONNECTION_JSON_INTERNAL_VIEW))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().json(VALID_SAMPLE_CONNECTION_JSON_PUBLIC_VIEW, true));

        verify(lnsConnectionService).create(lnsConnectionArgumentCaptor.capture());
        LnsConnection lnsConnectionCapturedArgument = lnsConnectionArgumentCaptor.getValue();
        compare(VALID_SAMPLE_CONNECTION, lnsConnectionCapturedArgument);
    }

    @Test
    public void doCall_update_and_ensure_only_publicView_is_returned_to_client() throws Exception {
        String existingLnsConnectionName = "EXISTING-TEST-LNS-1";
        when(lnsConnectionService.update(eq(existingLnsConnectionName), any())).thenReturn(VALID_SAMPLE_CONNECTION);

        this.mockMvc.perform(put("/lns-connection/" + existingLnsConnectionName)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(VALID_SAMPLE_CONNECTION_JSON_INTERNAL_VIEW))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(VALID_SAMPLE_CONNECTION_JSON_PUBLIC_VIEW, true));

        verify(lnsConnectionService).update(eq(existingLnsConnectionName), lnsConnectionArgumentCaptor.capture());
        LnsConnection lnsConnectionCapturedArgument = lnsConnectionArgumentCaptor.getValue();
        compare(VALID_SAMPLE_CONNECTION, lnsConnectionCapturedArgument);
    }

    @Test
    public void doCall_delete() throws Exception {
        String lnsConnectionNameToDelete = "DELETE-TEST-LNS-1";

        this.mockMvc.perform(delete("/lns-connection/" + lnsConnectionNameToDelete))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(lnsConnectionService).delete(eq(lnsConnectionNameToDelete));
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
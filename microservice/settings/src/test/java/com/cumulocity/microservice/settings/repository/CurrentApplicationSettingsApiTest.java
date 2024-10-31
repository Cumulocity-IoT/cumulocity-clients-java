package com.cumulocity.microservice.settings.repository;


import com.cumulocity.rest.representation.tenant.OptionsRepresentation;
import com.cumulocity.sdk.client.RestOperations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.MediaType;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CurrentApplicationSettingsApiTest {

    @Mock
    private RestOperations rest;

    private CurrentApplicationSettingsApi settingsApi;

    @BeforeEach
    public void setup() {
        settingsApi = new CurrentApplicationSettingsApi(rest, () -> "http://not.real.url");
        settingsApi = spy(settingsApi);

        when(settingsApi.getBackoffRate()).thenReturn(0);
    }

    @Test
    public void retryWhenErrorOccurredOnCallingSettingsApi() {
        //Given
        errorOnGettingSettings();

        //When
        Assertions.assertThrows(RuntimeException.class, () -> {
            settingsApi.findAll();
        });

        //Then
        verify(rest, times(3)).get(anyString(), eq(MediaType.APPLICATION_JSON_TYPE), eq(OptionsRepresentation.class));
    }

    private void errorOnGettingSettings() {
        when(rest.get(anyString(), eq(MediaType.APPLICATION_JSON_TYPE), eq(OptionsRepresentation.class))).thenThrow(new RuntimeException("Error calling settings api"));
    }
}

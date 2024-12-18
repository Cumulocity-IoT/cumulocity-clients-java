package com.cumulocity.sdk.client.event;

import com.cumulocity.rest.representation.event.EventCollectionRepresentation;
import com.cumulocity.rest.representation.event.EventsApiRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class EventBinaryApiTest {
    private static final String EVENT_API_URL = "http://localhost/event/events";
    private static final String EVENTS_BINARY_URL = "http://localhost/event/events/{id}/binaries";

    @Mock
    private RestConnector restConnector;

    EventsApiRepresentation eventsApiRepresentation = new EventsApiRepresentation();
    EventBinaryApi eventBinaryApi;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        EventCollectionRepresentation eventCollectionRepresentation = new EventCollectionRepresentation();
        eventCollectionRepresentation.setSelf(EVENT_API_URL);
        eventsApiRepresentation.setEvents(eventCollectionRepresentation);

        eventBinaryApi = new EventBinaryApiImpl(restConnector, eventsApiRepresentation);
    }

    @Test
    public void shouldGetEventBinaryFile() {
        //Given
        String eventId = "10";
        InputStream inputStream = IOUtils.toInputStream("hello", StandardCharsets.UTF_8);
        String eventBinaryUrl = EVENTS_BINARY_URL.replace("{id}", eventId);
        when(restConnector.get(eventBinaryUrl, MediaType.APPLICATION_OCTET_STREAM_TYPE, InputStream.class)).thenReturn(inputStream);

        //When
        InputStream eventBinary = eventBinaryApi.getEventBinary(eventId);

        //Then
        assertThat(eventBinary, sameInstance(inputStream));
    }

    @Test
    public void shouldThrowExceptionOnGetFileIfBinaryIDIsNull() throws SDKException {
        // When
        Throwable thrown = catchThrowable(() -> eventBinaryApi.getEventBinary(null));

        // Then
        assertThat(thrown, is(instanceOf(NullPointerException.class)));
    }

    @Test
    public void shouldThrowSDKExceptionIfAnyIssueWhileRestCallOnGetFile() throws SDKException {
        //Given
        String eventId = "11";
        String eventBinaryUrl = EVENTS_BINARY_URL.replace("{id}", eventId);
        when(restConnector.get(eventBinaryUrl, MediaType.APPLICATION_OCTET_STREAM_TYPE, InputStream.class)).thenThrow(SDKException.class);

        //When
        Throwable thrown = catchThrowable(() ->  eventBinaryApi.getEventBinary(eventId));

        //Then
        assertThat(thrown, is(instanceOf(SDKException.class)));
    }

    @Test
    public void shouldDeleteEventBinary() {
        //Given
        String eventId = "12";
        String eventBinaryUrl = EVENTS_BINARY_URL.replace("{id}", eventId);

        //When
        eventBinaryApi.deleteEventBinary(eventId);

        //Then
        verify(restConnector, times(1)).delete(eventBinaryUrl);
    }

    @Test
    public void shouldThrowExceptionOnDeleteFileIfBinaryIDIsNull() throws SDKException {
        // When
        Throwable thrown = catchThrowable(() -> eventBinaryApi.deleteEventBinary(null));

        // Then
        assertThat(thrown, is(instanceOf(NullPointerException.class)));

    }

    @Test
    public void shouldCreateFileAsByteArray() throws SDKException {
        // Given
        byte[] binaryData = {1,2,4,6,6};
        String eventId = "13";
        String eventBinaryUrl = EVENTS_BINARY_URL.replace("{id}", eventId);

        // When
        eventBinaryApi.createEventBinary(eventId, binaryData);

        // Then
        verify(restConnector, times(1)).postFile(eventBinaryUrl, binaryData, MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Test
    public void shouldThrowExceptionOnPostFileIfBinaryIDIsNull() throws SDKException {
        // Given
        byte[] binaryData = {1,2,4,6,6};

        // When
        Throwable thrown = catchThrowable(() -> eventBinaryApi.createEventBinary(null, binaryData));

        // Then
        assertThat(thrown, is(instanceOf(NullPointerException.class)));

    }

    @Test
    public void shouldUpdateFileAsByteArray() throws SDKException {
        // Given
        byte[] binaryData = {1,2,4,6,6};
        String eventId = "14";
        String eventBinaryUrl = EVENTS_BINARY_URL.replace("{id}", eventId);

        // When
        eventBinaryApi.updateEventBinary(eventId, binaryData);

        // Then
        verify(restConnector, times(1)).putFile(eventBinaryUrl, binaryData, MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Test
    public void shouldThrowExceptionOnPutFileIfBinaryIDIsNull() throws SDKException {
        // Given
        byte[] binaryData = {1,2,4,6,6};

        // When
        Throwable thrown = catchThrowable(() -> eventBinaryApi.updateEventBinary(null, binaryData));

        // Then
        assertThat(thrown, is(instanceOf(NullPointerException.class)));

    }
}

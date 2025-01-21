package com.cumulocity.sdk.client.event;

import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EventBinaryApiIT extends JavaSdkITBase {
   final EventBinaryApi eventBinaryApi = platform.getEventBinaryApi();
   final EventApi eventApi = platform.getEventApi();

    @Test
    public void shouldDownloadUploadedFile() throws IOException {
        // given
        byte[] binaryData = "event upload binary".getBytes(StandardCharsets.UTF_8);
        ManagedObjectRepresentation managedObject = createManagedObject();
        String eventId = createEvent(managedObject);

        // when
        eventBinaryApi.createEventBinary(eventId, binaryData);
        InputStream downloaded = eventBinaryApi.getEventBinary(eventId);

        // then
        assertThat(downloaded.readAllBytes()).isEqualTo(binaryData);
    }

    @Test
    public void shouldUpdateBinaryFile() throws IOException {
        // given
        byte[] text = "event binary text".getBytes(StandardCharsets.UTF_8);
        byte[] updatedText = "updated event binary text".getBytes(StandardCharsets.UTF_8);
        ManagedObjectRepresentation managedObject = createManagedObject();
        String eventId = createEvent(managedObject);
        eventBinaryApi.createEventBinary(eventId, text);

        // when
        eventBinaryApi.updateEventBinary(eventId, updatedText);
        InputStream downloaded = eventBinaryApi.getEventBinary(eventId);

        // then
        assertThat(downloaded.readAllBytes()).isEqualTo(updatedText);
    }

    @Test
    public void shouldDeleteBinaryFile() {
        // given
        byte[] text = "event binary text".getBytes(StandardCharsets.UTF_8);
        ManagedObjectRepresentation managedObject = createManagedObject();
        String eventId = createEvent(managedObject);
        eventBinaryApi.createEventBinary(eventId, text);
        eventBinaryApi.deleteEventBinary(eventId);

        // when
        SDKException sdkException = assertThrows(SDKException.class, () -> eventBinaryApi.getEventBinary(eventId));

        // then
        assertThat(sdkException.getHttpStatus()).isEqualTo(404);
    }

    @Test
    public void shouldNotDownloadNonExistingFile() {
        // given
        String id = "Non-existing";

        // when
        SDKException sdkException = assertThrows(SDKException.class, () -> eventBinaryApi.getEventBinary(id));

        // then
        assertThat(sdkException.getHttpStatus()).isEqualTo(404);
    }

    @Test
    public void shouldNotDeletedNonExistingFile() {
        // given
        String id = "Non-existing";

        // when
        SDKException sdkException = assertThrows(SDKException.class, () -> eventBinaryApi.deleteEventBinary(id));

        // then
        assertThat(sdkException.getHttpStatus()).isEqualTo(404);
    }

    private String createEvent(ManagedObjectRepresentation managementObject) {
        EventRepresentation eventRepresentation = new EventRepresentation();
        eventRepresentation.setType("event");
        eventRepresentation.setDateTime(new DateTime());
        eventRepresentation.setText("Event binary uploaded");
        eventRepresentation.setSource(managementObject);
        EventRepresentation createdEvent = eventApi.create(eventRepresentation);
        return createdEvent.getId().getValue();
    }

    private ManagedObjectRepresentation createManagedObject() {
        ManagedObjectRepresentation managedObjectRepresentation = new ManagedObjectRepresentation();
        managedObjectRepresentation.setName("MO for event");
        return platform.getInventoryApi().create(managedObjectRepresentation);
    }
}

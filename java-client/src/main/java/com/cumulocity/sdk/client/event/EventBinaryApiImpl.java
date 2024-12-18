package com.cumulocity.sdk.client.event;

import com.cumulocity.rest.representation.event.EventsApiRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import jakarta.ws.rs.core.MediaType;

import java.io.InputStream;

public class EventBinaryApiImpl implements EventBinaryApi {

    private final RestConnector restConnector;
    private final EventsApiRepresentation eventsApiRepresentation;

    public EventBinaryApiImpl(RestConnector restConnector, EventsApiRepresentation eventsApiRepresentation) {
        this.restConnector = restConnector;
        this.eventsApiRepresentation = eventsApiRepresentation;
    }

    @Override
    public InputStream getEventBinary(String eventId) throws SDKException {
        return restConnector.get(getEventBinaryUrl(eventId), MediaType.APPLICATION_OCTET_STREAM_TYPE, InputStream.class);
    }

    @Override
    public void createEventBinary(String id, byte[] bytes) throws SDKException {
        restConnector.postFile(getEventBinaryUrl(id), bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Override
    public void updateEventBinary(String id, byte[] bytes) throws SDKException {
        restConnector.putFile(getEventBinaryUrl(id), bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Override
    public void deleteEventBinary(String eventId) throws SDKException {
        restConnector.delete(getEventBinaryUrl(eventId));
    }

    private String getEventBinaryUrl(String eventId) {
        String self = eventsApiRepresentation.getEvents().getSelf();
        String eventBinaryUrl = self + "/{id}/binaries";
        return eventBinaryUrl.replace("{id}", eventId);
    }
}

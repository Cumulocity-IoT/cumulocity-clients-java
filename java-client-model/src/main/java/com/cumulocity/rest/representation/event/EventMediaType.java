package com.cumulocity.rest.representation.event;

import jakarta.ws.rs.core.MediaType;

import com.cumulocity.rest.representation.CumulocityMediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name). 
 */
public class EventMediaType extends CumulocityMediaType {

    public static final EventMediaType EVENT = new EventMediaType("event");

    public static final String EVENT_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "event+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final EventMediaType EVENT_COLLECTION = new EventMediaType("eventCollection");

    public static final String EVENT_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "eventCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final EventMediaType EVENT_API = new EventMediaType("eventApi");

    public static final String EVENTS_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "eventApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public EventMediaType(String string) {
        super(string);
    }
}


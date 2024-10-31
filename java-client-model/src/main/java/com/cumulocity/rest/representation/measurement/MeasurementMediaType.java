package com.cumulocity.rest.representation.measurement;

import jakarta.ws.rs.core.MediaType;

import com.cumulocity.rest.representation.CumulocityMediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name). 
 */
public class MeasurementMediaType extends CumulocityMediaType {

    public static final MeasurementMediaType MEASUREMENT = new MeasurementMediaType("measurement");

    public static final String MEASUREMENT_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "measurement+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final MeasurementMediaType MEASUREMENT_COLLECTION = new MeasurementMediaType("measurementCollection");

    public static final String MEASUREMENT_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "measurementCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final MeasurementMediaType MEASUREMENT_API = new MeasurementMediaType("measurementApi");

    public static final String MEASUREMENT_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "measurementApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public MeasurementMediaType(String entity) {
        super("application", VND_COM_NSN_CUMULOCITY + entity + "+json;" + VND_COM_NSN_CUMULOCITY_PARAMS);
    }
}

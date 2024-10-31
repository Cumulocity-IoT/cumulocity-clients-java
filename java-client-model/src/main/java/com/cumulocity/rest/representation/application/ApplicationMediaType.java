package com.cumulocity.rest.representation.application;

import com.cumulocity.rest.representation.CumulocityMediaType;

import jakarta.ws.rs.core.MediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name). 
 */
public class ApplicationMediaType extends CumulocityMediaType {
 
    public static final ApplicationMediaType APPLICATION = new ApplicationMediaType("application");
    
    public static final String APPLICATION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "application+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final ApplicationMediaType APPLICATION_COLLECTION = new ApplicationMediaType("applicationCollection");

    public static final String APPLICATION_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "applicationCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final ApplicationMediaType APPLICATION_REFERENCE = new ApplicationMediaType("applicationReference");

    public static final String APPLICATION_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "applicationReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;
    
    public static final ApplicationMediaType APPLICATION_REFERENCE_COLLECTION = new ApplicationMediaType("applicationReferenceCollection");

    public static final String APPLICATION_REFERENCE_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "applicationReferenceCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;
  
    public static final ApplicationMediaType APPLICATION_API = new ApplicationMediaType("applicationApi");
 
    public static final String APPLICATION_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "applicationApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final CumulocityMediaType APPLICATION_USER_COLLECTION_MEDIA_TYPE = new CumulocityMediaType("applicationUserCollection");

    public static final String APPLICATION_USER_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "applicationUserCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String APPLICATION_LOGS_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "applicationLogs+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String APPLICATION_VERSION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "applicationVersion+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public ApplicationMediaType(String entity) {
        super(entity);
    }
}

package com.cumulocity.rest.representation.identity;

import jakarta.ws.rs.core.MediaType;

import com.cumulocity.rest.representation.CumulocityMediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name). 
 */
public class IdentityMediaType extends CumulocityMediaType {

    public static final IdentityMediaType EXTERNAL_ID = new IdentityMediaType("externalId");

    public static final String EXTERNAL_ID_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "externalId+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final IdentityMediaType EXTERNAL_ID_COLLECTION = new IdentityMediaType("externalIdCollection");

    public static final String EXTERNAL_ID_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "externalIdCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final IdentityMediaType IDENTITY_API = new IdentityMediaType("identityApi");

    public static final String IDENTITY_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "identityApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public IdentityMediaType(String entity) {
        super("application", VND_COM_NSN_CUMULOCITY + entity + "+json;" + VND_COM_NSN_CUMULOCITY_PARAMS);
    }

}

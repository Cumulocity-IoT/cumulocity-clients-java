package com.cumulocity.rest.representation.tenant;

import com.cumulocity.rest.representation.CumulocityMediaType;

import jakarta.ws.rs.core.MediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name).
 */
public class TenantMediaType extends CumulocityMediaType {

    public static final TenantMediaType TENANT = new TenantMediaType("tenant");
    
    public static final TenantMediaType TENANT_REFERENCE = new TenantMediaType("tenantReference");

    public static final String TENANT_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "tenant+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String TENANT_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "tenantApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final String TENANT_REFERENCE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "tenantReference+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public static final TenantMediaType TENANT_COLLECTION = new TenantMediaType("tenantCollection");

    public static final String TENANT_COLLECTION_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "tenantCollection+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public TenantMediaType(String entity) {
        super(entity);
    }
}

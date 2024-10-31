package com.cumulocity.rest.representation.platform;

import jakarta.ws.rs.core.MediaType;

import com.cumulocity.rest.representation.CumulocityMediaType;

/**
 * We follow here convention from {@link MediaType} class, where we have both {@link MediaType}
 * instances, and string representations (with '_TYPE' suffix in name). 
 */
public class PlatformMediaType extends CumulocityMediaType {

    public static final PlatformMediaType PLATFORM_API = new PlatformMediaType("platformApi");

    public static final String PLATFORM_API_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "platformApi+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    public PlatformMediaType(String string) {
        super(string);
    }
}


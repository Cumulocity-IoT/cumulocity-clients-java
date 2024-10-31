package com.cumulocity.rest.representation;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CumulocityMediaType extends MediaType {

    protected static final String VND_COM_NSN_CUMULOCITY = "vnd.com.nsn.cumulocity.";

    public static final String APPLICATION_VND_COM_NSN_CUMULOCITY = "application/" + VND_COM_NSN_CUMULOCITY;


    public static final String APPLICATION_JSON_STREAM_TYPE = "application/json-stream";

    public static final MediaType APPLICATION_JSON_STREAM = new MediaType("application","json-stream");

    public static final CumulocityMediaType ERROR_MESSAGE = new CumulocityMediaType("error");
    
    public static final String VND_COM_NSN_CUMULOCITY_CHARSET = "charset=UTF-8";
    
    public static final String VND_COM_NSN_CUMULOCITY_VERSION = "ver=0.9";
    
    public static final String VND_COM_NSN_CUMULOCITY_PARAMS = VND_COM_NSN_CUMULOCITY_CHARSET + ";" + VND_COM_NSN_CUMULOCITY_VERSION;

    public static final String ERROR_MESSAGE_TYPE = APPLICATION_VND_COM_NSN_CUMULOCITY + "error+json;" + VND_COM_NSN_CUMULOCITY_PARAMS;

    @Deprecated
    @SuppressWarnings("serial")
    private static Map<String, String> mediaTypeParams = new HashMap<String, String>() {
        {
            put("ver", "0.9");
            put("charset", UTF_8.name());
        };
    };

    private MediaType parametrizedMediaTypeObject;

    public CumulocityMediaType() {
        super();
    }

    public CumulocityMediaType(String type, String subtype) {
        super(type, subtype);
    }

    public CumulocityMediaType(String entity) {
        super("application", VND_COM_NSN_CUMULOCITY + entity + "+json;" + VND_COM_NSN_CUMULOCITY_PARAMS);
    }

    public CumulocityMediaType(String type, String subtype, Map<String, String> parameters) {
        super(type, subtype, parameters);
    }

    @Deprecated
    public MediaType withParams() {
        if (parametrizedMediaTypeObject == null) {
            parametrizedMediaTypeObject = new MediaType(getType(), getSubtype(), mediaTypeParams);
        }
        return parametrizedMediaTypeObject;
    }

    public String getTypeString() {
        return getType() + "/" + getSubtype();
    }

}

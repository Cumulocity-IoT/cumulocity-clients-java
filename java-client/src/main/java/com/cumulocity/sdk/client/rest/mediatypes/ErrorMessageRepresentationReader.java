package com.cumulocity.sdk.client.rest.mediatypes;

import com.cumulocity.model.CumulocityCharset;
import com.cumulocity.rest.representation.ErrorMessageRepresentation;
import org.svenson.JSONParser;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Scanner;

@Provider
public class ErrorMessageRepresentationReader implements MessageBodyReader<ErrorMessageRepresentation> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ErrorMessageRepresentation.class.equals(type);
    }

    @Override
    public ErrorMessageRepresentation readFrom(Class<ErrorMessageRepresentation> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
            WebApplicationException {
        String content = convertStreamToString(entityStream);
        return JSONParser.defaultJSONParser().parse(type, content);
    }

    public String convertStreamToString(InputStream is) {
        return new Scanner(is, CumulocityCharset.CHARSET).useDelimiter("\\A").next();
    }
}

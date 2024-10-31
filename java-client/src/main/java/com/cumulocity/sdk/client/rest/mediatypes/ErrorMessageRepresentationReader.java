package com.cumulocity.sdk.client.rest.mediatypes;

import com.cumulocity.rest.representation.ErrorMessageRepresentation;
import org.svenson.JSONParser;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        return new Scanner(is, UTF_8.name()).useDelimiter("\\A").next();
    }
}

package com.cumulocity.sdk.client.rest.providers;

import com.cumulocity.model.JSONBase;
import com.cumulocity.rest.representation.BaseResourceRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.svenson.JSONParseException;
import org.svenson.JSONParser;
import org.svenson.SvensonRuntimeException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;

@Provider
@Slf4j
public class CumulocityJSONMessageBodyReader implements MessageBodyReader<BaseResourceRepresentation> {

    private final JSONParserAdapter unmarshaller;

    public CumulocityJSONMessageBodyReader(JSONParserAdapter unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public CumulocityJSONMessageBodyReader() {
        this(new JSONParserAdapter() {
            private final JSONParser jsonParser = JSONBase.getJSONParser();

            @Override
            public <T> T parse(Class<T> targetType, String json) {
                return jsonParser.parse(targetType, json);
            }
        });
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return BaseResourceRepresentation.class.isAssignableFrom(type);
    }

    @Override
    public BaseResourceRepresentation readFrom(Class<BaseResourceRepresentation> type, Type genericType,
                                               Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws WebApplicationException {

        String content = convertStreamToString(entityStream);
        if (content == null) {
            return null;
        }

        try {
            return unmarshaller.parse(type, content);
        } catch (SvensonRuntimeException e) {
            //Svenson leaks class and fields names inside its message
            log.debug("Could not parse JSON request", e);
            String cause = Optional.ofNullable(e.getCause())
                    .map(Throwable::getMessage)
                    .orElse("");
            throw new JSONParseException("Could not parse JSON request: " + cause);
        } catch (IllegalArgumentException e) {
            throw new JSONParseException("Could not parse JSON request: " + e.getMessage());
        }
    }

    private String convertStreamToString(InputStream is) {
        try {
            // \A is the beginning of the input
            return new Scanner(is, UTF_8.name()).useDelimiter("\\A").next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public interface JSONParserAdapter {
        <T> T parse(Class<T> targetType, String json);
    }
}

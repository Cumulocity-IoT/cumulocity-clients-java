package com.cumulocity.sdk.client.rest.providers;

import com.cumulocity.model.JSONBase;
import com.cumulocity.rest.representation.BaseResourceRepresentation;
import com.cumulocity.sdk.client.SvensonConfig;
import com.google.common.base.Strings;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.svenson.JSONParseException;
import org.svenson.JSONParser;
import org.svenson.SvensonRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Provider
@Slf4j
public class CumulocityJSONMessageBodyReader implements MessageBodyReader<BaseResourceRepresentation> {

    @Getter(AccessLevel.PACKAGE) //for tests
    private final JSONParserAdapter unmarshaller;

    public CumulocityJSONMessageBodyReader() {
        this(new SvensonJSONParserAdapter());
    }

    public CumulocityJSONMessageBodyReader(JSONParserAdapter unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public CumulocityJSONMessageBodyReader(SvensonConfig svensonConfig) {
        this.unmarshaller = new SvensonJSONParserAdapter(svensonConfig);
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

        if (Strings.isNullOrEmpty(content)) {
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
            return IOUtils.toString(is, UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public interface JSONParserAdapter {
        <T> T parse(Class<T> targetType, String json);
    }

    private static class SvensonJSONParserAdapter implements JSONParserAdapter {

        private static final Pattern PATTERN_INVALID_JSON_CHARS = Pattern.compile("\\p{javaIdentifierIgnorable}");
        private final JSONParser jsonParser = JSONBase.getJSONParser();
        SvensonConfig svensonConfig;

        public SvensonJSONParserAdapter() {
            this(new SvensonConfig());
        }

        public SvensonJSONParserAdapter(SvensonConfig svensonConfig) {
            this.svensonConfig = svensonConfig;
        }

        @Override
        public <T> T parse(Class<T> targetType, String json) {
            if (this.svensonConfig.isStripControlCharacters()) {
                String sanitizedJson = stripInvalidCharacters(json);

                int originalLen = json.length();
                int sanitizedLen = sanitizedJson.length();

                if (sanitizedLen != originalLen) {
                    log.warn("Removed {} invalid characters from JSON input", originalLen - sanitizedLen);
                }
                return jsonParser.parse(targetType, sanitizedJson);
            } else {
                return jsonParser.parse(targetType, json);
            }
        }

        private String stripInvalidCharacters(String json) {
            return PATTERN_INVALID_JSON_CHARS.matcher(json).replaceAll("");
        }

    }

}
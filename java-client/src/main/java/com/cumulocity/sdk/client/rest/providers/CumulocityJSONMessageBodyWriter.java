package com.cumulocity.sdk.client.rest.providers;

import com.cumulocity.model.JSONBase;
import com.cumulocity.rest.representation.BaseResourceRepresentation;
import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.SourceableConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.svenson.JSON;
import org.svenson.WriterSink;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

@Provider
public class CumulocityJSONMessageBodyWriter implements MessageBodyWriter<BaseResourceRepresentation> {
    private final Logger log = LoggerFactory.getLogger(CumulocityJSONMessageBodyWriter.class);
    private static final Charset DEFAULT_CHARSET = Charset.forName(UTF_8.name());

    private final JSON marshaller;

    public CumulocityJSONMessageBodyWriter() {
        this.marshaller = JSONBase.JSONGeneratorBuilder.jsonGenerator()
                .withDefaults()
                .typeConverter(new SourceableConverter())
                .build();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return BaseResourceRepresentation.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(BaseResourceRepresentation t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(BaseResourceRepresentation representation,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        if (!mediaType.isCompatible(CumulocityMediaType.APPLICATION_JSON_STREAM)) {
            String value = marshaller.forValue(representation);
            entityStream.write(value.getBytes(DEFAULT_CHARSET));
        } else {
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(entityStream, DEFAULT_CHARSET))) {
                marshaller.dumpObject(new WriterSink(out), representation);
            }
        }
    }
}

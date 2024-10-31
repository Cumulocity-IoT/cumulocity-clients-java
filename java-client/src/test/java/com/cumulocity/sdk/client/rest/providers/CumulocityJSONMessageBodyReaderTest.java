package com.cumulocity.sdk.client.rest.providers;

import com.cumulocity.rest.representation.BaseResourceRepresentation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.svenson.JSONParseException;
import org.svenson.SvensonRuntimeException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static com.cumulocity.sdk.client.rest.providers.CumulocityJSONMessageBodyReader.JSONParserAdapter;

public class CumulocityJSONMessageBodyReaderTest {

    @Test
    public void messageShouldNotContainClassNames() {
        // given
        JSONParserAdapter unmarshaller = Mockito.mock(JSONParserAdapter.class);
        MessageBodyReader<BaseResourceRepresentation> reader = new CumulocityJSONMessageBodyReader(unmarshaller);
        when(unmarshaller.parse(eq(BaseResourceRepresentation.class), eq("{}")))
                .thenThrow(new SvensonRuntimeException("Class names leaking svenson runtime exception", new RuntimeException("RealCause")));

        //when
        Throwable thrown = catchThrowable(() ->
                reader.readFrom(
                        BaseResourceRepresentation.class,
                        BaseResourceRepresentation.class,
                        new Annotation[0],
                        MediaType.APPLICATION_JSON_TYPE,
                        null,
                        new ByteArrayInputStream("{}".getBytes())
                )
        );

        // then
        assertThat(thrown.getMessage()).doesNotContain("Class", "names", "leaking", "svenson");
        assertThat(thrown.getMessage()).contains("RealCause");
        assertThat(thrown).isInstanceOf(JSONParseException.class);
    }
}

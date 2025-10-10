package com.cumulocity.sdk.client.rest.providers;

import com.cumulocity.rest.representation.BaseResourceRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SvensonConfig;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.svenson.JSONParseException;
import org.svenson.SvensonRuntimeException;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;

import static com.cumulocity.sdk.client.rest.providers.CumulocityJSONMessageBodyReader.JSONParserAdapter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class CumulocityJSONMessageBodyReaderTest {

    String jsonWithInvalidChars= """
            {
              "foo": "abcd",
              "name": "Invalid â€œCelâ€ String",
              "description": "This string contains an illegal  character."
            }
            """;

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

    @Test
    public void shouldStripInvalidCharsInStripInvalidCharsMode() {
        SvensonConfig svensonConfig = new SvensonConfig();
        svensonConfig.setStripControlCharacters(true);
        CumulocityJSONMessageBodyReader cumulocityJSONMessageBodyReader = new CumulocityJSONMessageBodyReader(svensonConfig);
        JSONParserAdapter jsonParserAdapter = cumulocityJSONMessageBodyReader.getUnmarshaller();
        ManagedObjectRepresentation mo = jsonParserAdapter.parse(ManagedObjectRepresentation.class, jsonWithInvalidChars);
        assertFalse(mo.toJSON().contains("\u009D"));
        assertFalse(mo.toJSON().contains("\u009F"));
        assertTrue(mo.getName().contains("String"));
        assertEquals("abcd", mo.get("foo"));
    }

    @Test
    public void shouldRaiseExceptionOnInvalidCharacterInDefaultMode() {
        SvensonConfig svensonConfig = new SvensonConfig();
        CumulocityJSONMessageBodyReader cumulocityJSONMessageBodyReader = new CumulocityJSONMessageBodyReader(svensonConfig);
        JSONParserAdapter jsonParserAdapter = cumulocityJSONMessageBodyReader.getUnmarshaller();
        assertThrows(JSONParseException.class, () -> {
            jsonParserAdapter.parse(ManagedObjectRepresentation.class, jsonWithInvalidChars);
        });
    }


}

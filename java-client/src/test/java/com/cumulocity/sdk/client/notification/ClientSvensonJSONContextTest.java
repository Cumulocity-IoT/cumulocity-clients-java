package com.cumulocity.sdk.client.notification;

import static com.cumulocity.model.idtype.GId.asGId;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.Message.Mutable;
import org.cometd.common.HashMapMessage;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cumulocity.rest.representation.event.EventRepresentation;
import org.skyscreamer.jsonassert.JSONAssert;

public class ClientSvensonJSONContextTest {
    private final String EVENT_JSON = "{\"id\":\"1\"}";

    private final String MESSAGE_JSON = format(
            "{\"id\":\"403\",\"data\":%s,\"supportedConnectionTypes\":[\"long-polling\"],\"channel\":\"/meta/handshake\",\"version\":\"1.0\"}",
            EVENT_JSON);

    private final String INVALID_MESSAGE = "{\"data\":{\"creationTime\":\"2021-01-14T12:51:34.606Z\",\"deviceName\":\"myDevice\",\"deviceId\":\"107\",\"self\":\"http://cumulocity.default.svc.cluster.local/devicecontrol/operations/117\",\"id\":\"117\",\"status\":\"PENDING\",\"description\":\"Dummy operation\",\"com_cumulocity_sdk_client_notification_DummyOperation\":{\"strings\":\"some string\"}}}";

    private final String MESSAGE_JSON_ARRAY = format("[%s]", MESSAGE_JSON);

    private final String INVALID_MESSAGE_JSON_ARRAY = format("[%s]", INVALID_MESSAGE);

    private final HashMapMessage MESSAGE = new HashMapMessage();
    {
        MESSAGE.put(Message.SUPPORTED_CONNECTION_TYPES_FIELD, Arrays.asList("long-polling"));
        MESSAGE.put(Message.ID_FIELD, "403");
        MESSAGE.put(Message.CHANNEL_FIELD, "/meta/handshake");
        MESSAGE.put(Message.VERSION_FIELD, "1.0");
        final EventRepresentation event = new EventRepresentation();
        event.setId(asGId("1"));
        MESSAGE.put(Message.DATA_FIELD, event);
    }

    private ClientSvensonJSONContext jsonContextClient;

    @BeforeEach
    public void setUp() {
        jsonContextClient = new ClientSvensonJSONContext();
    }

    @Test
    public final void shouldParseFromInputStream() throws ParseException {
        final Mutable[] unserialized = jsonContextClient.parse(new ByteArrayInputStream(MESSAGE_JSON_ARRAY.getBytes()));
        verifyUnserialized(unserialized);
    }

    @Test
    public final void shouldGenerateExceptionForInvalidJson() {
        Throwable thrown = catchThrowable(() -> jsonContextClient.parse(new ByteArrayInputStream("null".getBytes())));

        assertThat(thrown).isInstanceOf(ParseException.class);
    }

    @Test
    public final void shouldParseFromStringAndData() throws ParseException {
        jsonContextClient = new ClientSvensonJSONContext(EventRepresentation.class);
        final Mutable[] unserialized = jsonContextClient.parse(MESSAGE_JSON_ARRAY).toArray(new Mutable[0]);
        verifyUnserialized(unserialized, EventRepresentation.class);
    }

    @Test
    public final void shouldParseFromReader() throws ParseException {
        final Mutable[] unserialized = jsonContextClient.parse(new StringReader(MESSAGE_JSON_ARRAY));
        verifyUnserialized(unserialized);
    }

    @Test
    public final void shouldGenerateExceptionForInvalidCustomClassJson() {
        Assertions.assertThrows(ParseException.class, () -> {
            jsonContextClient.parse(new StringReader(INVALID_MESSAGE_JSON_ARRAY));
        });
    }

    @Test
    public final void shouldParseFromString() throws ParseException {
        final Mutable[] unserialized = jsonContextClient.parse(MESSAGE_JSON_ARRAY).toArray(new Mutable[0]);
        verifyUnserialized(unserialized);
    }

    @Test
    public final void shouldGenerateJSONFromMessageObject() throws JSONException {
        final String result = jsonContextClient.generate(MESSAGE);
        assertThat(result).isNotNull().isNotEmpty();
        JSONAssert.assertEquals(MESSAGE_JSON, result, false);
    }

    @Test
    public final void shouldGenerateJSONFromArrayOfMessageObjects() throws JSONException {
        final String result = jsonContextClient.generate(Arrays.<Mutable>asList(MESSAGE));
        assertThat(result).isNotNull().isNotEmpty();
        JSONAssert.assertEquals(MESSAGE_JSON_ARRAY, result, false);
    }

    private void verifyUnserialized(final Mutable[] unserialized) {
        verifyUnserialized(unserialized, Map.class);
    }

    private void verifyUnserialized(final Mutable[] unserialized, Class<?> dataType) {

        assertThat(unserialized).isNotNull().isNotEmpty().hasSize(1).hasOnlyElementsOfType(jsonContextClient.targetClass());
        final Object event = unserialized[0].getData();
        assertThat(event).isNotNull().isInstanceOf(dataType);
    }
}

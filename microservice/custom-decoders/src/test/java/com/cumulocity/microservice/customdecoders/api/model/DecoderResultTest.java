package com.cumulocity.microservice.customdecoders.api.model;

import com.cumulocity.microservice.customdecoders.api.configuration.JacksonConfiguration;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JacksonConfiguration.class)
public class DecoderResultTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void nullFieldsShouldBeIgnoredOnSerialization() throws Exception {
        DecoderResult decoderResult = new DecoderResult();
        decoderResult.setMessage("\"DecoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'inputData', 'manufacturer and/or model'\"");

        String expectedJson = "{\"message\":\"\\\"DecoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'inputData', 'manufacturer and/or model'\\\"\",\"success\":true}";
        String outputJson = objectMapper.writeValueAsString(decoderResult);
        assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(outputJson));
    }

    @Test
    public void nullFieldsShouldBeIgnoredOnSerialization_FailCase() throws Exception {
        DecoderResult decoderResult = new DecoderResult();
        decoderResult.setMessage("\"DecoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'inputData', 'manufacturer and/or model'\"");

        String expectedJson = "{\"self\":null, \"alarms\":null, \"alarmTypesToUpdate\":null, \"events\":null, \"measurements\":null, \"dataFragments\":null, \"message\":\"\\\"DecoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'inputData', 'manufacturer and/or model'\\\"\",\"success\":true}";
        String outputJson = objectMapper.writeValueAsString(decoderResult);
        assertNotEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(outputJson));
    }

    @Test
    public void successOnSerializationWhenSettingDateTime() throws Exception {
        DateTime dateTime = new DateTime(17399691);
        DecoderResult decoderResult = new DecoderResult();
        EventRepresentation event = new EventRepresentation();
        event.setDateTime(dateTime);

        List<EventRepresentation> eventsList = List.of(event);
        decoderResult.setEvents(eventsList);

        String expectedJson = "{\"events\":[{\"self\":null,\"attrs\":{},\"id\":null,\"type\":null,\"time\":17399691,\"creationTime\":null,\"text\":null,\"externalSource\":null,\"source\":null,\"dateTime\":17399691,\"creationDateTime\":null,\"lastUpdatedDateTime\":null,\"selfDecoded\":null}],\"success\":true}";
        String outputJson = objectMapper.writeValueAsString(decoderResult);
        assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(outputJson));
    }
}
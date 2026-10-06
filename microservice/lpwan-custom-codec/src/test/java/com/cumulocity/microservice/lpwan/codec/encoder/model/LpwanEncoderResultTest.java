package com.cumulocity.microservice.lpwan.codec.encoder.model;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LpwanEncoderResultTest {
    @Test
    public void fportShouldNotAppearTwiceOnSerialization() throws JacksonException {
        LpwanEncoderResult lpwanEncoderResult = new LpwanEncoderResult();
        lpwanEncoderResult.setEncodedCommand("9F000000");
        Map<String, String> properties = new HashMap<>();
        properties.put("fport", "20");
        lpwanEncoderResult.setProperties(properties);
        lpwanEncoderResult.setSuccess(true);
        lpwanEncoderResult.setMessage("Successfully Encoded the payload");

        ObjectMapper objectMapper = new ObjectMapper();
        String outputJson = objectMapper.writeValueAsString(lpwanEncoderResult);
        assertTrue(outputJson.contains("\"properties\":{\"fport\":\"20\"}"));
        // Used to appear in form {"encodedCommand":"9F000000","properties":{"fport":"20"},"message":"Successfully Encoded the payload","success":true,"fport":20} when @JsonIgnore was missing
        assertFalse(outputJson.contains(",\"fport\":20"));
    }

    @Test
    public void nullFieldsShouldBeIgnoredOnSerialization() throws Exception {
        LpwanEncoderResult lpwanEncoderResult = new LpwanEncoderResult();
        lpwanEncoderResult.setMessage("\"EncoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'manufacturer, model and/or supportedCommands', 'commandName'\"");
        ObjectMapper objectMapper = new ObjectMapper();

        String expectedJson = "{\"message\":\"\\\"EncoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'manufacturer, model and/or supportedCommands', 'commandName'\\\"\",\"success\":true}";
        String outputJson = objectMapper.writeValueAsString(lpwanEncoderResult);
        assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(outputJson));
    }

    @Test
    public void nullFieldsShouldBeIgnoredOnSerialization_FailCase() throws Exception {
        LpwanEncoderResult lpwanEncoderResult = new LpwanEncoderResult();
        lpwanEncoderResult.setMessage("\"EncoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'manufacturer, model and/or supportedCommands', 'commandName'\"");
        ObjectMapper objectMapper = new ObjectMapper();

        String expectedJson = "{\"encodedCommand\":null, \"properties\":null,\"message\":\"\\\"EncoderInputData is missing mandatory fields: 'sourceDeviceId', 'sourceDeviceEui', 'manufacturer, model and/or supportedCommands', 'commandName'\\\"\",\"success\":true}";
        String outputJson = objectMapper.writeValueAsString(lpwanEncoderResult);
        assertNotEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(outputJson));
    }

    @Test
    public void deserializedObjectShouldHaveFPort() throws JacksonException {
        String inputJson = "{\"encodedCommand\":\"9F000000\",\"properties\":{\"fport\":\"20\"},\"message\":\"Successfully Encoded the payload\",\"success\":true}";
        ObjectMapper objectMapper = new ObjectMapper();
        LpwanEncoderResult lpwanEncoderResult = objectMapper.readValue(inputJson, LpwanEncoderResult.class);
        assertEquals(20, lpwanEncoderResult.getFport());
    }
}
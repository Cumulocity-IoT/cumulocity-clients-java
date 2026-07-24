package com.cumulocity.microservice.lpwan.codec.rest;


import com.cumulocity.microservice.customdecoders.api.exception.DecoderServiceException;
import com.cumulocity.microservice.customdecoders.api.model.DecoderInputData;
import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;
import com.cumulocity.microservice.customdecoders.api.service.DecoderService;
import com.cumulocity.microservice.customencoders.api.exception.EncoderServiceException;
import com.cumulocity.microservice.customencoders.api.model.EncoderInputData;
import com.cumulocity.microservice.customencoders.api.model.EncoderResult;
import com.cumulocity.microservice.customencoders.api.service.EncoderService;
import com.cumulocity.microservice.lpwan.codec.decoder.model.LpwanDecoderInputData;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderInputData;
import com.cumulocity.microservice.lpwan.codec.handler.CodecExceptionsHandler;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;
import com.cumulocity.model.idtype.GId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CodecController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableWebMvc
public class CodecControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CodecController controller;

    @MockitoBean
    private DecoderService decoderService;

    @MockitoBean
    private EncoderService encoderService;

    @Captor
    private ArgumentCaptor<DecoderInputData> decoderInputCapture;

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    @Test
    public void doCallDecodeAPI_withValidJSONInput_Success() throws Exception {
        DecoderInputData decoderInputData = new DecoderInputData();
        decoderInputData.setValue("HEX VALUE");
        decoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanDecoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        decoderInputData.setArgs(args);

        DecoderResult decoderResult = new DecoderResult();
        decoderResult.setSuccess(true);

        when(decoderService.decode(eq("HEX VALUE"), eq(GId.asGId("SOURCE_DEVICE_ID")), eq(args))).thenReturn(decoderResult);

        this.mockMvc.perform(post("/decode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(decoderInputData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(decoderResult), true));

        verify(decoderService).decode(eq("HEX VALUE"), eq(GId.asGId("SOURCE_DEVICE_ID")), eq(args));
    }

    @Test
    public void doCallEncodeAPI_withValidJSONInput_Success() throws Exception {
        EncoderInputData encoderInputData = new EncoderInputData();
        encoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");
        encoderInputData.setCommandName("set config");
        encoderInputData.setCommandData("{\"set config\":{\"breakpoint\":\"false\",\"selfadapt\":\"true\",\"oneoff\":\"false\",\"alreport\":\"false\",\"pos\":\"0\",\"hb\":\"10\"}}");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanEncoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        encoderInputData.setArgs(args);

        EncoderResult encoderResult = new EncoderResult();
        encoderResult.setSuccess(true);

        when(encoderService.encode(encoderInputData)).thenReturn(encoderResult);

        this.mockMvc.perform(post("/encode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(encoderInputData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(encoderResult), true));

        verify(encoderService).encode(encoderInputData);
    }

    @Test
    public void doCallDecodeAPI_withDecoderServiceException_Fail() throws Exception {
        DecoderInputData decoderInputData = new DecoderInputData();
        decoderInputData.setValue("HEX VALUE");
        decoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanDecoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        decoderInputData.setArgs(args);

        DecoderResult decoderResult = new DecoderResult();
        decoderResult.setSuccess(false);
        decoderResult.setMessage("Decoding failed.");

        when(decoderService.decode(eq("HEX VALUE"), eq(GId.asGId("SOURCE_DEVICE_ID")), eq(args))).thenThrow(new DecoderServiceException(null, "Decoding failed.", DecoderResult.empty()));

        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(CodecExceptionsHandler.class).build().perform(post("/decode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(decoderInputData)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(decoderResult), true));

        verify(decoderService).decode(eq("HEX VALUE"), eq(GId.asGId("SOURCE_DEVICE_ID")), eq(args));
    }

    @Test
    public void doCallEncodeAPI_withEncoderServiceException_Fail() throws Exception {
        EncoderInputData encoderInputData = new EncoderInputData();
        encoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");
        encoderInputData.setCommandName("set config");
        encoderInputData.setCommandData("{\"set config\":{\"breakpoint\":\"false\",\"selfadapt\":\"true\",\"oneoff\":\"false\",\"alreport\":\"false\",\"pos\":\"0\",\"hb\":\"10\"}}");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanEncoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        encoderInputData.setArgs(args);

        EncoderResult encoderResult = new EncoderResult();
        encoderResult.setSuccess(false);
        encoderResult.setMessage("Encoding failed.");

        when(encoderService.encode(encoderInputData)).thenThrow(new EncoderServiceException(null, "Encoding failed.", EncoderResult.empty()));

        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(CodecExceptionsHandler.class).build().perform(post("/encode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(encoderInputData)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(encoderResult), true));
    }

    @Test
    public void doCallEncodeAPI_withInvalidJSONInput_Fail() throws Exception {
        EncoderInputData encoderInputData = new EncoderInputData();

        EncoderResult encoderResult = new EncoderResult();
        encoderResult.setSuccess(true);

        try {
            mockMvc.perform(post("/encode")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(jsonObjectMapper.writeValueAsString(encoderInputData)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(jsonObjectMapper.writeValueAsString(encoderResult), true));
        } catch(Exception e) {
            Assert.assertEquals("Unparsable JSON string: ", e.getMessage());
        }
    }

    @Test
    public void doCallDecodeAPI_without_DecoderService_implementation() throws Exception {
        CodecController controllerWithManuallyInjectedMocks = new CodecController();
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "decoderService", null);
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "encoderService", encoderService);

        DecoderInputData decoderInputData = new DecoderInputData();
        decoderInputData.setValue("HEX VALUE");
        decoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanDecoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        decoderInputData.setArgs(args);

        DecoderResult decoderResult = new DecoderResult();
        decoderResult.setSuccess(false);
        decoderResult.setMessage("No implementation provided for the DecoderService");

        MockMvcBuilders.standaloneSetup(controllerWithManuallyInjectedMocks).setControllerAdvice(CodecExceptionsHandler.class).build().perform(post("/decode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(decoderInputData)))
                .andDo(print())
                .andExpect(status().isNotImplemented())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(decoderResult), true));
    }

    @Test
    public void doCallEncodeAPI_without_DecoderService_implementation() throws Exception {
        CodecController controllerWithManuallyInjectedMocks = new CodecController();
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "decoderService", null);
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "encoderService", encoderService);

        EncoderInputData encoderInputData = new EncoderInputData();
        encoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");
        encoderInputData.setCommandName("set config");
        encoderInputData.setCommandData("{\"set config\":{\"breakpoint\":\"false\",\"selfadapt\":\"true\",\"oneoff\":\"false\",\"alreport\":\"false\",\"pos\":\"0\",\"hb\":\"10\"}}");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanEncoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        encoderInputData.setArgs(args);

        EncoderResult encoderResult = new EncoderResult();
        encoderResult.setSuccess(true);

        when(encoderService.encode(encoderInputData)).thenReturn(encoderResult);

        MockMvcBuilders.standaloneSetup(controllerWithManuallyInjectedMocks).setControllerAdvice(CodecExceptionsHandler.class).build().perform(post("/encode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(encoderInputData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(encoderResult), true));

        verify(encoderService).encode(encoderInputData);
    }

    @Test
    public void doCallDecodeAPI_without_EncoderService_implementation() throws Exception {
        CodecController controllerWithManuallyInjectedMocks = new CodecController();
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "decoderService", decoderService);
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "encoderService", null);

        DecoderInputData decoderInputData = new DecoderInputData();
        decoderInputData.setValue("HEX VALUE");
        decoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanDecoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        decoderInputData.setArgs(args);

        DecoderResult decoderResult = new DecoderResult();
        decoderResult.setSuccess(true);

        when(decoderService.decode(eq("HEX VALUE"), eq(GId.asGId("SOURCE_DEVICE_ID")), eq(args))).thenReturn(decoderResult);

        MockMvcBuilders.standaloneSetup(controllerWithManuallyInjectedMocks).setControllerAdvice(CodecExceptionsHandler.class).build().perform(post("/decode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(decoderInputData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(decoderResult), true));

        verify(decoderService).decode(eq("HEX VALUE"), eq(GId.asGId("SOURCE_DEVICE_ID")), eq(args));
    }

    @Test
    public void doCallEncodeAPI_without_EncoderService_implementation() throws Exception {
        CodecController controllerWithManuallyInjectedMocks = new CodecController();
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "decoderService", decoderService);
        ReflectionTestUtils.setField(controllerWithManuallyInjectedMocks, "encoderService", null);

        EncoderInputData encoderInputData = new EncoderInputData();
        encoderInputData.setSourceDeviceId("SOURCE_DEVICE_ID");
        encoderInputData.setCommandName("set config");
        encoderInputData.setCommandData("{\"set config\":{\"breakpoint\":\"false\",\"selfadapt\":\"true\",\"oneoff\":\"false\",\"alreport\":\"false\",\"pos\":\"0\",\"hb\":\"10\"}}");

        HashMap<String, String> args = new HashMap<>();
        args.put(LpwanEncoderInputData.SOURCE_DEVICE_EUI_KEY, "SOURCE_DEVICE_EUI_ID");
        args.put(DeviceInfo.DEVICE_MANUFACTURER, "MANUFACTURER_1");
        args.put(DeviceInfo.DEVICE_MODEL, "MODEL_1");
        encoderInputData.setArgs(args);

        EncoderResult encoderResult = new EncoderResult();
        encoderResult.setSuccess(false);
        encoderResult.setMessage("No implementation provided for the EncoderService");

        MockMvcBuilders.standaloneSetup(controllerWithManuallyInjectedMocks).setControllerAdvice(CodecExceptionsHandler.class).build().perform(post("/encode")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonObjectMapper.writeValueAsString(encoderInputData)))
                .andDo(print())
                .andExpect(status().isNotImplemented())
                .andExpect(content().json(jsonObjectMapper.writeValueAsString(encoderResult), true));
    }
}
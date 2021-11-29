package com.cumulocity.lpwan.payload.service;

import com.cumulocity.lpwan.codec.model.DecoderOutput;
import com.cumulocity.lpwan.codec.model.LpwanCodecDetails;
import com.cumulocity.lpwan.devicetype.model.DeviceType;
import com.cumulocity.lpwan.devicetype.model.UplinkConfiguration;
import com.cumulocity.lpwan.payload.exception.PayloadDecodingFailedException;
import com.cumulocity.lpwan.payload.uplink.model.UplinkMessage;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PayloadDecoderServiceTest {

    @Mock
    private MicroserviceSubscriptionsService subscriptionsService;

    @Mock
    private ContextService<MicroserviceCredentials> contextService;

    @Mock
    DecoderOutput decoderOutput;

    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

    @InjectMocks
    PayloadDecoderService<UplinkMessage> payloadDecoderService = new PayloadDecoderService<>(null, null);


    @Test
    public void decodeLittleEndianHex() {
        try {
            UplinkConfiguration uplinkConfiguration = new UplinkConfiguration();
            uplinkConfiguration.setLittleEndian(true);
            uplinkConfiguration.setStartBit(0);
            uplinkConfiguration.setNoBits(32);
            uplinkConfiguration.setMultiplier(1.0);
            uplinkConfiguration.setOffset(0.0);

            Double actualVal = payloadDecoderService.decodeByConfiguration("5A109061", uplinkConfiguration);
            Double expectedVal = 1636831322.0;
            assertEquals(expectedVal, actualVal);

            uplinkConfiguration.setStartBit(8);
            uplinkConfiguration.setNoBits(16);
            actualVal = payloadDecoderService.decodeByConfiguration("5A109061", uplinkConfiguration);
            expectedVal = 36880.0;
            assertEquals(expectedVal, actualVal);

            uplinkConfiguration.setSigned(true);
            actualVal = payloadDecoderService.decodeByConfiguration("5A109061", uplinkConfiguration);
            expectedVal = -28656.0;
            assertEquals(expectedVal, actualVal);

            uplinkConfiguration.setBcd(true);
            actualVal = payloadDecoderService.decodeByConfiguration("5A109061", uplinkConfiguration);
            expectedVal = -990.0;
            assertEquals(expectedVal, actualVal);

        } catch (PayloadDecodingFailedException e) {
            fail();
        }
    }


    @Test
    public void decodeWithMultiplierAndOffset() {
        try {
            UplinkConfiguration uplinkConfiguration = new UplinkConfiguration();
            uplinkConfiguration.setStartBit(0);
            uplinkConfiguration.setNoBits(16);
            uplinkConfiguration.setMultiplier(1.0);
            uplinkConfiguration.setOffset(0.0);

            Double actualVal = payloadDecoderService.decodeByConfiguration("9062", uplinkConfiguration);
            Double expectedVal = 36962.0;
            assertEquals(expectedVal, actualVal);

            uplinkConfiguration.setMultiplier(0.5);
            actualVal = payloadDecoderService.decodeByConfiguration("9062", uplinkConfiguration);
            expectedVal = 18481.0;
            assertEquals(expectedVal, actualVal);

            uplinkConfiguration.setOffset(1.0);
            actualVal = payloadDecoderService.decodeByConfiguration("9062", uplinkConfiguration);
            expectedVal = 18482.0;
            assertEquals(expectedVal, actualVal);

            uplinkConfiguration.setBcd(true);
            actualVal = payloadDecoderService.decodeByConfiguration("9062", uplinkConfiguration);
            expectedVal = 4532.0;
            assertEquals(expectedVal, actualVal);

        } catch (PayloadDecodingFailedException e) {
            fail();
        }
    }

    @Test
    public void shouldTestPayloadDecodeAndMap() throws PayloadDecodingFailedException {
        UplinkMessage uplinkMessage = Mockito.mock(UplinkMessage.class, Mockito.CALLS_REAL_METHODS);
        when(uplinkMessage.getPayloadHex()).thenReturn("ABCDEF1234567");
        when(uplinkMessage.getExternalId()).thenReturn("deviceExternalId");
        when(uplinkMessage.getFport()).thenReturn(1);
        when(uplinkMessage.getDateTime()).thenReturn(new DateTime("2020-11-28T10:11:12.123"));

        LpwanCodecDetails lpwanCodecDetails = new LpwanCodecDetails("Manufacturer_X", "Model_X", "lpwanContextPath");

        DeviceType deviceType  = new DeviceType();
        deviceType.setLpwanCodecDetails(lpwanCodecDetails);
        deviceType.setFieldbusType("LORA");

        MicroserviceCredentials credentials = new MicroserviceCredentials("tenant", "username", "password", null, null, null, "appKey");
        when(contextService.getContext()).thenReturn(credentials);
        when(subscriptionsService.getTenant()).thenReturn("tenant");
        when(subscriptionsService.getCredentials(eq("tenant"))).thenReturn(Optional.of(credentials));

        payloadDecoderService.decodeAndMap(uplinkMessage, ManagedObjects.asManagedObject(GId.asGId("12345")), deviceType);

        verify(contextService).runWithinContext(eq(credentials), taskCaptor.capture());
        taskCaptor.getValue().run();
    }
}

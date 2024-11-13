/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.payload.service;

import com.cumulocity.lpwan.codec.exception.LpwanCodecServiceException;
import com.cumulocity.lpwan.codec.service.LpwanCodecService;
import com.cumulocity.lpwan.devicetype.model.DeviceType;
import com.cumulocity.lpwan.devicetype.model.UplinkConfiguration;
import com.cumulocity.lpwan.mapping.model.DecodedObject;
import com.cumulocity.lpwan.mapping.model.MappingCollections;
import com.cumulocity.lpwan.mapping.model.MessageTypeMapping;
import com.cumulocity.lpwan.payload.exception.PayloadDecodingFailedException;
import com.cumulocity.lpwan.payload.uplink.model.MessageIdConfiguration;
import com.cumulocity.lpwan.payload.uplink.model.MessageIdMapping;
import com.cumulocity.lpwan.payload.uplink.model.UplinkMessage;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

@Slf4j
public class PayloadDecoderService<T extends UplinkMessage> {
    @Autowired
    private MicroserviceSubscriptionsService subscriptionsService;

    @Autowired
    private ContextService<MicroserviceCredentials> contextService;

    @Autowired
    private LpwanCodecService lpwanCodecService;

    private final PayloadMappingService payloadMappingService;

    private final MessageIdReader<T> messageIdReader;

    public PayloadDecoderService(PayloadMappingService payloadMappingService, MessageIdReader<T> messageIdReader){
        this.payloadMappingService =  payloadMappingService;
        this.messageIdReader =  messageIdReader;
    }

    public interface MessageIdReader<T> {
        Integer read(T uplinkMessage, MessageIdConfiguration messageIdConfiguration);
    }

    /**
     * Finds uplink message id from the uplink payload based on the given configuration.
     *
     * @param uplink                 the uplink message
     * @param messageIdConfiguration the message ID configuration
     * @return decimal value of found message id
     */
    public static Integer messageIdFromPayload(UplinkMessage uplink, MessageIdConfiguration messageIdConfiguration) {
        String payload = uplink.getPayloadHex();
        MessageIdMapping messageIdMapping = messageIdConfiguration.getMessageIdMapping();
        try {
            return DecoderUtil.extractDecimalFromHex(payload, messageIdMapping.getStartBit(), messageIdMapping.getNoBits());
        } catch (Exception e) {
            String errorMsg = "Error extracting message id from payload: " + e.getMessage();
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * According to the configuration given for the device type,
     * decode the input uplink message and
     * persist the mapped data as measurement/alarm/event/managed object update to the source device.
     *
     * @param uplinkMessage the uplink message
     * @param source        the source device managed object
     * @param deviceType    the device type
     */
    public void decodeAndMap(T uplinkMessage, ManagedObjectRepresentation source, DeviceType deviceType) {
        String requestId = MDC.get("requestId");
        final String tenantId = contextService.getContext().getTenant();
        Optional<MicroserviceCredentials> serviceUser = subscriptionsService.getCredentials(tenantId);
        contextService.runWithinContext(serviceUser.get(), () -> {
            if (deviceType.getLpwanCodecDetails() == null) {
                List<UplinkConfiguration> uplinkConfigurations = deviceType.getUplinkConfigurations();

                MessageIdConfiguration messageIdConfiguration = deviceType.getMessageIdConfiguration();

                try {
                    Integer messageTypeId = messageIdReader.read(uplinkMessage, messageIdConfiguration);

                    MessageTypeMapping messageTypeMappings = deviceType.getMessageTypes().getMappingIndexesByMessageType(Integer.toString(messageTypeId));

                    if (messageTypeMappings == null) {
                        log.warn("Request Id {}, Message type id {} not found for device type {}", requestId, messageTypeId, deviceType);
                        return;
                    }

                    MappingCollections mappingCollections = new MappingCollections();
                    log.debug("Request Id {}, Decoding payload for device type {}", requestId, deviceType.getName());
                    for (Integer registerIndex : messageTypeMappings.getRegisterIndexes()) {

                        try {
                            UplinkConfiguration uplinkConfiguration = uplinkConfigurations.get(registerIndex);
                            DecodedObject decodedObject = generateDecodedData(uplinkMessage, uplinkConfiguration);
                            payloadMappingService.addMappingsToCollection(mappingCollections, decodedObject, uplinkConfiguration);
                        } catch (PayloadDecodingFailedException e) {
                            log.error("Request Id {}, Error decoding payload for device type {}: {} Skipping decoding payload part", requestId, deviceType, e.getMessage());
                        }
                    }
                    log.debug("Request Id {}, Executing mappings for device type {}", requestId, deviceType.getName());
                    payloadMappingService.executeMappings(mappingCollections, source, uplinkMessage.getDateTime());
                    log.debug("Request Id {}, Mappings executed for device type {}", requestId, deviceType.getName());
                } catch (Exception e) {
                    log.error("Request Id {}, exception: {}", requestId, e.getMessage());
                }
            } else {
                DecoderResult decoderResult;
                try {
                    log.debug("Request Id {}, Decoding using custom codec for device type {}", requestId, deviceType.getName());
                    decoderResult = lpwanCodecService.decode(deviceType, source, uplinkMessage);
                    log.debug("Request Id {}, Decoding using custom codec for device type {} completed", requestId, deviceType.getName());
                } catch (LpwanCodecServiceException e) {
                    decoderResult = DecoderResult.empty();
                    decoderResult.setAsFailed(String.format("Error decoding payload for device EUI '%s'. Skipping the decoding of the payload part. \nCause: %s", uplinkMessage.getExternalId(), e.getMessage()));
                }

                try {
                    log.debug("Request Id {}, Handling the decoder response for the device with EUI '{}'", requestId, uplinkMessage.getExternalId());
                    payloadMappingService.handleCodecServiceResponse(decoderResult, source, uplinkMessage.getExternalId());
                    log.debug("Request Id {}, Handling the decoder response for the device with EUI '{}' completed", requestId, uplinkMessage.getExternalId());
                } catch (PayloadDecodingFailedException e) {
                    log.error("Request Id {}, Error handling the decoder response for the device with EUI '{}'.", requestId, uplinkMessage.getExternalId(), e);
                }
            }
        });
    }

    private DecodedObject generateDecodedData(T uplinkMessage, UplinkConfiguration uplinkConfiguration)
            throws PayloadDecodingFailedException {
        DecodedObject decodedData = new DecodedObject();
        Double value = decodeByConfiguration(uplinkMessage.getPayloadHex(), uplinkConfiguration);
        String unit = uplinkConfiguration.getUnit();

        decodedData.putValue(value);
        if (StringUtils.isNotBlank(unit)) {
            decodedData.putUnit(unit);
        }

        return decodedData;
    }

    protected Double decodeByConfiguration(String payloadHex, UplinkConfiguration uplinkConfiguration)
            throws PayloadDecodingFailedException {
        Integer startBit = uplinkConfiguration.getStartBit();
        Integer numberOfBits = uplinkConfiguration.getNoBits();
        Double multiplication = uplinkConfiguration.getMultiplier();
        Double offset = uplinkConfiguration.getOffset();
        Double value;

        if (uplinkConfiguration.isLittleEndian()) {
            payloadHex = DecoderUtil.convertHexToBigEndianOrdering(payloadHex, startBit, numberOfBits);
            startBit = 0;
        }

        if (uplinkConfiguration.isBcd()) {
            if (uplinkConfiguration.isSigned()) {
                value = (double) DecoderUtil.extractSignedBCDFromHex(payloadHex, startBit, numberOfBits);
            } else {
                value = (double) DecoderUtil.extractBCDFromHex(payloadHex, startBit, numberOfBits);
            }
        } else if (uplinkConfiguration.isSigned()) {
            value = (double) DecoderUtil.extractSignedDecimalFromHex(payloadHex, startBit, numberOfBits);
        } else {
            value = (double) DecoderUtil.extractDecimalFromHex(payloadHex, startBit, numberOfBits);
        }

        value = DecoderUtil.multiply(value, multiplication);
        value = DecoderUtil.offset(value, offset);

        return value;
    }
}
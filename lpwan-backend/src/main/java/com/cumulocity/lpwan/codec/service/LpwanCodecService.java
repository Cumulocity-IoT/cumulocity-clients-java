/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.codec.service;

import c8y.Command;
import com.cumulocity.lpwan.codec.exception.LpwanCodecServiceException;
import com.cumulocity.lpwan.devicetype.model.DeviceType;
import com.cumulocity.lpwan.payload.uplink.model.UplinkMessage;
import com.cumulocity.microservice.context.inject.TenantScope;
import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;
import com.cumulocity.microservice.lpwan.codec.decoder.model.LpwanDecoderInputData;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderInputData;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderResult;
import com.cumulocity.microservice.lpwan.codec.model.DeviceCommand;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;
import com.cumulocity.microservice.lpwan.codec.model.LpwanCodecDetails;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;


@Slf4j
@Service
@TenantScope
public class LpwanCodecService {

    private MicroserviceSubscriptionsService subscriptionsService;

    private RestConnector restConnector;

    private InventoryApi inventoryApi;

    private WebClient webClient;

    @Autowired
    public LpwanCodecService(MicroserviceSubscriptionsService subscriptionsService, RestConnector restConnector, InventoryApi inventoryApi) {
        this.subscriptionsService = subscriptionsService;
        this.restConnector = restConnector;
        this.inventoryApi = inventoryApi;
        this.webClient = WebClientFactory.builder()
                .timeout(WebClientFactory.DEFAULT_TIMEOUT_IN_MILLIS)
                .baseUrl(restConnector.getPlatformParameters().getHost())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Performs the decoding of the input payload
     *
     * @param deviceType represents the DeviceType of the device
     * @param source ManagedObjectRepresentation of the  device
     * @param uplinkMessage represents the payload to be decoded
     * @return DecoderResult represents the output that carries the measurement(s)/event(s)/alarm(s) to be created and/or the managed object properties to be updated.
     * @throws LpwanCodecServiceException
     * @see LpwanCodecServiceException {@link com.cumulocity.lpwan.codec.exception.LpwanCodecServiceException}
     */
    public DecoderResult decode(DeviceType deviceType, ManagedObjectRepresentation source, UplinkMessage uplinkMessage) throws LpwanCodecServiceException {
        LpwanCodecDetails lpwanCodecDetails = deviceType.getLpwanCodecDetails();
        try {
            lpwanCodecDetails.validate();
        } catch (IllegalArgumentException e) {
            throw new LpwanCodecServiceException(String.format("'c8y_LpwanCodecDetails' fragment in the device type associated with device EUI '%s' is invalid.", uplinkMessage.getExternalId()), e);
        }

        DeviceInfo deviceInfo = new DeviceInfo(lpwanCodecDetails.getSupportedDevice().getDeviceManufacturer(), lpwanCodecDetails.getSupportedDevice().getDeviceModel(), lpwanCodecDetails.getSupportedDevice().getSupportedCommands());
        LpwanDecoderInputData decoderInputData = new LpwanDecoderInputData(
                source.getId().getValue(),
                uplinkMessage.getExternalId(),
                deviceInfo,
                uplinkMessage.getPayloadHex(),
                uplinkMessage.getFport(),
                uplinkMessage.getDateTime().getMillis()
        );

        String codecServiceContextPath = lpwanCodecDetails.getCodecServiceContextPath();
        log.debug("Invoking the LPWAN /decode service with context path '{}' with input \n{}", codecServiceContextPath, decoderInputData);

        String authentication = subscriptionsService.getCredentials(subscriptionsService.getTenant()).get()
                .toCumulocityCredentials().getAuthenticationString();
        try {
            Mono<DecoderResult> decoderResult = webClient.post()
                    .uri("/service/" + codecServiceContextPath + "/decode")
                    .header(HttpHeaders.AUTHORIZATION, authentication)
                    .body(Mono.just(decoderInputData), LpwanDecoderInputData.class)
                    .retrieve()
                    .bodyToMono(DecoderResult.class);
            DecoderResult result = decoderResult.block(WebClientFactory.DEFAULT_TIMEOUT_IN_MILLIS);
            log.debug("Successfully invoked the LPWAN /decode service with context path '{}'. Returned \n{}", codecServiceContextPath, result);
            return result;
        } catch (Exception e) {
            String errorMessage = String.format("Error invoking the LPWAN /decode service with context path '%s'", codecServiceContextPath);
            log.error(errorMessage, e);
            throw new LpwanCodecServiceException(errorMessage, e);
        }
    }

    /**
     * Performs the encoding of the device command to be executed
     *
     * @param deviceType represents the DeviceType of the device
     * @param source ManagedObjectRepresentation of the  device
     * @param deviceEui represents the external ID of the device
     * @param operation the OperationRepresentation containing the device command to be encoded
     * @return LpwanEncoderResult represents the output that carries the encoded hexadecimal command to be executed and/or the accompanying properties like fport
     * @throws LpwanCodecServiceException
     * @see LpwanCodecServiceException {@link com.cumulocity.lpwan.codec.exception.LpwanCodecServiceException}
     */
    public LpwanEncoderResult encode(DeviceType deviceType, ManagedObjectRepresentation source, String deviceEui, OperationRepresentation operation) throws LpwanCodecServiceException {
        LpwanCodecDetails lpwanCodecDetails = deviceType.getLpwanCodecDetails();
        try {
            lpwanCodecDetails.validate();
        } catch (IllegalArgumentException e) {
            throw new LpwanCodecServiceException(String.format("'c8y_LpwanCodecDetails' fragment in the device type associated with device EUI '%s' is invalid.", deviceEui), e);
        }

        String codecServiceContextPath = lpwanCodecDetails.getCodecServiceContextPath();
        DeviceInfo deviceInfo = new DeviceInfo(lpwanCodecDetails.getSupportedDevice().getDeviceManufacturer(), lpwanCodecDetails.getSupportedDevice().getDeviceModel(), lpwanCodecDetails.getSupportedDevice().getSupportedCommands());

        String commandName = getCommandName(operation);
        String commandData = getCommandData(operation);
        LpwanEncoderInputData encoderInputData = new LpwanEncoderInputData(source.getId().getValue(), deviceEui, deviceInfo, commandName, commandData);
        log.debug("Invoking the LPWAN /encode service with context path '{}' with input \n{}", codecServiceContextPath, encoderInputData);
        
        String authentication = subscriptionsService.getCredentials(subscriptionsService.getTenant()).get()
                .toCumulocityCredentials().getAuthenticationString();
        try {
            Mono<LpwanEncoderResult> encoderResult = webClient.post()
                    .uri("/service/" + codecServiceContextPath + "/encode")
                    .header(HttpHeaders.AUTHORIZATION, authentication)
                    .body(Mono.just(encoderInputData), LpwanEncoderInputData.class)
                    .retrieve()
                    .bodyToMono(LpwanEncoderResult.class);
            LpwanEncoderResult result = encoderResult.block(WebClientFactory.DEFAULT_TIMEOUT_IN_MILLIS);
            log.debug("Successfully invoked the LPWAN /encode service with context path '{}'. Returned \n{}", codecServiceContextPath, result);
            return result;
        } catch (Exception e) {
            String errorMessage = String.format("Error invoking the LPWAN /encode service with context path '%s', for encoding the command '%s'", codecServiceContextPath, commandName);
            log.error(errorMessage, e);
            throw new LpwanCodecServiceException(errorMessage, e);
        }
    }
    
    private String getCommandName(OperationRepresentation operation) {
        String commandName = (String) operation.get("description");
        if (!Strings.isNullOrEmpty(commandName)) {
            int indexOfColon = commandName.indexOf(':');
            if (indexOfColon >= 0) {
                commandName = commandName.substring(indexOfColon + 1);
            }
        }
        return commandName.trim();
    }

    private String getCommandData(OperationRepresentation operation) {
        Command cmd = operation.get(Command.class);
        if (cmd != null) {
            return cmd.getText();
        }
        return null;
    }

    /**
     * Check if the device command is generated by the codec
     *
     * @param operation the OperationRepresentation containing the device command to be encoded
     * @param deviceType represents the DeviceType of the device
     * @return boolean
     */
    public boolean isCodecGeneratedCommand(OperationRepresentation operation, DeviceType deviceType) {
        if (Objects.isNull(deviceType.getLpwanCodecDetails())) {
            return false;
        }
        String commandName = getCommandName(operation);
        if (!Strings.isNullOrEmpty(commandName)) {
            Set<DeviceCommand> supportedCommands = deviceType.getLpwanCodecDetails().getSupportedDevice().getSupportedCommands();
            return Objects.nonNull(supportedCommands) && supportedCommands.contains(new DeviceCommand(commandName, null, null));
        }
        return false;
    }
}

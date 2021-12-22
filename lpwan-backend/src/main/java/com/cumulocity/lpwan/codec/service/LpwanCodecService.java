package com.cumulocity.lpwan.codec.service;

import c8y.Command;
import com.cumulocity.lpwan.codec.exception.LpwanCodecServiceException;
import com.cumulocity.lpwan.devicetype.model.DeviceType;
import com.cumulocity.lpwan.payload.exception.PayloadDecodingFailedException;
import com.cumulocity.lpwan.payload.uplink.model.UplinkMessage;
import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;
import com.cumulocity.microservice.customencoders.api.model.EncoderResult;
import com.cumulocity.microservice.lpwan.codec.decoder.model.LpwanDecoderInputData;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderInputData;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderResult;
import com.cumulocity.microservice.lpwan.codec.model.DeviceCommand;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;
import com.cumulocity.microservice.lpwan.codec.model.LpwanCodecDetails;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;


@Slf4j
@Service
public class LpwanCodecService {
    @Autowired
    private MicroserviceSubscriptionsService subscriptionsService;

    @Autowired
    private InventoryApi inventoryApi;

    private WebClient webClient;

    public LpwanCodecService() {
        this.webClient = WebClientFactory.builder()
                .timeout(WebClientFactory.DEFAULT_TIMEOUT_IN_MILLIS)
                .baseUrl(System.getenv("C8Y_BASEURL"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public DecoderResult decode(DeviceType deviceType, ManagedObjectRepresentation source, UplinkMessage uplinkMessage) throws LpwanCodecServiceException {
        LpwanCodecDetails lpwanCodecDetails = deviceType.getLpwanCodecDetails();
        try {
            lpwanCodecDetails.validate();
        } catch (IllegalArgumentException e) {
            throw new LpwanCodecServiceException(String.format("'c8y_LpwanCodecDetails' fragment in the device type associated with device EUI '%s' is invalid.", uplinkMessage.getExternalId()), e);
        }

        DeviceInfo deviceInfo = new DeviceInfo(lpwanCodecDetails.getSupportedDevice().getManufacturer(), lpwanCodecDetails.getSupportedDevice().getModel(), lpwanCodecDetails.getSupportedDevice().getSupportedCommands());
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

    public LpwanEncoderResult encode(DeviceType deviceType, ManagedObjectRepresentation source, String deviceEui, OperationRepresentation operation) throws LpwanCodecServiceException {
        LpwanCodecDetails lpwanCodecDetails = deviceType.getLpwanCodecDetails();
        try {
            lpwanCodecDetails.validate();
        } catch (IllegalArgumentException e) {
            throw new LpwanCodecServiceException(String.format("'c8y_LpwanCodecDetails' fragment in the device type associated with device EUI '%s' is invalid.", deviceEui), e);
        }

        String codecServiceContextPath = lpwanCodecDetails.getCodecServiceContextPath();
        DeviceInfo deviceInfo = new DeviceInfo(lpwanCodecDetails.getSupportedDevice().getManufacturer(), lpwanCodecDetails.getSupportedDevice().getModel(), lpwanCodecDetails.getSupportedDevice().getSupportedCommands());

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
        return commandName;
    }

    private String getCommandData(OperationRepresentation operation) {
        Command cmd = operation.get(Command.class);
        if (cmd != null) {
            return cmd.getText();
        }
        return null;
    }

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

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.microservice.lpwan.codec.rest;

import com.cumulocity.microservice.customdecoders.api.exception.DecoderServiceException;
import com.cumulocity.microservice.customdecoders.api.exception.InvalidInputDataException;
import com.cumulocity.microservice.customdecoders.api.model.DecoderInputData;
import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;
import com.cumulocity.microservice.customdecoders.api.service.DecoderService;
import com.cumulocity.microservice.customencoders.api.exception.EncoderServiceException;
import com.cumulocity.microservice.customencoders.api.exception.InvalidCommandDataException;
import com.cumulocity.microservice.customencoders.api.model.EncoderInputData;
import com.cumulocity.microservice.customencoders.api.model.EncoderResult;
import com.cumulocity.microservice.customencoders.api.service.EncoderService;
import com.cumulocity.model.idtype.GId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Objects;


/**
 * The <b>CodecController</b> is a rest controller that defines the endpoints.
 */
@RestController
@Slf4j
public class CodecController {

    @Autowired(required = false)
    private DecoderService decoderService;

    @Autowired(required = false)
    private EncoderService encoderService;

    /**
     * This REST API should expose '/decode' endpoint
     *
     * @param inputData A non-null input parameter that is carries the payload to be decoded along with other supporting elements.
     * @return DecoderResult represents the output that carries the measurement(s)/event(s)/alarm(s) to be created and/or the managed object properties to be updated.
     * @throws DecoderServiceException DecoderServiceException
     * @see DecoderServiceException {@link com.cumulocity.microservice.customdecoders.api.exception.DecoderServiceException}
     */
    @PostMapping(value = "/decode", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @NotNull DecoderResult decode(@RequestBody @NotNull DecoderInputData inputData) throws DecoderServiceException {
        if(decoderService == null) {
            Exception exception = new UnsupportedOperationException("No implementation provided for the DecoderService");
            throw new DecoderServiceException(exception, exception.getMessage(), DecoderResult.empty());
        }

        try {
            if(Objects.isNull(inputData)) {
                throw new IllegalArgumentException("Decoder is invoked with null input data.");
            }
            log.debug("Decoder is invoked with the following input data \\n {}", inputData);

            return decoderService.decode(inputData.getValue(), GId.asGId(inputData.getSourceDeviceId()), inputData.getArgs());
        } catch (IllegalArgumentException e) {
            log.error("Decoder failed as it received invalid input.", e);
            throw new InvalidInputDataException(e, e.getMessage(), DecoderResult.empty());
        }
    }

    /**
     * This REST API should expose '/encode' endpoint
     *
     * @param inputData A non-null input parameter that carries the command to be encoded
     * @return EncoderResult represents the output that carries the encoded hexadecimal command to be executed and/or the accompanying properties like fport
     * @throws EncoderServiceException EncoderServiceException
     * @see EncoderServiceException {@link com.cumulocity.microservice.customencoders.api.exception.EncoderServiceException}
     */
    @PostMapping(value = "/encode", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @NotNull EncoderResult encode(@RequestBody @NotNull EncoderInputData inputData) throws EncoderServiceException {
        if(encoderService == null) {
            Exception exception = new UnsupportedOperationException("No implementation provided for the EncoderService");
            throw new EncoderServiceException(exception, exception.getMessage(), EncoderResult.empty());
        }

        try {
            if(Objects.isNull(inputData)) {
                throw new IllegalArgumentException("Encoder is invoked with null input data.");
            }
            log.debug("Encoder is invoked with the following input data \\n {}", inputData);
            return encoderService.encode(inputData);
        } catch (IllegalArgumentException e) {
            log.error("Encoder failed as it received invalid input.", e);
            throw new InvalidCommandDataException(e, e.getMessage(), EncoderResult.empty());
        }
    }
}

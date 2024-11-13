/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.handler;

import com.cumulocity.lpwan.exception.InputDataValidationException;
import com.cumulocity.lpwan.exception.LpwanServiceException;
import com.cumulocity.rest.representation.ErrorDetails;
import com.cumulocity.rest.representation.ErrorMessageRepresentation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.cumulocity.rest.representation.CumulocityMediaType.ERROR_MESSAGE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LpwanExceptionsHandlerTest {

    @Test
    void doHandleException_InvalidInputDataException() {
        InputDataValidationException exception = new InputDataValidationException("LnsConnectionService failed with missing input parameters.", new IllegalArgumentException("Missing input parameters."));
        ResponseEntity<ErrorMessageRepresentation> responseEntity = new LpwanExceptionsHandler().handleInputDataValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getHeaders().getContentType());
        assertEquals(ERROR_MESSAGE_TYPE, responseEntity.getHeaders().getContentType().toString());

        assertNotNull(responseEntity.getBody());
        ErrorMessageRepresentation errorMessageRepresentation = responseEntity.getBody();
        assertEquals("Lpwan Backend Error", errorMessageRepresentation.getError());
        assertEquals(exception.getMessage(), errorMessageRepresentation.getMessage());

        ErrorDetails details = errorMessageRepresentation.getDetails();
        assertNotNull(details);
        assertEquals(InputDataValidationException.class.getCanonicalName(), details.getExpectionClass());
        assertEquals(exception.getMessage(), details.getExceptionMessage());
    }

    @Test
    void doHandleException_LpwanServiceException() {
        LpwanServiceException exception = new LpwanServiceException("LnsConnectionService failed with internal service error.", new Exception("Some internal error."));
        ResponseEntity<ErrorMessageRepresentation> responseEntity = new LpwanExceptionsHandler().handleLpwanServiceException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getHeaders().getContentType());
        assertEquals(ERROR_MESSAGE_TYPE, responseEntity.getHeaders().getContentType().toString());

        assertNotNull(responseEntity.getBody());
        ErrorMessageRepresentation errorMessageRepresentation = responseEntity.getBody();
        assertEquals("Lpwan Backend Error", errorMessageRepresentation.getError());
        assertEquals(exception.getMessage(), errorMessageRepresentation.getMessage());

        ErrorDetails details = errorMessageRepresentation.getDetails();
        assertNotNull(details);
        assertEquals(LpwanServiceException.class.getCanonicalName(), details.getExpectionClass());
        assertEquals(exception.getMessage(), details.getExceptionMessage());
    }

    @Test
    void doHandleException_IllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input parameter message");
        ResponseEntity<ErrorMessageRepresentation> responseEntity = new LpwanExceptionsHandler().handleIllegalArgumentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getHeaders().getContentType());
        assertEquals(ERROR_MESSAGE_TYPE, responseEntity.getHeaders().getContentType().toString());

        assertNotNull(responseEntity.getBody());
        ErrorMessageRepresentation errorMessageRepresentation = responseEntity.getBody();
        assertEquals("Lpwan Backend Error", errorMessageRepresentation.getError());
        assertEquals(exception.getMessage(), errorMessageRepresentation.getMessage());

        ErrorDetails details = errorMessageRepresentation.getDetails();
        assertNotNull(details);
        assertEquals(IllegalArgumentException.class.getCanonicalName(), details.getExpectionClass());
        assertEquals(exception.getMessage(), details.getExceptionMessage());
    }

    @Test
    void doHandleException_UnsupportedOperationException() {
        UnsupportedOperationException exception = new UnsupportedOperationException("Operation not supported");
        ResponseEntity<ErrorMessageRepresentation> responseEntity = new LpwanExceptionsHandler().handleUnsupportedOperationException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getHeaders().getContentType());
        assertEquals(ERROR_MESSAGE_TYPE, responseEntity.getHeaders().getContentType().toString());

        assertNotNull(responseEntity.getBody());
        ErrorMessageRepresentation errorMessageRepresentation = responseEntity.getBody();
        assertEquals("Lpwan Backend Error", errorMessageRepresentation.getError());
        assertEquals(exception.getMessage(), errorMessageRepresentation.getMessage());

        ErrorDetails details = errorMessageRepresentation.getDetails();
        assertNotNull(details);
        assertEquals(UnsupportedOperationException.class.getCanonicalName(), details.getExpectionClass());
        assertEquals(exception.getMessage(), details.getExceptionMessage());
    }
}
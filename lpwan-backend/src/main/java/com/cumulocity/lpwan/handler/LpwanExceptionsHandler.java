/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.handler;

import com.cumulocity.lpwan.exception.InputDataValidationException;
import com.cumulocity.lpwan.exception.LpwanServiceException;
import com.cumulocity.lpwan.exception.LpwanUserNotFoundException;
import com.cumulocity.rest.representation.ErrorDetails;
import com.cumulocity.rest.representation.ErrorMessageRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.cumulocity.rest.representation.CumulocityMediaType.ERROR_MESSAGE_TYPE;

/**
 * The <b>LpwanExceptionsHandler</b> is a custom exception handler.
 *
 */

@ControllerAdvice
@Slf4j
public class LpwanExceptionsHandler {

    /**
     * This method handles the custom <b>{@link com.cumulocity.lpwan.exception.LpwanServiceException}</b>.
     *
     * @param exception       represents the exception
     * @return ResponseEntity <code>HttpStatus.INTERNAL_SERVER_ERROR</code>
     * @see com.cumulocity.lpwan.exception.LpwanServiceException {@link com.cumulocity.lpwan.exception.LpwanServiceException}
     */
    @ExceptionHandler(value = LpwanServiceException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessageRepresentation> handleLpwanServiceException(LpwanServiceException exception) {
        log.error(exception.getMessage(), exception, exception.getUrl());
        return buildErrorResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * This method handles the custom <b>{@link com.cumulocity.lpwan.exception.InputDataValidationException}</b>.
     *
     * @param exception       represents the InputDataValidationException
     * @return ResponseEntity <code>HttpStatus.BAD_REQUEST</code>
     * @see com.cumulocity.lpwan.exception.InputDataValidationException {@link com.cumulocity.lpwan.exception.InputDataValidationException}
     */
    @ExceptionHandler(value = InputDataValidationException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessageRepresentation> handleInputDataValidationException(InputDataValidationException exception) {
        log.error(exception.getMessage(), exception);
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

    /**
     * This method handles the custom <b>{@link com.cumulocity.lpwan.exception.LpwanUserNotFoundException}</b>.
     *
     * @param exception       represents the LpwanUserNotFoundException
     * @return ResponseEntity <code>HttpStatus.NOT_FOUND</code>
     * @see com.cumulocity.lpwan.exception.LpwanUserNotFoundException {@link com.cumulocity.lpwan.exception.LpwanUserNotFoundException}
     */
    @ExceptionHandler(value = LpwanUserNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessageRepresentation> handleLpwanUserNotFoundException(LpwanUserNotFoundException exception) {
        log.error(exception.getMessage(), exception);
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    /**
     * This method handles the <b>UnsupportedOperationException</b>.
     *
     * @param exception       represents the UnsupportedOperationException
     * @return ResponseEntity <code>HttpStatus.INTERNAL_SERVER_ERROR</code>
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/UnsupportedOperationException.html">UnsupportedOperationException</a>
     */
    @ExceptionHandler(value = UnsupportedOperationException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessageRepresentation> handleUnsupportedOperationException(UnsupportedOperationException exception) {
        log.error(exception.getMessage(), exception);
        return buildErrorResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * This method handles the <b>IllegalArgumentException</b>.
     *
     * @param exception       represents the IllegalArgumentException
     * @return ResponseEntity <code>HttpStatus.BAD_REQUEST</code>
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/IllegalArgumentException.html">IllegalArgumentException</a>
     */
    @ExceptionHandler(value = IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessageRepresentation> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error(exception.getMessage(), exception);
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

    /**
     * This method handles the <b>Exception</b>.
     *
     * @param exception       represents the Exception
     * @return ResponseEntity <code>HttpStatus.INTERNAL_SERVER_ERROR</code>
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html">Exception</a>
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorMessageRepresentation> handleException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return buildErrorResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    protected ResponseEntity<ErrorMessageRepresentation> buildErrorResponse(Throwable exception, HttpStatus status) {
        ErrorMessageRepresentation representation = buildErrorMessageRepresentation(exception);
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType(ERROR_MESSAGE_TYPE)).body(representation);
    }

    private ErrorMessageRepresentation buildErrorMessageRepresentation(Throwable exception) {
        ErrorMessageRepresentation representation = new ErrorMessageRepresentation();
        representation.setError("Lpwan Backend Error");
        representation.setMessage(exception.getMessage());
        representation.setDetails(buildErrorDetails(exception));
        if(exception instanceof LpwanServiceException) {
            representation.set(((LpwanServiceException) exception).getUrl(), "URL");
        }
        return representation;
    }

    private ErrorDetails buildErrorDetails(Throwable exception) {
        ErrorDetails errorDetails = new ErrorDetails();
        errorDetails.setExpectionClass(exception.getClass().getCanonicalName());
        errorDetails.setExceptionMessage(exception.getMessage());
        return errorDetails;
    }
}

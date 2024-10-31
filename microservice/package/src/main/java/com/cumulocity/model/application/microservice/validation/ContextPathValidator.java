package com.cumulocity.model.application.microservice.validation;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContextPathValidator implements ConstraintValidator<ValidContextPath, String> {

    public void initialize(ValidContextPath constraint) {
        //do nothing
    }

    public boolean isValid(String contextPath, ConstraintValidatorContext context) {
        return !StringUtils.contains(contextPath, StringUtils.SPACE);
    }
}

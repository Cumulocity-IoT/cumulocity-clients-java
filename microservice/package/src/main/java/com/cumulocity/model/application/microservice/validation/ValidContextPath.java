package com.cumulocity.model.application.microservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;


@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = ContextPathValidator.class
)
public @interface ValidContextPath {
    String message() default "Context Path must not contain a space character.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
package com.cumulocity.model.application.microservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = MinDataSizeValidator.class
)
public @interface MinDataSize {
    String value();

    String message() default "The data size should be at least {value} ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

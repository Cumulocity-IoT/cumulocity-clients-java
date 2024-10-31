package com.cumulocity.model.application.microservice.validation;

import com.cumulocity.model.DataSize;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Range;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinDataSizeValidator implements ConstraintValidator<MinDataSize, String> {
    private DataSize minimum;
    private Optional<DataSize> maximum;

    @Override
    public void initialize(MinDataSize valid) {
        minimum = DataSize.parse(valid.value());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        final Optional<DataSize> parsed = DataSize.tryParse(value);
        return Strings.isNullOrEmpty(value) || !parsed.isPresent() || inRange(parsed);
    }

    private boolean inRange(Optional<DataSize> parsed) {
        return Range.atLeast(minimum).contains(parsed.get());
    }
}

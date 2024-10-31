package com.cumulocity.model.application.microservice.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProbeValidator implements ConstraintValidator<ValidProbe, Probe> {
    @Override
    public void initialize(ValidProbe validProbe) {

    }

    @Override
    public boolean isValid(Probe probe, ConstraintValidatorContext constraintValidatorContext) {
        if (probe == null) return true;
        // Check if actions specified. Probe is valid only when only single action is defined.
        final boolean hasExec = probe.getExec() != null;
        final boolean hasHttpGet = probe.getHttpGet() != null;
        final boolean hasTcp = probe.getTcpSocket() != null;
        return  ((hasExec ^ hasHttpGet) ^ hasTcp);
    }
}

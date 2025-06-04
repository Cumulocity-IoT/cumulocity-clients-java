package com.cumulocity.sdk.client.notification2.exception;

import lombok.Getter;

public class Notifications2FieldInvalidException extends RuntimeException{
    @Getter private final String field;
    public Notifications2FieldInvalidException(String field, String value) {
        super(field + " '" + value + "' is invalid. It can only contain letters and digits");
        this.field = field;
    }
}

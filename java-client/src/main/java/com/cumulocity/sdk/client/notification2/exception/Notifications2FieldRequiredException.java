package com.cumulocity.sdk.client.notification2.exception;

import lombok.Getter;

public class Notifications2FieldRequiredException extends RuntimeException{
    @Getter private final String field;

    public Notifications2FieldRequiredException(String field) {
        super(field + " is required");
        this.field = field;
    }
}

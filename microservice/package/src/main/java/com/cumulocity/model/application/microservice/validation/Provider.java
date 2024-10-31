package com.cumulocity.model.application.microservice.validation;

import lombok.*;
import org.svenson.JSONProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Provider {

    @NotNull
    @Size(min = 1)
    private String name;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String domain;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private String support;
}

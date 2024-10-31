package com.cumulocity.model;

import lombok.*;
import org.svenson.JSONProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationOption {

    @NotNull
    @Size(min = 1)
    private String key;
    @NotNull
    @Size(min = 1)
    private String defaultValue;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private boolean editable;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private ConfigurationOptionSchema valueSchema;
    @Getter(onMethod_ = @JSONProperty(ignoreIfNull = true))
    private boolean inheritFromOwner = true;
}

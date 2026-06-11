package com.cumulocity.sdk.client.notification2.config;

import com.cumulocity.sdk.client.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Notifications2Properties {
    /**
     * Base URL for Notifications 2.0.
     * From property <b>C8Y.notifications2.websocketUrl</b>
     */
    @With
    private String websocketUrl;

    public boolean isDisabled() {
        return StringUtils.isBlank(this.websocketUrl);
    }
}

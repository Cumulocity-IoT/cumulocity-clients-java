package com.cumulocity.sdk.client;

import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.sdk.client.notification2.config.Notifications2Properties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

import static java.util.Optional.ofNullable;

@NoArgsConstructor(staticName = "platform")
@AllArgsConstructor
public class PlatformBuilder {
    @With
    private String baseUrl;
    @With
    private CumulocityCredentials credentials;
    @With
    private String proxyHost;
    @With
    private Integer proxyPort;
    @With
    private String tfaToken;
    @With
    private ResponseMapper responseMapper;
    @With
    private boolean forceInitialHost;
    @With
    private Notifications2Properties notifications2;

    public Platform build() {
        return configure(new PlatformImpl(baseUrl, buildCredentials()));
    }

    private PlatformImpl configure(PlatformImpl platform) {
        if (proxyHost != null && !proxyHost.isEmpty()) {
            platform.setProxyHost(proxyHost);
        }
        if (proxyPort != null && proxyPort > 0) {
            platform.setProxyPort(proxyPort);
        }
        if (responseMapper != null) {
            platform.setResponseMapper(responseMapper);
        }
        platform.setTfaToken(tfaToken);
        platform.setForceInitialHost(forceInitialHost);
        platform.setNotifications2(ofNullable(notifications2).orElseGet(Notifications2Properties::new));
        return platform;
    }

    private CumulocityCredentials buildCredentials() {
        return credentials;
    }
}

package com.cumulocity.sdk.client;

import com.cumulocity.model.authentication.CumulocityCredentials;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@Wither
@NoArgsConstructor(staticName = "platform")
@AllArgsConstructor
public class PlatformBuilder {
    private String baseUrl;
    private CumulocityCredentials credentials;
    private String proxyHost;
    private Integer proxyPort;
    private String tfaToken;
    private ResponseMapper responseMapper;
    private boolean forceInitialHost;
    private String baseWebSocketUrl;

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
        platform.setBaseWebSocketUrl(baseWebSocketUrl);
        return platform;
    }

    private CumulocityCredentials buildCredentials() {
        return credentials;
    }
}

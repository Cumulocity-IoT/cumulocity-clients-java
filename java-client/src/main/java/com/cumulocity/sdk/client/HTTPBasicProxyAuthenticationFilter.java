package com.cumulocity.sdk.client;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class HTTPBasicProxyAuthenticationFilter implements ClientRequestFilter {

    private static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    private final String authentication;

    /**
     * Creates a new HTTP Proxy-Authorization filter using provided username
     * and password credentials
     *
     * @param username a username
     * @param password a password
     */
    public HTTPBasicProxyAuthenticationFilter(final String username, final String password) {
        String credentials = username + ":" + password;
        authentication = "Basic " + new String(Base64.getEncoder().encode(credentials.getBytes(StandardCharsets.US_ASCII)), StandardCharsets.US_ASCII);
    }

    @Override
    public void filter(ClientRequestContext cr) throws IOException {
        if (!cr.getHeaders().containsKey(PROXY_AUTHORIZATION)) {
            cr.getHeaders().putSingle(PROXY_AUTHORIZATION, authentication);
        }
    }
}

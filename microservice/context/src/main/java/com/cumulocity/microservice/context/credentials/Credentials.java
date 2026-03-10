package com.cumulocity.microservice.context.credentials;

public interface Credentials {
    String getUsername();
    String getTenant();
    String getOAuthAccessToken();
    String getXsrfToken();
    String getPassword();
    String getTfaToken();
    String getAppKey();
    String getOrigin();
}


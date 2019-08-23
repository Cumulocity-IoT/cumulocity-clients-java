package com.cumulocity.microservice.security.token;

import com.cumulocity.rest.representation.user.CurrentUserRepresentation;
import com.cumulocity.rest.representation.user.UserMediaType;
import com.cumulocity.sdk.client.CumulocityAuthenticationFilter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sun.jersey.client.apache.ApacheHttpClient;

public class CumulocityOAuthUserDetails {
    ApacheHttpClient client;
    String baseUrl;

    public static final CumulocityOAuthUserDetails from(String baseUrl, JwtTokenAuthentication jwtTokenAuthentication) {
        ApacheHttpClient client = new ApacheHttpClient();
        if (jwtTokenAuthentication != null) {
            if (jwtTokenAuthentication.getCredentials() instanceof JwtAndXsrfTokenCredentials) {
                JwtAndXsrfTokenCredentials credentials = (JwtAndXsrfTokenCredentials) jwtTokenAuthentication.getCredentials();
                client.addFilter(new CumulocityAuthenticationFilter(
                        credentials.getJwt().getEncoded()
                        , credentials.getXsrfToken()
                ));
            } else {
                client.addFilter(new CumulocityAuthenticationFilter(
                        ((JwtCredentials) jwtTokenAuthentication.getCredentials()).getJwt().getEncoded(),
                        null
                ));
            }
        }
        return new CumulocityOAuthUserDetails(baseUrl, client);
    }

    public CumulocityOAuthUserDetails(String baseUrl, ApacheHttpClient client) {
        this.client = client;
        this.baseUrl = baseUrl;
    }

    protected CurrentUserRepresentation getCurrentUser() {
        return client.resource(baseUrl + "/user/currentUser")
                .accept(UserMediaType.CURRENT_USER)
                .get(CurrentUserRepresentation.class);
    }

    protected String getTenantName() {
        SimplifiedCurrentTenantRepresentation currentTenantRepresentation = client.resource(baseUrl + "/tenant/currentTenant")
                .accept(UserMediaType.CURRENT_TENANT)
                .get(SimplifiedCurrentTenantRepresentation.class);
        return currentTenantRepresentation.name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class SimplifiedCurrentTenantRepresentation {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

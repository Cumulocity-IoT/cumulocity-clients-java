package com.cumulocity.model.authentication;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import org.apache.commons.lang3.StringUtils;

/**
 * Used to create CumulocityCredentials objects,
 * if given accessToken and xsrfToken,
 * then {@link CumulocityOAuthCredentials} is created
 * else
 * {@link CumulocityBasicCredentials} is created
 */
@With
@NoArgsConstructor
@AllArgsConstructor
public class CumulocityCredentialsFactory {

    private String tenant;
    private String username;
    private String password;
    private String oAuthAccessToken;
    private String xsrfToken;
    private String applicationKey;
    private String requestOrigin;
    private AuthenticationMethod authenticationMethod;

    public CumulocityCredentials getCredentials() {
        if (StringUtils.isNotEmpty(oAuthAccessToken)) {
            return CumulocityOAuthCredentials.builder()
                    .oAuthAccessToken(oAuthAccessToken)
                    .xsrfToken(xsrfToken)
                    .tenantId(tenant)
                    .username(username)
                    .applicationKey(applicationKey)
                    .requestOrigin(requestOrigin)
                    .authenticationMethod(authenticationMethod != null ? authenticationMethod :
                            xsrfToken != null ? AuthenticationMethod.COOKIE : AuthenticationMethod.HEADER)
                    .build();
        } else {
            return CumulocityBasicCredentials.builder()
                    .tenantId(tenant)
                    .username(username)
                    .password(password)
                    .applicationKey(applicationKey)
                    .requestOrigin(requestOrigin)
                    .build();
        }
    }
}

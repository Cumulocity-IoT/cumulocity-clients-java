package com.cumulocity.model.authentication;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.codec.binary.Base64;
import org.svenson.JSONParser;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@EqualsAndHashCode
@ToString(of = {"oAuthAccessToken", "xsrfToken", "applicationKey", "requestOrigin"})
public class CumulocityOAuthCredentials implements CumulocityCredentials {

    public static final String CUMULOCITY_USER_ID_CLAIM = "sub";
    public static final String TENANT_ID_CLAIM = "ten";

    private final String oAuthAccessToken;
    private final Map<String, Object> accessTokenHeader = new HashMap<>();
    private final Map<String, Object> accessTokenClaims = new HashMap<>();

    @Getter
    private String xsrfToken;
    @Setter
    @Getter
    private String applicationKey;
    @Setter
    @Getter
    private String requestOrigin;
    @Setter
    @Getter
    private AuthenticationMethod authenticationMethod;
    @Setter
    private String tenantId;
    @Setter
    private String username;

    public CumulocityOAuthCredentials(String oAuthAccessToken, String xsrfToken, String applicationKey, String requestOrigin, AuthenticationMethod authenticationMethod) {
        this.oAuthAccessToken = oAuthAccessToken;
        this.xsrfToken = xsrfToken;
        this.applicationKey = applicationKey;
        this.requestOrigin = requestOrigin;
        this.authenticationMethod = authenticationMethod != null ? authenticationMethod : AuthenticationMethod.COOKIE;
        parseAccessToken();
    }

    @lombok.Builder
    private static CumulocityOAuthCredentials create(
            String oAuthAccessToken,
            String xsrfToken,
            String applicationKey,
            String requestOrigin,
            AuthenticationMethod authenticationMethod,
            String tenantId,
            String username
    ) {
        CumulocityOAuthCredentials oAuthCredentials = new CumulocityOAuthCredentials(oAuthAccessToken, xsrfToken, applicationKey, requestOrigin, authenticationMethod);
        oAuthCredentials.tenantId = tenantId;
        oAuthCredentials.username = username;
        return oAuthCredentials;
    }

    private void parseAccessToken() {
        String[] headerAndClaims = oAuthAccessToken.split("\\.");
        accessTokenHeader.putAll(parseAccessTokenFragment(headerAndClaims[0]));
        accessTokenClaims.putAll(parseAccessTokenFragment(headerAndClaims[1]));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAccessTokenFragment(String tokenFragment) {
        return JSONParser.defaultJSONParser().parse(Map.class, new String(Base64.decodeBase64(tokenFragment)));
    }

    @Override
    public String getAuthenticationString() {
        switch (authenticationMethod) {
            case COOKIE:
                return oAuthAccessToken;
            case HEADER:
                return "Bearer " + oAuthAccessToken;
        }
        return null;
    }

    @Override
    public String getUsername() {
        return ofNullable(accessTokenClaims.get(CUMULOCITY_USER_ID_CLAIM))
                .map(String::valueOf)
                .orElse(username);
    }

    @Override
    public String getTenantId() {
        return Stream.of(accessTokenClaims, accessTokenHeader)
                .filter(m -> m.containsKey(TENANT_ID_CLAIM))
                .map(m -> m.get(TENANT_ID_CLAIM))
                .map(String::valueOf)
                .findFirst()
                .orElse(tenantId);
    }

    @Override
    public CumulocityCredentials copy() {
        return CumulocityOAuthCredentials.builder()
                .oAuthAccessToken(getAuthenticationString())
                .xsrfToken(getXsrfToken())
                .requestOrigin(getRequestOrigin())
                .applicationKey(getApplicationKey())
                .authenticationMethod(getAuthenticationMethod())
                .tenantId(getTenantId())
                .username(getUsername())
                .build();
    }

    @Override
    public <T> T accept(CumulocityCredentialsVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getAccessToken() {
        return oAuthAccessToken;
    }
}

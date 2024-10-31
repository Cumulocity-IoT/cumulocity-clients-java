package com.cumulocity.microservice.security.filter.provider;

import com.cumulocity.microservice.context.credentials.UserCredentials;
import com.cumulocity.microservice.security.filter.util.HttpRequestUtils.AuthorizationHeader;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

import static com.cumulocity.microservice.security.filter.util.HttpRequestUtils.TFA_TOKEN_HEADER;
import static com.cumulocity.microservice.security.filter.util.HttpRequestUtils.XSRF_TOKEN_HEADER;
import static com.cumulocity.microservice.security.filter.util.HttpRequestUtils.X_CUMULOCITY_APPLICATION_KEY;
import static com.cumulocity.microservice.security.filter.util.HttpRequestUtils.authorizationHeader;
import static com.cumulocity.microservice.security.filter.util.HttpRequestUtils.hasAuthorizationHeader;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class HttpContextProvider implements PreAuthorizationContextProvider<HttpServletRequest> {
    @Override
    public UserCredentials get(HttpServletRequest request) {
        final String authorization = request.getHeader(AUTHORIZATION);
        final String applicationKey = request.getHeader(X_CUMULOCITY_APPLICATION_KEY);
        final String tfaToken = request.getHeader(TFA_TOKEN_HEADER);
        final AuthorizationHeader from = authorizationHeader(authorization);

        if (StringUtils.hasText(from.getTenant())) {
            return UserCredentials.builder()
                    .tenant(from.getTenant())
                    .username(from.getUsername())
                    .password(from.getPassword())
                    .oAuthAccessToken(obtainOAuthAccessToken(request))
                    .xsrfToken(request.getHeader(XSRF_TOKEN_HEADER))
                    .appKey(applicationKey)
                    .tfaToken(tfaToken)
                    .build();
        }
        return null;
    }

    private String obtainOAuthAccessToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        Optional<Cookie> cookieOptional = Arrays.stream(request.getCookies()).filter(cookie -> AUTHORIZATION.equalsIgnoreCase(cookie.getName())).findFirst();

        return cookieOptional.map(Cookie::getValue).orElse(null);
    }

    @Override
    public boolean supports(HttpServletRequest credentialSource) {
        return hasAuthorizationHeader(credentialSource);
    }
}

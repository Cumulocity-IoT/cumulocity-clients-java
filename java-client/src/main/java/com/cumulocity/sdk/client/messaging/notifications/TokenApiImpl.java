package com.cumulocity.sdk.client.messaging.notifications;

import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.reliable.notification.NotificationTokenRequestRepresentation;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.svenson.JSONParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
public class TokenApiImpl implements TokenApi {
    public static final CumulocityMediaType TOKEN_MEDIA_TYPE = new CumulocityMediaType("application", "json");

    public static final String TOKEN_REQUEST_URI = "notification2/token";
    public static final String UNSUBSCRIBE_REQUEST_URI = "notification2/unsubscribe";
    private static final String JWT_TOKEN_SPLIT = "\\.";
    private static final String TOPIC_SPLIT = "/";

    private final PlatformParameters platformParameters;
    private final RestConnector restConnector;

    @Override
    public Token create(NotificationTokenRequestRepresentation tokenRequest) throws IllegalArgumentException, SDKException {
        if (tokenRequest == null) {
            throw new IllegalArgumentException("Token claim is null");
        }

        return restConnector.post(
                getTokenRequestUri(),
                TOKEN_MEDIA_TYPE,
                TOKEN_MEDIA_TYPE,
                tokenRequest,
                Token.class
        );
    }

    @Override
    public TokenClaims verify(Token token) throws SDKException {
        return restConnector
                .get(getTokenRequestUri() + "?token=" + token.getTokenString(),
                        TOKEN_MEDIA_TYPE,
                        TokenClaims.class);
    }

    @Override
    public Token refresh(Token expiredToken) throws IllegalArgumentException, SDKException {
        if (expiredToken == null || expiredToken.getTokenString() == null) {
            throw new IllegalArgumentException("Expired token is null");
        }
        String claimsString = null;
        try {
            String[] tokenParts = expiredToken.getTokenString().split(JWT_TOKEN_SPLIT);
            claimsString = new String(Base64.getDecoder().decode(tokenParts[1]));
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException("Not a valid token");
        }

        TokenClaims parsedToken = JSONParser.defaultJSONParser().parse(TokenClaims.class, claimsString);

        String subscription = null;
        try {
            subscription = parsedToken.getTopic().split(TOPIC_SPLIT)[2];
        } catch (IndexOutOfBoundsException ie) {
            throw new IllegalArgumentException("Not a valid topic");
        }
        long expiry = parsedToken.getExp() - parsedToken.getIat();
        long validityPeriodMinutes = expiry / 60;

        return create(new NotificationTokenRequestRepresentation(
                parsedToken.getSubscriber(),
                subscription,
                validityPeriodMinutes,
                parsedToken.isShared(),
                parsedToken.isNonPersistent()));
    }

    @Override
    public void unsubscribe(Token token) throws SDKException {
        if (token == null || token.getTokenString() == null) {
            throw new IllegalArgumentException("token is null");
        }

        final Response response = restConnector.getClient().target(getTokenUnsubscribeUri())
                .queryParam("token", token.getTokenString())
                .request()
                .post(Entity.text(""));
        response.close();
    }

    private String getTokenRequestUri() {
        return platformParameters.getHost() + TOKEN_REQUEST_URI;
    }

    private String getTokenUnsubscribeUri() {
        return platformParameters.getHost() + UNSUBSCRIBE_REQUEST_URI;
    }
}

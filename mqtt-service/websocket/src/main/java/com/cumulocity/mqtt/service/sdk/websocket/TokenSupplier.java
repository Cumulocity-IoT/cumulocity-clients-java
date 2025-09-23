package com.cumulocity.mqtt.service.sdk.websocket;

import com.cumulocity.rest.representation.reliable.notification.NotificationTokenRequestRepresentation;
import com.cumulocity.sdk.client.messaging.notifications.Token;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;

import java.util.function.Supplier;

import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static java.util.concurrent.TimeUnit.MINUTES;

@Deprecated(forRemoval = true)
class TokenSupplier {

    private static final long TOKEN_EXPIRATION_IN_MINUTES = 1440;
    private static final long CACHE_EXPIRATION_IN_MINUTES = TOKEN_EXPIRATION_IN_MINUTES - 120;
    private static final String TOKEN_TYPE = "mqtt";

    private final Supplier<Token> supplier;

    TokenSupplier(final TokenApi tokenApi, final String topic, final String subscriber) {
        this.supplier = memoizeWithExpiration(() -> tokenApi.create(buildTokenRepresentation(topic, subscriber)), CACHE_EXPIRATION_IN_MINUTES, MINUTES);
    }

    public Token getToken() {
        return supplier.get();
    }

    private NotificationTokenRequestRepresentation buildTokenRepresentation(final String topic, final String subscriber) {
        final NotificationTokenRequestRepresentation representation = new NotificationTokenRequestRepresentation();
        representation.setSubscriber(subscriber);
        representation.setSubscription(topic);
        representation.setType(TOKEN_TYPE);
        representation.setSigned(true);
        representation.setExpiresInMinutes(TOKEN_EXPIRATION_IN_MINUTES);
        representation.setShared(false);
        representation.setNonPersistent(false);
        return representation;
    }

}

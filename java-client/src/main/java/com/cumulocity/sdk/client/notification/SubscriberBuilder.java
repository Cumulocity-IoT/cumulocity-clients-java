/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.cumulocity.sdk.client.notification;

import com.cumulocity.sdk.client.PlatformParameters;
import org.cometd.bayeux.client.ClientSession.Extension;
import org.cometd.client.ext.AckExtension;

import java.util.Collection;
import java.util.LinkedList;

import static com.cumulocity.sdk.client.notification.DefaultBayeuxClientProvider.createProvider;

public class SubscriberBuilder<T, R> {

    public static final String NOTIFICATIONS = "cep/notifications";

    public static final String REALTIME = "notification/realtime";

    public static final <T> SubscriptionNameResolver<T>  identityNameResolve() {
        return new SubscriptionNameResolver<T>() {
            public String apply(T input) {
                return String.valueOf(input);
            }
        };
    }

    private String endpoint;

    private Class<R> dataType;

    private PlatformParameters parameters;

    private SubscriptionNameResolver<T> subscriptionNameResolver;

    private boolean messageReliability = false;

    private UnauthorizedConnectionWatcher unauthorizedConnectionWatcher = new UnauthorizedConnectionWatcher();

    public static <T, R> SubscriberBuilder<T, R> anSubscriber() {
        return new SubscriberBuilder<T, R>();
    }

    public SubscriberBuilder<T, R> withRealtimeEndpoint() {
        return withEndpoint(REALTIME);
    }

    public SubscriberBuilder<T, R> withNotificationEndpoint() {
        return withEndpoint(NOTIFICATIONS);
    }

    public SubscriberBuilder<T, R> withEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public SubscriberBuilder<T, R> withDataType(Class<R> dataType) {
        this.dataType = dataType;
        return this;
    }

    public SubscriberBuilder<T, R> withParameters(PlatformParameters parameters) {
        this.parameters = parameters;
        return this;
    }

    public SubscriberBuilder<T, R> withIdentityNameResolver() {
        return withSubscriptionNameResolver(SubscriberBuilder.<T>identityNameResolve());
    }

    public SubscriberBuilder<T, R> withSubscriptionNameResolver(SubscriptionNameResolver<T> subscriptionNameResolver) {
        this.subscriptionNameResolver = subscriptionNameResolver;
        return this;
    }

    public SubscriberBuilder<T, R> withMessageDeliveryAcknowlage(boolean enabled) {
        this.messageReliability = enabled;
        return this;
    }

    public SubscriberBuilder<T, R> withUnauthorizedConnectionRetries(int retries) {
        this.unauthorizedConnectionWatcher = new UnauthorizedConnectionWatcher(retries);
        return this;
    }

    public Subscriber<T, R> build() {
        verifyRequiredFields();
        return new TypedSubscriber<T, R>(new SubscriberImpl<T>(subscriptionNameResolver, createSessionProvider(), unauthorizedConnectionWatcher), dataType);
    }

    private void verifyRequiredFields() {
        checkNotNull(endpoint, "endpoint can't be null");
        checkNotNull(parameters, "platform parameters can't be null");
        checkNotNull(subscriptionNameResolver, "subscriptionNameResolver can't be null");
        checkNotNull(dataType, "dataType can't be null");
    }

    private void checkNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private BayeuxSessionProvider createSessionProvider() {
        return createProvider(endpoint, parameters, dataType, unauthorizedConnectionWatcher, resolveEnabledExtensions());
    }

    private Extension[] resolveEnabledExtensions() {
        Collection<Extension> extensions = new LinkedList<Extension>();
        if (messageReliability) {
            extensions.add(new AckExtension());
        }
        return extensions.toArray(new Extension[] {});
    }
}

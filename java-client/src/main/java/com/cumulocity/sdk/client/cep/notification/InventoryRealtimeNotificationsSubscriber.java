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
package com.cumulocity.sdk.client.cep.notification;

import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.notification.*;

/**
 * @deprecated Use {@link com.cumulocity.sdk.client.notification.NotificationSubscriberProducer} instead.
 */
@Deprecated
public class InventoryRealtimeNotificationsSubscriber implements Subscriber<String, ManagedObjectNotification> {

    private static final String REALTIME_NOTIFICATIONS_URL = "notification/realtime";

    private final Subscriber<String, ManagedObjectNotification> subscriber;

    private static final String channelPrefix = "/managedobjects/";

    public InventoryRealtimeNotificationsSubscriber(PlatformParameters parameters) {
        subscriber = createSubscriber(parameters);
    }

    private Subscriber<String, ManagedObjectNotification> createSubscriber(PlatformParameters parameters) {
        // @formatter:off
        return SubscriberBuilder.<String, ManagedObjectNotification>anSubscriber()
                    .withParameters(parameters)
                    .withEndpoint(REALTIME_NOTIFICATIONS_URL)
                    .withSubscriptionNameResolver(new Identity())
                    .withDataType(ManagedObjectNotification.class)
                    .build();
        // @formatter:on
    }

    /**
     * This method does NOT allow to receive device realtime DELETE actions
     */
    public Subscription<String> subscribe(final String channelID, final SubscriptionListener<String, ManagedObjectNotification> handler) throws SDKException {
        return subscriber.subscribe(channelPrefix + channelID, handler);
    }

    @Override
    public Subscription<String> subscribe(String channelID, SubscribeOperationListener subscribeOperationListener,
                                       SubscriptionListener<String, ManagedObjectNotification> handler,
                                       boolean autoRetry) throws SDKException {
        return subscriber.subscribe(channelID, subscribeOperationListener, handler, autoRetry);
    }

    public void disconnect() {
        subscriber.disconnect();
    }

    private static final class Identity implements SubscriptionNameResolver<String> {
        @Override
        public String apply(String id) {
            return id;
        }
    }
}

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

import com.cumulocity.sdk.client.SDKException;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.Message.Mutable;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSession.Extension;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

class SubscriberImpl<T> implements Subscriber<T, Message>, ConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriberImpl.class);

    private static final int RETRIES_ON_SHORT_NETWORK_FAILURES = 5;

    private static final int SUBSCRIPTION_WATCHER_DELAY = 60;

    private final SubscriptionNameResolver<T> subscriptionNameResolver;

    private final BayeuxSessionProvider bayeuxSessionProvider;

    private final Set<SubscriptionRecord> subscriptions = new CopyOnWriteArraySet<>();

    private final Set<SubscriptionRecord> pendingSubscriptions = new CopyOnWriteArraySet<>();

    private final Object connectionLock = new Object();

    private volatile ClientSession session;

    private final SubscriptionWatcher subscriptionWatcher;

    public SubscriberImpl(SubscriptionNameResolver<T> channelNameResolver, BayeuxSessionProvider bayeuxSessionProvider,
                          final UnauthorizedConnectionWatcher unauthorizedConnectionWatcher) {
        this.subscriptionNameResolver = channelNameResolver;
        this.bayeuxSessionProvider = bayeuxSessionProvider;
        unauthorizedConnectionWatcher.addListener(this);
        subscriptionWatcher = new SubscriptionWatcher();
    }

    public void start() throws SDKException {
        log.trace("starting new subscriber");
        checkState(!isConnected(), "subscriber already started");
        session = bayeuxSessionProvider.get();
        subscriptionWatcher.start();
    }

    private boolean isSuccessfulHandshake(Mutable message) {
        return ClientSessionChannel.META_HANDSHAKE.equals(message.getChannel()) && message.isSuccessful();
    }

    private boolean isSuccessfulConnected(Mutable message) {
        return ClientSessionChannel.META_CONNECT.equals(message.getChannel()) && message.isSuccessful();
    }

    public Subscription<T> subscribe(T object, final SubscriptionListener<T, Message> handler) throws SDKException {
        return this.subscribe(object, new LoggingSubscribeOperationListener(), handler, true);
    }

    public Subscription<T> subscribe(T object,
                                     final SubscribeOperationListener subscribeOperationListener,
                                     final SubscriptionListener<T, Message> handler,
                                     final boolean autoRetry) throws SDKException {
        return subscribe(object, subscribeOperationListener, handler, autoRetry, 0);
    }

    Subscription<T> subscribe(T object,
                              final SubscribeOperationListener subscribeOperationListener,
                              final SubscriptionListener<T, Message> handler,
                              final boolean autoRetry,
                              final int retriesCount) throws SDKException {
        checkArgument(object != null, "object can't be null");
        checkArgument(handler != null, "handler can't be null");
        checkArgument(subscribeOperationListener != null, "subscribeOperationListener can't be null");

        ensureConnection();
        final ClientSessionChannel channel = getChannel(object);
        log.debug("subscribing to channel {}", channel.getId());
        SubscriptionRecord subscriptionRecord = new SubscriptionRecord(object, handler, subscribeOperationListener);
        final MessageListenerAdapter listener = new MessageListenerAdapter(handler, channel, object, subscriptionRecord);
        boolean firstSubscriber = CollectionUtils.isEmpty(channel.getSubscribers());

        // Only listen on subscribe operation result for the first subscription as from the 2nd one on, there is no interaction with server
        if (firstSubscriber) {
            final ClientSessionChannel metaSubscribeChannel = session.getChannel(ClientSessionChannel.META_SUBSCRIBE);
            SubscriptionResultListener subscriptionResultListener = new SubscriptionResultListener(
                    subscriptionRecord, listener, subscribeOperationListener, channel, autoRetry, retriesCount);
            metaSubscribeChannel.addListener(subscriptionResultListener);
        }

        channel.subscribe(listener);

        // Add to pending subscriptions list if autoRetry and this is first subscriber
        if (autoRetry && firstSubscriber) {
            pendingSubscriptions.add(subscriptionRecord);
        } else if (!firstSubscriber) {
            log.info("Added listener to a channel that has been subscribed by other");
            subscriptions.add(subscriptionRecord);
        }

        // Notify immediately if there would be no interaction with server
        if (!firstSubscriber) {
            try {
                subscribeOperationListener.onSubscribingSuccess(channel.getId());
            } catch (Exception e) {
                log.error("Error notifying listener", e);
            }
        }

        return listener.getSubscription();
    }

    private void ensureConnection() {
        synchronized (connectionLock) {
            if (!isConnected()) {
                start();
                session.addExtension(new ReconnectOnSuccessfulConnected());
            }
        }
    }

    private boolean isConnected() {
        return session != null;
    }

    private ClientSessionChannel getChannel(final T object) {
        final String channelId = subscriptionNameResolver.apply(object);
        checkState(channelId != null && channelId.length() > 0, "channelId is null or empty for object : " + object);
        return session.getChannel(channelId);
    }

    @Override
    public void disconnect() {
        synchronized (connectionLock) {
            if (isConnected()) {
                subscriptionWatcher.stop();
                subscriptions.clear();
                pendingSubscriptions.clear();
                session.disconnect();
                session = null;
            }
        }
    }

    private void checkState(boolean state, String message) {
        if (!state) {
            throw new IllegalStateException(message);
        }
    }

    private void checkArgument(boolean state, String message) {
        if (!state) {
            throw new IllegalArgumentException(message);
        }
    }

    private void resubscribeAll() {
        Set<SubscriptionRecord> allSubscriptions = new HashSet<>(subscriptions);
        allSubscriptions.addAll(pendingSubscriptions);

        resubscribe(allSubscriptions);
    }

    private void resubscribePending() {
        if (pendingSubscriptions.isEmpty()) {
            return;
        }

        resubscribe(new HashSet<>(pendingSubscriptions));
    }

    private void resubscribe(Set<SubscriptionRecord> subscriptionsToResubscribe) {
        removeBrokenListeners();

        for (SubscriptionRecord subscribed : subscriptionsToResubscribe) {
            removeBrokenSubscription(subscribed);
            Subscription<T> subscription = subscribe(subscribed.getId(), subscribed.subscribeOperationListener,
                    subscribed.listener, true);
            try {
                subscribed.getListener().onError(subscription,
                        new ReconnectedSDKException("bayeux client reconnected clientId: " + session.getId()));
            } catch (Exception e) {
                log.warn("Error when executing onError of listener: {}, {}", subscribed.getListener(), e.getMessage());
            }
        }
    }

    private void removeBrokenSubscription(SubscriptionRecord subscription) {
        log.debug("Removing broken subscription: {}", subscription.id);

        // Remove pending subscription
        boolean foundInPendingSubscriptions = pendingSubscriptions.remove(subscription);
        if (foundInPendingSubscriptions) {
            log.debug("Removed subscription from pending subscriptions list");
        }

        // Remove subscription from list
        boolean foundInSubscriptions = subscriptions.remove(subscription);
        if (foundInSubscriptions) {
            log.debug("Removed subscription from subscriptions list");
        }
    }

    private void removeBrokenListeners() {
        // Remove those unsubscribe listeners added when the subscription has been made, see SubscriptionResultListener#onMessage
        final ClientSessionChannel metaUnsubscribeChannel = session.getChannel(ClientSessionChannel.META_UNSUBSCRIBE);
        removeAllListeners(metaUnsubscribeChannel);

        final ClientSessionChannel metaSubscribeChannel = session.getChannel(ClientSessionChannel.META_SUBSCRIBE);
        removeAllListeners(metaSubscribeChannel);
    }

    private void removeAllListeners(ClientSessionChannel channel) {
        List<ClientSessionChannel.ClientSessionChannelListener> channelListeners = channel.getListeners();
        if (!CollectionUtils.isEmpty(channelListeners)) {
            log.debug("Removing {} listener(s) on {} channel", channelListeners.size(), channel.getId());
            for (ClientSessionChannel.ClientSessionChannelListener channelListener : channelListeners) {
                channel.removeListener(channelListener);
            }
        }
    }

    @Override
    public void onDisconnection(final int httpCode) {
        for (final SubscriptionRecord subscription : subscriptions) {
            subscription.getListener().onError(new DummySubscription(subscription),
                    new SDKException(httpCode, "bayeux client disconnected  clientId: " + session.getId()));
        }
    }

    public final class ReconnectOnSuccessfulConnected implements Extension {

        private volatile boolean reHandshakeSuccessful = false;

        private volatile boolean reconnectedSuccessful = false;

        @Override
        public boolean sendMeta(ClientSession session, Mutable message) {
            return true;
        }

        @Override
        public boolean send(ClientSession session, Mutable message) {
            return true;
        }

        @Override
        public boolean rcvMeta(ClientSession session, Mutable message) {
            if (isSuccessfulHandshake(message)) {
                reHandshakeSuccessful = true;
            } else if (isSuccessfulConnected(message)) {
                reconnectedSuccessful = true;
            } else {
                return true;
            }
            // Resubscribe all only there is a successful handshake and successfully connected.
            if (reHandshakeSuccessful && reconnectedSuccessful) {
                log.debug("reconnect operation detected for session {} - {} ", bayeuxSessionProvider, session.getId());
                reHandshakeSuccessful = false;
                reconnectedSuccessful = false;
                resubscribeAll();
            }
            // Subscribe pending on each successful /meta/connect
            if (reconnectedSuccessful) {
                reconnectedSuccessful = false;
                resubscribePending();
            }
            return true;
        }

        @Override
        public boolean rcv(ClientSession session, Mutable message) {
            return true;
        }
    }

    private static class LoggingSubscribeOperationListener implements SubscribeOperationListener {

        private static final Logger LOG = LoggerFactory.getLogger(LoggingSubscribeOperationListener.class);

        @Override
        public void onSubscribingSuccess(String channelId) {
            LOG.info("Successfully subscribed: {}", channelId);
        }

        @Override
        public void onSubscribingError(String channelId, String message, Throwable throwable) {
            LOG.error("Error when subscribing channel: {}, error: {}", channelId, message, throwable);
        }
    }

    private final class SubscriptionResultListener implements MessageListener {

        private final SubscribeOperationListener subscribeOperationListener;

        private final MessageListenerAdapter listener;

        private final ClientSessionChannel channel;

        private final SubscriptionRecord subscription;

        private final boolean autoRetry;

        private final int retriesCount;

        private SubscriptionResultListener(SubscriptionRecord subscribed, MessageListenerAdapter listener,
                                           SubscribeOperationListener subscribeOperationListener,
                                           ClientSessionChannel channel, boolean autoRetry, int retriesCount) {
            this.subscription = subscribed;
            this.listener = listener;
            this.subscribeOperationListener = subscribeOperationListener;
            this.channel = channel;
            this.autoRetry = autoRetry;
            this.retriesCount = retriesCount;
        }

        @Override
        public void onMessage(ClientSessionChannel metaSubscribeChannel, Message message) {

            if (!Channel.META_SUBSCRIBE.equals(metaSubscribeChannel.getId())) {
                // Should never be here
                log.warn("Unexpected message to wrong channel, to SubscriptionSuccessListener: {}, {}", metaSubscribeChannel, message);
                return;
            }
            if (message.isSuccessful() && !isSubscriptionToChannel(message)) {
                return;
            }
            try {
                if (message.isSuccessful()) {
                    log.debug("subscribed successfully to channel {}, {}", this.channel, message);
                    if (autoRetry) {
                        pendingSubscriptions.remove(subscription);
                    }
                    subscriptions.add(subscription);
                    subscribeOperationListener.onSubscribingSuccess(this.channel.getId());
                } else {
                    log.debug("Error subscribing channel: {}, {}", this.channel.getId(), message);
                    if (message.containsKey(Message.ERROR_FIELD)) {
                        String error = (String) message.get(Message.ERROR_FIELD);
                        if (error.contains("402::Unknown")) {
                            log.warn("Resubscribing for channel {}", this.channel.getId());
                            resubscribe();
                        }
                    }
                    handleError(message);
                }
            } catch (NullPointerException ex) {
                log.warn("NPE on message {} - {}", message, Channel.META_SUBSCRIBE);
                throw new RuntimeException(ex);
            } finally {
                metaSubscribeChannel.removeListener(this);
            }
        }

        private void resubscribe() {
            if (session.isConnected()) {
                subscribe(subscription.getId(), subscribeOperationListener, listener.handler, autoRetry);
            } else {
                log.warn("Not Connected for channel {}", this.channel.getId());
                pendingSubscriptions.add(subscription);
            }
        }

        private boolean isSubscriptionToChannel(Message message) {
            return Objects.equals(channel.getId(), message.get(Message.SUBSCRIPTION_FIELD));
        }

        private void handleError(Message message) {
            if (autoRetry && isShortNetworkFailure(message)) {
                if (retriesCount > RETRIES_ON_SHORT_NETWORK_FAILURES) {
                    log.error("Detected a short network failure, giving up after {} retries. " +
                            "Another retry attempt only happen on another successfully handshake", retriesCount);
                } else {
                    log.debug("Detected a short network failure, retrying to subscribe channel: {}", channel.getId());
                    channel.unsubscribe(listener, new MessageListener() {
                        @Override
                        public void onMessage(ClientSessionChannel channel, Message message) {
                            subscribe(subscription.getId(), subscribeOperationListener, listener.handler, autoRetry,
                                    retriesCount + 1);
                        }
                    });
                }
            } else if (autoRetry) {
                log.debug("Detected an error (either server or long network error), " +
                        "another retry attempt only happen on another successfully handshake");
            }
            notifyListenerOnError(message);
        }

        private void notifyListenerOnError(Message message) {
            String errorMessage = "Unknown error (unspecified by server)";
            Throwable throwable = null;
            Object error = message.get(Message.ERROR_FIELD);
            if (error == null) {
                error = message.get("failure");
                if (error instanceof Map) {
                    throwable = (Throwable) ((Map) error).get("exception");
                    if (throwable != null) {
                        errorMessage = throwable.getMessage();
                    }
                }
            } else {
                errorMessage = (String) error;
            }
            subscribeOperationListener.onSubscribingError(channel.getId(), errorMessage, throwable);
        }

        private boolean isShortNetworkFailure(Message message) {
            Object failure = message.get("failure");
            return failure != null;
        }
    }

    private class ChannelSubscription implements Subscription<T> {

        private final MessageListener listener;

        private final ClientSessionChannel channel;

        private final T object;

        private final SubscriptionRecord subscriptionRecord;

        ChannelSubscription(MessageListener listener, ClientSessionChannel channel, T object, SubscriptionRecord subscriptionRecord) {
            this.listener = listener;
            this.channel = channel;
            this.object = object;
            this.subscriptionRecord = subscriptionRecord;
        }

        @Override
        public void unsubscribe() {
            log.debug("unsubscribing from channel {}", channel.getId());
            pendingSubscriptions.remove(subscriptionRecord);
            subscriptions.remove(subscriptionRecord);
            channel.unsubscribe(listener);
        }

        @Override
        public T getObject() {
            return object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ChannelSubscription that = (ChannelSubscription) o;

            if (!Objects.equals(channel, that.channel)) return false;
            if (!Objects.equals(object, that.object)) return false;
            return Objects.equals(subscriptionRecord, that.subscriptionRecord);
        }

        @Override
        public int hashCode() {
            int result = channel != null ? channel.hashCode() : 0;
            result = 31 * result + (object != null ? object.hashCode() : 0);
            result = 31 * result + (subscriptionRecord != null ? subscriptionRecord.hashCode() : 0);
            return result;
        }
    }

    private final class DummySubscription implements Subscription<T> {

        private final SubscriptionRecord subscription;

        DummySubscription(final SubscriptionRecord subscription) {
            this.subscription = subscription;
        }

        @Override
        public void unsubscribe() {
        }

        @Override
        public T getObject() {
            return subscription.getId();
        }
    }

    private final class MessageListenerAdapter implements MessageListener {

        private final SubscriptionListener<T, Message> handler;

        private final Subscription<T> subscription;

        MessageListenerAdapter(SubscriptionListener<T, Message> handler, ClientSessionChannel channel, T object, SubscriptionRecord subscriptionRecord) {
            this.handler = handler;
            subscription = createSubscription(channel, object, subscriptionRecord);
        }

        private ChannelSubscription createSubscription(ClientSessionChannel channel, T object, SubscriptionRecord subscriptionRecord) {
            return new ChannelSubscription(this, channel, object, subscriptionRecord);
        }

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            handler.onNotification(subscription, message);
        }

        public Subscription<T> getSubscription() {
            return subscription;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MessageListenerAdapter that = (MessageListenerAdapter) o;

            if (!Objects.equals(handler, that.handler)) return false;
            return Objects.equals(subscription, that.subscription);
        }

        @Override
        public int hashCode() {
            int result = handler != null ? handler.hashCode() : 0;
            result = 31 * result + (subscription != null ? subscription.hashCode() : 0);
            return result;
        }
    }

    private final class SubscriptionRecord {

        private final T id;

        private final SubscriptionListener<T, Message> listener;

        private final SubscribeOperationListener subscribeOperationListener;

        public SubscriptionRecord(T id, SubscriptionListener<T, Message> listener,
                                  SubscribeOperationListener subscribeOperationListener) {
            this.id = id;
            this.listener = listener;
            this.subscribeOperationListener = subscribeOperationListener;
        }

        public T getId() {
            return id;
        }

        public SubscriptionListener<T, Message> getListener() {
            return listener;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (listener != null ? listener.hashCode() : 0);
            result = 31 * result + (subscribeOperationListener != null ? subscribeOperationListener.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SubscriptionRecord that = (SubscriptionRecord) o;

            if (!Objects.equals(id, that.id)) return false;
            if (!Objects.equals(listener, that.listener)) return false;
            return Objects.equals(subscribeOperationListener, that.subscribeOperationListener);
        }
    }

    private final class SubscriptionWatcher {
        private final ScheduledExecutorService executorService;

        public SubscriptionWatcher() {
            ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("CumulocitySubscriptionWatcher-scheduler-%d")
                    .build();
            executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        }

        public void start() {
            executorService.scheduleWithFixedDelay(
                    checkSubscriptionsRunner(),
                    SUBSCRIPTION_WATCHER_DELAY,
                    SUBSCRIPTION_WATCHER_DELAY,
                    TimeUnit.SECONDS);
        }

        public void stop() {
            executorService.shutdown();
        }

        private Runnable checkSubscriptionsRunner() {
            return () -> {
                log.debug("Running watcher to check subscriptions");
                ImmutableSet.<SubscriptionRecord>builder()
                        .addAll(subscriptions)
                        .addAll(pendingSubscriptions)
                        .build()
                        .forEach(this::checkSubscription);
            };
        }

        private void checkSubscription(SubscriptionRecord subRec) {
            ClientSessionChannel channel = getChannel(subRec.getId());
            if (channel.getSubscribers().isEmpty()) {
                log.warn("{} bayeux channel {} has no client subscribers",
                        pendingSubscriptions.contains(subRec) ? "Pending" : "Subscribed", subRec.getId());
                reSubscribe(subRec);
            } else {
                log.debug("Bayeux channel {} has {} client subscriptions (OK)",
                        subRec.getId(), channel.getSubscribers().size());
            }
        }

        private void reSubscribe(SubscriptionRecord subRec) {
            if (session.isConnected()) {
                log.info("Trying to subscribe channel {}", subRec.getId());
                subscribe(subRec.getId(), subRec.subscribeOperationListener, subRec.listener, true);
            } else {
                log.warn("Session is not connected, adding channel {} to pending subscriptions", subRec.getId());
                subscriptions.remove(subRec);
                pendingSubscriptions.add(subRec);
            }
        }
    }
}

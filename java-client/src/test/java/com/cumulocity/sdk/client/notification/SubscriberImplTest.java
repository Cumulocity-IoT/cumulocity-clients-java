package com.cumulocity.sdk.client.notification;

import com.cumulocity.sdk.client.SDKException;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.Message.Mutable;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSession.Extension;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SubscriberImplTest {

    @Mock
    ClientSession client;

    @Mock
    ClientSessionChannel metaSubscribeChannel;

    @Mock
    ClientSessionChannel metaHandshakeChannel;

    @Mock
    ClientSessionChannel metaUnsubscribeChannel;

    @Mock
    ClientSessionChannel notificationChannel;

    @Mock
    SubscriptionNameResolver<Object> subscriptionNameResolver;

    @Mock
    BayeuxSessionProvider bayeuxSessionProvider;

    @Mock
    UnauthorizedConnectionWatcher unauthorizedConnectionWatcher;

    Subscriber<Object, Message> subscriber;

    @Mock
    SubscriptionListener<Object, Message> listener;

    final ArgumentCaptor<MessageListener> listenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

    @Before
    public void setup() throws SDKException {
        subscriber = new SubscriberImpl<>(subscriptionNameResolver, bayeuxSessionProvider, unauthorizedConnectionWatcher);
        mockClientProvider();
        when(metaSubscribeChannel.getId()).thenReturn(Channel.META_SUBSCRIBE);
        when(metaUnsubscribeChannel.getId()).thenReturn(Channel.META_UNSUBSCRIBE);
        when(client.getChannel(ClientSessionChannel.META_SUBSCRIBE)).thenReturn(metaSubscribeChannel);
        when(client.getChannel(ClientSessionChannel.META_UNSUBSCRIBE)).thenReturn(metaUnsubscribeChannel);
    }

    private void mockClientProvider() throws SDKException {
        Mockito.when(bayeuxSessionProvider.get()).thenReturn(client);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void shouldFailSubscribeWhenSubscriptionObjectIsNull() throws SDKException {
        //Given
        //When
        subscriber.subscribe(null, listener);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void shouldFailSubscribeWhenNotificationListenerIsNull() throws SDKException {
        //Given
        //When
        subscriber.subscribe(new Object(), null);
    }

    @Test(expected = IllegalStateException.class)
    public final void shouldFailSubscribeWhenNotConnected() throws SDKException {
        //Given
        //When
        subscriber.subscribe(new Object(), listener);
    }

    @Test
    public final void shouldSuccesfulySubscribeWhenConnected() throws SDKException {
        //Given
        final String channelId = "/channel";
        final ClientSessionChannel channel = givenChannel(channelId);
        //When
        subscriber.subscribe(channelId, listener);
        //Then
        verify(channel).subscribe(Mockito.any(MessageListener.class));
    }

    public final void shouldFailDisconnectNotThrowExcpetionWhenNotConnected() {
        //Given
        //When
        subscriber.disconnect();
        //Then
    }

    @Test
    public final void shouldSuccesfulySubscribeAndMakeHandshake() throws SDKException {
        //Given
        final String channelId = "/channel";
        final ClientSessionChannel channel = givenChannel(channelId);
        //When
        subscriber.subscribe(channelId, listener);
        verifyConnected();
        verifySubscribed(channel);
    }

    @Test
    public final void shouldSuccesfulyUnSubscribeAfterSubscribe() throws SDKException {
        //Given
        final String channelId = "/channel";
        final ClientSessionChannel channel = givenChannel(channelId);
        //When
        final Subscription<Object> subscription = subscriber.subscribe(channelId, listener);
        subscription.unsubscribe();
        //Then
        verifyConnected();
        verifySubscribed(channel);
        verifyUnsubscribe(channel);
    }

    @Test
    public final void shouldSuccesfulyDisconnectOnStop() throws SDKException {
        //Given
        subscriberConnected();
        //When
        subscriber.disconnect();
        //Then
        verifyDisconnected();
    }

    @Test
    public final void shouldNotifySubscriberAboutSubscribeOperationFail() throws SDKException {
        //Given
        final String channelId = "/channel";
        final ClientSessionChannel channel = givenChannel(channelId);
        final AtomicBoolean onError = new AtomicBoolean(false);
        SubscribeOperationListener subscribeOperationListener = new SubscribeOperationListener() {
            @Override
            public void onSubscribingSuccess(String channelId) {

            }

            @Override
            public void onSubscribingError(String channelId, String error, Throwable throwable) {
                onError.set(true);
            }
        };
        final Subscription<Object> subscription = subscriber.subscribe(channelId, subscribeOperationListener, listener, false);
        verify(metaSubscribeChannel).addListener(listenerCaptor.capture());
        listenerCaptor.getValue().onMessage(metaSubscribeChannel, mockSubscribeMessage(channelId, false));
        //Then
        verifyConnected();
        verifySubscribed(channel);
        assertThat(onError.get()).isTrue();
    }

    @Test
    public final void shouldResubscribeAllSubscriptionsOnReconnect() throws SDKException {
        //Given
        final ClientSessionChannel channel = givenChannelWithSubscription("/channel");
        final ClientSessionChannel channelSecond = givenChannelWithSubscription("/channelSecond");

        //When
        reconnect();

        //Then
        verifyConnected();
        verifySubscribed(channel, 2);
        verifySubscribed(channelSecond, 2);
    }

    @Test
    public final void shouldNotResubscribeToUnsubscribedSubscriptionsOnReconnect() throws SDKException {
        //Given
        final ClientSessionChannel channel = givenChannelWithSubscription("/channel");
        final ClientSessionChannel secondChannel = givenChannel("/channelSecond");
        final Subscription<Object> subscription = givenSubscription(secondChannel);
        givenSubsciptionsSuccessfulySubscribed("/channelSecond");
        //When
        subscription.unsubscribe();
        sendUnsubscribeMessage("/channelSecond");
        reconnect();
        //Then
        verifyConnected();
        verifySubscribed(channel, 2);
        verifySubscribed(secondChannel, 1);

    }

    @Test
    public final void shouldCallOnErrorOnSubscriptionsReconnect() throws SDKException {
        //given
        givenChannelWithSubscription("/channel");

        //when
        reconnect();

        //then
        verifyOnErrorListener();
    }

    private void sendUnsubscribeMessage(String channelId) {
        for (MessageListener listener : listenerCaptor.getAllValues()) {
            listener.onMessage(metaUnsubscribeChannel, mockSubscribeMessage(channelId, true));
        }
    }

    @Test
    public final void afterCreateMessageListenerShouldCreateCorrectSubscripsction() {
        //Given
        String subscribedOn = "channelId";
        givenChannel(subscribedOn);
        //When
        Subscription<Object> subscription = subscriber.subscribe(subscribedOn, Mockito.mock(SubscriptionListener.class));
        //Then
        assertThat(subscription).isNotNull();
        assertThat(subscription.getObject()).isNotNull().isEqualTo(subscribedOn);
    }

    @Test
    public final void onMessageShouldPassMessageData() {
        final SubscriptionListener listenerMock = Mockito.mock(SubscriptionListener.class);
        final Message message = Mockito.mock(Message.class);
        //Given
        String subscribedOn = "channelId";
        final ClientSessionChannel channel = givenChannel(subscribedOn);

        //When
        Subscription<Object> subscription = subscriber.subscribe(subscribedOn, listenerMock);
        Mockito.verify(channel).subscribe(listenerCaptor.capture());
        listenerCaptor.getValue().onMessage(channel, message);
        //Then
        verify(listenerMock).onNotification(subscription, message);
    }

    @Test
    public void shouldRetryOnSubscribeOperationFailed() {
        //Given
        final String channelId = "/channel";
        final ClientSessionChannel channel = givenChannel(channelId);
        final AtomicInteger onError = new AtomicInteger(0);
        SubscribeOperationListener subscribeOperationListener = new SubscribeOperationListener() {
            @Override
            public void onSubscribingSuccess(String channelId) {

            }

            @Override
            public void onSubscribingError(String channelId, String error, Throwable throwable) {
                onError.incrementAndGet();
            }
        };
        final ArgumentCaptor<MessageListener> unsubscribeListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        subscriber.subscribe(channelId, subscribeOperationListener, listener, true);
        verify(metaSubscribeChannel).addListener(listenerCaptor.capture());
        Message message = mockSubscribeMessage(false, ImmutableMap.of("failure", "Network unreachable!"));
        listenerCaptor.getValue().onMessage(metaSubscribeChannel, message);
        verify(channel).unsubscribe(any(MessageListener.class), unsubscribeListenerCaptor.capture());
        unsubscribeListenerCaptor.getValue().onMessage(metaUnsubscribeChannel, message);
        listenerCaptor.getValue().onMessage(metaSubscribeChannel, mockSubscribeMessage(true,
                ImmutableMap.of(Message.SUBSCRIPTION_FIELD, channelId)));

        //Then
        verifyConnected();
        verifySubscribed(channel, 2);
        assertThat(onError.get()).isEqualTo(1);
    }

    @Test
    public void shouldRemoveSubscriberOnUnsubscribe() {
        final String channelId = "/channel";
        final ClientSessionChannel channel = givenChannel(channelId);
        final Subscription subscription = subscriber.subscribe(channelId, listener);
        subscription.unsubscribe();

        assertThat(CollectionUtils.isEmpty(channel.getListeners())).isTrue();
        verifyUnsubscribe(channel);
    }

    @Test
    public void shouldCleanupSubscriberOnReconnect() {
        final ClientSessionChannel channel = givenChannelWithSubscription("/channel");
        ClientSessionChannel.ClientSessionChannelListener mockUnsubscribeListener = mock(ClientSessionChannel.ClientSessionChannelListener.class);

        when(metaUnsubscribeChannel.getListeners()).thenReturn(Arrays.asList(mockUnsubscribeListener));

        reconnect();

        verify(metaUnsubscribeChannel).removeListener(mockUnsubscribeListener);
        verifySubscribed(channel, 2);
    }

    @Test
    public void shouldNotAddSubscribeListenerWhenChannelHasSubscriber() {
        final String channelId = "/channel";
        final ClientSessionChannel channel = givenChannel(channelId);
        SubscriptionListener<Object, Message> listener1 = mock(SubscriptionListener.class);
        SubscriptionListener<Object, Message> listener2 = mock(SubscriptionListener.class);

        subscriber.subscribe(channelId, listener1);

        verify(metaSubscribeChannel).addListener(any(MessageListener.class));

        final AtomicBoolean notified = new AtomicBoolean(false);
        when(channel.getSubscribers()).thenReturn(Arrays.asList(mock(ClientSessionChannel.MessageListener.class)));
        subscriber.subscribe(channelId, new SubscribeOperationListener() {
            @Override
            public void onSubscribingSuccess(String channelId) {
                notified.set(true);
            }

            @Override
            public void onSubscribingError(String channelId, String error, Throwable throwable) {

            }
        }, listener2, true);

        verifyNoMoreInteractions(metaSubscribeChannel);
        assertThat(notified.get()).isTrue();
    }

    private Subscription<Object> givenSubscription(ClientSessionChannel channel) {
        final Subscription<Object> subscribe = subscriber.subscribe(channel.getId(), listener);
        return subscribe;
    }

    private ClientSessionChannel givenChannelWithSubscription(final String channelId) {
        final ClientSessionChannel channel = givenChannel(channelId);
        subscriber.subscribe(channelId, listener);
        givenSubsciptionsSuccessfulySubscribed(channelId);
        return channel;
    }

    private void givenSubsciptionsSuccessfulySubscribed(final String channelId) {
        verify(metaSubscribeChannel, Mockito.atLeastOnce()).addListener(listenerCaptor.capture());
        for (MessageListener listener : listenerCaptor.getAllValues()) {
            listener.onMessage(metaSubscribeChannel, mockSubscribeMessage(channelId, true));
        }
    }

    private void reconnect() {
        final ArgumentCaptor<Extension> listenerCaptor = ArgumentCaptor.forClass(Extension.class);
        verify(client).addExtension(listenerCaptor.capture());
        Extension reconnectListener = listenerCaptor.getValue();
        final Mutable message = Mockito.mock(Mutable.class);
        when(message.getChannel()).thenReturn(ClientSessionChannel.META_HANDSHAKE);
        when(message.isSuccessful()).thenReturn(true);

        final Mutable connectedMessage = Mockito.mock(Mutable.class);
        when(connectedMessage.getChannel()).thenReturn(ClientSessionChannel.META_CONNECT);
        when(connectedMessage.isSuccessful()).thenReturn(true);
        reconnectListener.rcvMeta(client, message);
        reconnectListener.rcvMeta(client, connectedMessage);
    }

    private Message mockSubscribeMessage(String channelID, boolean successful) {
        Message message = mock(Message.class);
        when(message.get(Message.SUBSCRIPTION_FIELD)).thenReturn(channelID);
        when(message.isSuccessful()).thenReturn(successful);
        return message;
    }

    private Message mockSubscribeMessage(boolean successful, Map<? extends Object, ? extends Object> additionalFields) {
        Message message = mock(Message.class);
        when(message.isSuccessful()).thenReturn(successful);
        if(!MapUtils.isEmpty(additionalFields)) {
            for(Map.Entry<? extends Object, ? extends Object> entry: additionalFields.entrySet()) {
                when(message.get(entry.getKey())).thenReturn(entry.getValue());
            }
        }
        return message;
    }

    private void subscriberConnected() throws SDKException {
        givenSubscription(givenChannel("/channel"));
    }

    private void verifyUnsubscribe(final ClientSessionChannel channel) {
        verify(channel).unsubscribe(Mockito.any(MessageListener.class));
    }

    private void verifySubscribed(final ClientSessionChannel channel) {
        verifySubscribed(channel, 1);
    }

    private void verifySubscribed(final ClientSessionChannel channel, int number) {
        verify(channel, times(number)).subscribe(Mockito.any(MessageListener.class));
    }

    private void verifyDisconnected() {
        verify(client).disconnect();
    }

    private void verifyConnected() throws SDKException {
        verify(bayeuxSessionProvider).get();
    }

    private ClientSessionChannel givenChannel(String channelId) {
        when(subscriptionNameResolver.apply(channelId)).thenReturn(channelId);
        ClientSessionChannel channel = Mockito.mock(ClientSessionChannel.class);
        when(client.getChannel(channelId)).thenReturn(channel);
        when(channel.getId()).thenReturn(channelId);
        return channel;
    }

    private void verifyOnErrorListener() throws SDKException {
        verify(listener).onError(any(Subscription.class), anyInstanceOf(ReconnectedSDKException.class));
    }

    private static <T> T anyInstanceOf(Class<T> clazz) {
        return (T) argThat(new AnyInstanceOfClass(clazz));
    }

    private static class AnyInstanceOfClass<T> implements ArgumentMatcher<T> {

        private final Class<T> clazz;

        public AnyInstanceOfClass(Class<T> clazz) {
            this.clazz = clazz;
        }


        @Override
        public boolean matches(T o) {
            return clazz.isInstance(o);
        }

        public String toString() {
            return "[argument must be a type of " + clazz.getCanonicalName() + "]";
        }
    }

}

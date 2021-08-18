package com.cumulocity.sdk.client.notification;

import static org.mockito.Mockito.verify;

import org.cometd.bayeux.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cumulocity.sdk.client.SDKException;

@ExtendWith(MockitoExtension.class)
public class TypedSubscriberTest {

    @Mock
    Subscriber<Object, Message> subscriberMock;

    @Mock
    SubscriptionListener<Object, Object> handler;

    TypedSubscriber<Object, Object> subscriber;

    @BeforeEach
    public void setup() {
        subscriber = new TypedSubscriber<>(subscriberMock, Object.class);
    }

    @Test
    public final void shouldDelegateSubscribe() throws SDKException {
        //Given
        final Object subscribeObject = new Object();
        //When
        subscriber.subscribe(subscribeObject, handler);
        //Then
        verify(subscriberMock).subscribe(Mockito.eq(subscribeObject), Mockito.any(SubscriptionListener.class));
    }

    @Test
    public final void shouldDelegateDisconnect() {

        //When
        subscriber.disconnect();
        //Then
        verify(subscriberMock).disconnect();
    }
 }

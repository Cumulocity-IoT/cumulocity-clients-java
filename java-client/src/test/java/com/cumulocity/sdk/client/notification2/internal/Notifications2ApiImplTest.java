package com.cumulocity.sdk.client.notification2.internal;

import com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionRepresentation;
import com.cumulocity.sdk.client.PlatformImpl;
import com.cumulocity.sdk.client.messaging.notifications.*;
import com.cumulocity.sdk.client.notification2.DeviceContextTargetApi;
import com.cumulocity.sdk.client.notification2.NotificationListener;
import com.cumulocity.sdk.client.notification2.Subscription;
import com.cumulocity.sdk.client.notification2.TenantContextTargetApi;
import com.cumulocity.sdk.client.notification2.config.Notifications2Properties;
import com.cumulocity.sdk.client.notification2.exception.Notifications2NotEnabledException;
import com.cumulocity.sdk.client.notification2.exception.Notifications2SubscriptionAlreadyEstablishedException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static com.cumulocity.sdk.client.notification2.internal.Notifications2ApiImpl.CONTEXT_DEVICE;
import static com.cumulocity.sdk.client.notification2.internal.Notifications2ApiImpl.CONTEXT_TENANT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Notifications2ApiImplTest {
    public static final String WS_URL = "ws://localhost:7711";
    private final PlatformImpl platform = mock(PlatformImpl.class);
    private final WebSocketClient client = mock(WebSocketClient.class);
    private final NotificationSubscriptionApi notificationSubscriptionApi = mock(NotificationSubscriptionApi.class);
    private final NotificationListener listener = mock(NotificationListener.class);
    private Notifications2ApiImpl api;
    private final NotificationSubscriptionCollection notificationSubscriptionCollection = mock(NotificationSubscriptionCollection.class);
    private final PagedNotificationSubscriptionCollectionRepresentation pagedRep = mock(PagedNotificationSubscriptionCollectionRepresentation.class);
    private final ArgumentCaptor<NotificationSubscriptionRepresentation> subscriptionCaptor = ArgumentCaptor.forClass(NotificationSubscriptionRepresentation.class);
    private final ArgumentCaptor<NotificationSubscriptionFilter> filterCaptor = ArgumentCaptor.forClass(NotificationSubscriptionFilter.class);


    @BeforeEach
    public void setupMocks() {
        when(platform.getNotifications2()).thenReturn(new Notifications2Properties().withWebsocketUrl(WS_URL));
        when(platform.getTenantId()).thenReturn("Tenant1");
        when(platform.getNotificationSubscriptionApi()).thenReturn(notificationSubscriptionApi);
        when(notificationSubscriptionApi.getSubscriptionsByFilter(any())).thenReturn(notificationSubscriptionCollection);
        when(notificationSubscriptionCollection.get()).thenReturn(pagedRep);
        when(pagedRep.getSubscriptions()).thenReturn(Collections.emptyList());
        initApiWithMocks();
    }

    private void initApiWithMocks() {
        api = new Notifications2ApiImpl(platform.getNotifications2(), "tenant1", platform);
        api.setClientFactoryFunction((sub, lis) -> client);
    }

    @Test
    public void testParametersNotSet() {
        // given
        when(platform.getNotifications2()).thenReturn(new Notifications2Properties());
        initApiWithMocks();

        // when/then
        Assertions.assertThrows(Notifications2NotEnabledException.class,
                () -> api.subscribe(Mockito.mock(Subscription.class), listener));

        Assertions.assertThrows(Notifications2NotEnabledException.class,
                () -> api.disconnect(mock(Subscription.ID.class), anyBoolean()));

    }

    @Test
    public void testTenantSubscriptionWhenSubscriptionDoesntExist() {
        // when
        Subscription subscription = Subscription.Builder.get().withId("test", "mySubscriber").withTenantContextTargetApis(TenantContextTargetApi.EVENTS)
                .withTenantId("tenant1").withTypeFilter("c8y_test").build();
        api.subscribe(subscription, listener);

        // then
        verify(notificationSubscriptionApi).subscribe(subscriptionCaptor.capture());
        verify(pagedRep).getSubscriptions();
        verify(client).start();

        NotificationSubscriptionRepresentation sub = subscriptionCaptor.getValue();
        assertEquals("test", sub.getSubscription());
        assertTrue(sub.getSubscriptionFilter().getApis().contains(TenantContextTargetApi.EVENTS.getTarget()));
        assertTrue(sub.isNonPersistent());
        assertEquals(CONTEXT_TENANT, sub.getContext());
        assertNull(sub.getSource());
        assertEquals("c8y_test", sub.getSubscriptionFilter().getTypeFilter());
    }

    @Test
    public void testDeviceSubscriptionWhenSubscriptionDoesntExist() {
        // when
        Subscription subscription = Subscription.Builder.get().withId("test", "mySubscriber").withDeviceContextTargetApis("dev1", DeviceContextTargetApi.OPERATIONS)
                .withTenantId("tenant1").build();
        api.subscribe(subscription, listener);

        // then
        verify(notificationSubscriptionApi).subscribe(subscriptionCaptor.capture());
        verify(pagedRep).getSubscriptions();
        verify(client).start();
        NotificationSubscriptionRepresentation sub = subscriptionCaptor.getValue();
        assertEquals("test", sub.getSubscription());
        assertTrue(sub.getSubscriptionFilter().getApis().contains(DeviceContextTargetApi.OPERATIONS.getTarget()));
        assertEquals(CONTEXT_DEVICE, sub.getContext());
        assertEquals("dev1", sub.getSource().getId().getValue());
        assertTrue(sub.isNonPersistent());
        assertNull(sub.getSubscriptionFilter().getTypeFilter());
    }

    @Test
    public void testSubscriptionWhenPlatformObjectAlreadyExists() {
        // given
        when(pagedRep.getSubscriptions()).thenReturn(Lists.list(mock(NotificationSubscriptionRepresentation.class)));

        // when
        Subscription subscription = Subscription.Builder.get().withId("test", "mySubscriber").withDeviceContextTargetApis("dev1", DeviceContextTargetApi.OPERATIONS)
                .withTenantId("tenant1").build();
        api.subscribe(subscription, listener);

        // then
        verify(notificationSubscriptionApi, times(0)).subscribe(any());
        verify(client).start();
    }

    @Test
    public void testSubscriptionWhenClientIsAlreadyRunning() {
        // given
        Subscription subscription = Subscription.Builder.get().withId("test", "mySubscriber").withDeviceContextTargetApis("dev1", DeviceContextTargetApi.OPERATIONS)
                .withTenantId("tenant1").build();
        api.subscribe(subscription, listener);
        when(client.isRunning()).thenReturn(true);

        // when
        Assertions.assertThrows(Notifications2SubscriptionAlreadyEstablishedException.class,
                () -> api.subscribe(subscription, listener));

        // then
        verify(notificationSubscriptionApi, times(1)).subscribe(any());
        verify(pagedRep, times(1)).getSubscriptions();
        verify(client, times(1)).start();
    }

    @Test
    public void testUnsubscribeFromDeviceTarget() {
        // given
        Subscription subscription = Subscription.Builder.get().withId("test", "mySubscriber").withDeviceContextTargetApis("dev1", DeviceContextTargetApi.OPERATIONS)
                .withTenantId("tenant1").build();
        api.subscribe(subscription, listener);
        when(pagedRep.getSubscriptions()).thenReturn(Lists.list(mock(NotificationSubscriptionRepresentation.class)));

        verify(client, times(0)).stop(false);

        // when
        api.disconnect(subscription.getId(), false);

        // then
        verify(client, times(1)).stop(false);
    }

    @Test
    public void testUnsubscribeFromTenantTarget() {
        // given
        Subscription subscription = Subscription.Builder.get().withId("test", "mySubscriber").withTenantContextTargetApis(TenantContextTargetApi.EVENTS)
                .withTenantId("tenant1").build();
        api.subscribe(subscription, listener);
        when(pagedRep.getSubscriptions()).thenReturn(Lists.list(mock(NotificationSubscriptionRepresentation.class)));

        verify(client, times(0)).stop(true);

        // when
        api.disconnect(subscription.getId(), true);

        // then
        verify(client, times(1)).stop(true);
    }

    @Test
    public void testCreateTenantSubscription() {
        Subscription subscription = Subscription.Builder.get().withId("mySub", "mySubscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.OPERATIONS).withTenantId("tenant1").build();
        api.createSubscription(subscription);

        verify(notificationSubscriptionApi).subscribe(subscriptionCaptor.capture());
        NotificationSubscriptionRepresentation sub = subscriptionCaptor.getValue();
        assertNotNull(sub);
        assertEquals("mySub", sub.getSubscription());
        assertNull(sub.getSource());
        assertEquals(CONTEXT_TENANT, sub.getContext());
        assertTrue(sub.getSubscriptionFilter().getApis().contains("operations"));
        assertEquals(1, sub.getSubscriptionFilter().getApis().size());
    }

    @Test
    public void testCreateDeviceSubscription() {
        Subscription subscription = Subscription.Builder.get().withId("mySub", "mySubscriber")
                .withDeviceContextTargetApis("dev1", DeviceContextTargetApi.OPERATIONS).withTenantId("tenant1").build();
        api.createSubscription(subscription);

        verify(notificationSubscriptionApi).subscribe(subscriptionCaptor.capture());
        NotificationSubscriptionRepresentation sub = subscriptionCaptor.getValue();
        assertNotNull(sub);
        assertEquals("mySub", sub.getSubscription());
        assertEquals("dev1", sub.getSource().getId().getValue());
        assertEquals(CONTEXT_DEVICE, sub.getContext());
        assertTrue(sub.getSubscriptionFilter().getApis().contains("operations"));
        assertEquals(1, sub.getSubscriptionFilter().getApis().size());
    }

    @Test
    public void testSimpleFind() {
        // given
        NotificationSubscriptionRepresentation sub = mock(NotificationSubscriptionRepresentation.class);
        when(pagedRep.getSubscriptions()).thenReturn(Lists.list(sub));

        // when
        boolean result = api.subscriptionExists("test");

        // then
        verify(notificationSubscriptionApi).getSubscriptionsByFilter(filterCaptor.capture());
        assertEquals("test", filterCaptor.getValue().getSubscription());
        assertTrue(result);
    }

    @Test
    public void testDeleteExistingSubscription() {
        NotificationSubscriptionRepresentation sub = mock(NotificationSubscriptionRepresentation.class);
        when(pagedRep.getSubscriptions()).thenReturn(Lists.list(sub));

        api.delete(new Subscription.ID("name", "subscriber"));

        verify(notificationSubscriptionApi).delete(subscriptionCaptor.capture());
        NotificationSubscriptionRepresentation s = subscriptionCaptor.getValue();
        assertEquals(sub, s);
    }

    @Test
    public void testDeleteNonExistingSubscription() {
        when(pagedRep.getSubscriptions()).thenReturn(Collections.emptyList());
        api.delete(new Subscription.ID("name", "subscriber"));
        verify(notificationSubscriptionApi, times(0)).delete(any());
    }
}

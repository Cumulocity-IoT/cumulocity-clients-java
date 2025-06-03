package com.cumulocity.sdk.client.messaging.notifications;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionRepresentation;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.UrlProcessor;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApiImpl.MEDIA_TYPE;
import static com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApiImpl.SUBSCRIPTION_REQUEST_URI;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

public class NotificationSubscriptionApiImplTest {

    private RestConnector restConnector;
    private PlatformParameters platformParameters;
    private UrlProcessor urlProcessor;
    private NotificationSubscriptionApi api;

    private static final int DEFAULT_PAGE_SIZE = 3;
    private static final String DEFAULT_HOST = "host/";
    private static final String DEFAULT_GID_VALUE = "value";

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void initialize() {
        restConnector = mock(RestConnector.class);
        platformParameters = mock(PlatformParameters.class);
        urlProcessor = mock(UrlProcessor.class);
        api = new NotificationSubscriptionApiImpl(restConnector, urlProcessor, DEFAULT_PAGE_SIZE);

        when(platformParameters.getHost()).thenReturn(DEFAULT_HOST);
        when(restConnector.getPlatformParameters()).thenReturn(platformParameters);
        when(urlProcessor.replaceOrAddQueryParam(anyString(), any(Map.class))).thenCallRealMethod();
    }

    @Test
    public void restConnectorCannotBeNullInConstructor() {
        assertThrows(NullPointerException.class,
                () -> new NotificationSubscriptionApiImpl(null, urlProcessor, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void urlProcessorCannotBeNullInConstructor() {
        assertThrows(NullPointerException.class,
                () -> new NotificationSubscriptionApiImpl(restConnector, null, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void representationCannotBeNullInDelete() {
        assertThrows(NullPointerException.class,
                () -> api.delete(null));
    }

    @Test
    public void representationCannotBeNullInSubscribe() {
        assertThrows(NullPointerException.class,
                () -> api.subscribe(null));
    }

    @Test
    public void testSubscribe() {
        NotificationSubscriptionRepresentation subscription = new NotificationSubscriptionRepresentation();
        api.subscribe(subscription);
        verify(restConnector, atMost((1))).post(DEFAULT_HOST + SUBSCRIPTION_REQUEST_URI, MEDIA_TYPE, subscription);
    }

    @Test
    public void testDeleteById() {
        NotificationSubscriptionRepresentation subscription = new NotificationSubscriptionRepresentation();
        subscription.setId(new GId(DEFAULT_GID_VALUE));
        api.delete(subscription);
        verify(restConnector, atMost((1))).delete(DEFAULT_HOST + SUBSCRIPTION_REQUEST_URI + "/" + DEFAULT_GID_VALUE);
    }

    @Test
    public void testDeleteBySource() {
        api.deleteBySource(DEFAULT_GID_VALUE);
        verify(restConnector, atMost((1))).delete(DEFAULT_HOST + SUBSCRIPTION_REQUEST_URI + "?source=" + DEFAULT_GID_VALUE);
    }

    @SuppressWarnings("all")
    @Test
    public void testDeleteByFilter() {
        val filter = new NotificationSubscriptionFilter()
                .bySource(new GId(DEFAULT_GID_VALUE))
                .byContext("context");

        api.deleteByFilter(filter);
        verify(restConnector, atMost((1))).delete(DEFAULT_HOST + SUBSCRIPTION_REQUEST_URI + "?context=context&source=" + DEFAULT_GID_VALUE);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeleteByFilterThrowsRuntimeException() {
        assertThrows(RuntimeException.class, () -> api.deleteByFilter(null));
        assertThrows(RuntimeException.class, () -> api.deleteByFilter(new NotificationSubscriptionFilter()));
        assertThrows(RuntimeException.class, () -> api.deleteByFilter(new NotificationSubscriptionFilter().bySubscription("subscription")));
        assertThrows(RuntimeException.class, () -> api.deleteByFilter(new NotificationSubscriptionFilter().byTypeFilter("typeFilter")));
        assertThrows(RuntimeException.class, () -> api.deleteByFilter(new NotificationSubscriptionFilter().bySubscription("subscription").byTypeFilter("typeFilter")));
        verify(restConnector, never()).delete(anyString());
    }

    @Test
    public void testDeleteByTenantSubscription() {
        api.deleteTenantSubscriptions();
        verify(restConnector, atMost((1))).delete(DEFAULT_HOST + SUBSCRIPTION_REQUEST_URI + "?context=tenant");
    }
}

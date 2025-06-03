package com.cumulocity.sdk.client.messaging.notifications;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.UrlProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Stream;

import static com.cumulocity.sdk.client.messaging.notifications.SubscriptionContext.TENANT;
import static java.util.Objects.requireNonNull;

@Slf4j
@RequiredArgsConstructor
public class NotificationSubscriptionApiImpl implements NotificationSubscriptionApi {

    public static final CumulocityMediaType MEDIA_TYPE = new CumulocityMediaType("application", "json");
    public static final String SUBSCRIPTION_REQUEST_URI = "notification2/subscriptions";

    private final RestConnector restConnector;
    private final int pageSize;
    private final UrlProcessor urlProcessor;

    public NotificationSubscriptionApiImpl(
            RestConnector restConnector,
            UrlProcessor urlProcessor,
            int pageSize
    ) {
        this.restConnector = requireNonNull(restConnector, "restConnector");
        this.urlProcessor = requireNonNull(urlProcessor, "urlProcessor");
        this.pageSize = pageSize;
    }

    @Override
    public NotificationSubscriptionRepresentation subscribe(NotificationSubscriptionRepresentation representation) throws SDKException {
        requireNonNull(representation, "representation");
        return restConnector.post(getSelfUri(), MEDIA_TYPE, representation);
    }

    @Override
    public NotificationSubscriptionCollection getSubscriptions() throws SDKException {
        return new NotificationSubscriptionCollectionImpl(restConnector, getSelfUri(), pageSize);
    }

    @Override
    public NotificationSubscriptionCollection getSubscriptionsByFilter(NotificationSubscriptionFilter filter) throws SDKException {
        if (filter == null) return getSubscriptions();
        return new NotificationSubscriptionCollectionImpl(restConnector, urlProcessor.replaceOrAddQueryParam(getSelfUri(), filter.getQueryParams()), pageSize);
    }

    @Override
    public void delete(NotificationSubscriptionRepresentation subscription) throws SDKException {
        requireNonNull(subscription, "subscription");
        deleteById(subscription.getId().getValue());
    }

    @Override
    public void deleteById(String subscriptionId) {
        requireNonNull(subscriptionId, "subscriptionId");
        String url = getSelfUri() + "/" + subscriptionId;
        restConnector.delete(url);
    }

    @Override
    public void deleteByFilter(NotificationSubscriptionFilter filter) {
        requireNonNull(filter, "filter");
        final Map<String, String> params = filter.getQueryParams();
        if (params.isEmpty()) {
            throw new SDKException("Cannot delete by filter as filter is empty.");
        } else if (Stream.of(filter.getSubscription(), filter.getTypeFilter()).anyMatch(params::containsValue)) {
            throw new SDKException("Cannot delete by filter as filter contains unsupported parameters. 'subscription' and 'typeFilter' filters are not supported.");
        }
        restConnector.delete(urlProcessor.replaceOrAddQueryParam(getSelfUri(), params));
    }

    @Override
    public void deleteBySource(String source) {
        requireNonNull(source, "source");
        NotificationSubscriptionFilter filter = new NotificationSubscriptionFilter().bySource(new GId(source));
        deleteByFilter(filter);
    }

    @Override
    public void deleteTenantSubscriptions() {
        NotificationSubscriptionFilter filter = new NotificationSubscriptionFilter().byContext(TENANT.toString());
        deleteByFilter(filter);
    }

    private String getSelfUri() throws SDKException {
        return restConnector.getPlatformParameters().getHost() + SUBSCRIPTION_REQUEST_URI;
    }
}

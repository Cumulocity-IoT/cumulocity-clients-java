package com.cumulocity.sdk.client.messaging.notifications;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.reliable.notification.NotificationSubscriptionRepresentation;
import com.cumulocity.rest.representation.user.RoleReferenceRepresentation;
import com.cumulocity.rest.representation.user.RoleRepresentation;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReliableNotificationsIT extends JavaSdkITBase {

    private final NotificationSubscriptionApi subscriptionApi = platform.getNotificationSubscriptionApi();
    private final RestConnector restConnector = platform.createRestConnector();

    @BeforeEach
    public void setup() {
        // Set up and subscribe the current tenant's user to Notification 2.0 role
        val endpoint = "/user/%s/users/%s/roles".formatted(platform.getTenantId(), platform.getUser());
        val mediaType = CumulocityMediaType.valueOf("application/vnd.com.nsn.cumulocity.rolereference+json");
        val role = new RoleRepresentation();
        val payload = new RoleReferenceRepresentation();

        role.setSelf("%suser/roles/ROLE_NOTIFICATION_2_ADMIN".formatted(platform.getHost()));
        payload.setRole(role);
        restConnector.post(endpoint, mediaType, payload);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeleteByFilterSuccess() {
        val subscription = new NotificationSubscriptionRepresentation();
        subscription.setId(new GId());
        subscription.setContext("tenant");
        subscription.setSubscription("subscription");

        subscriptionApi.subscribe(subscription);

        val filter = new NotificationSubscriptionFilter()
                .bySource(subscription.getId())
                .byContext("tenant");

        subscriptionApi.deleteByFilter(filter);

        val subscriptions = subscriptionApi.getSubscriptionsByFilter(filter).get(0).getSubscriptions();
        assertThat(subscriptions).isEmpty();
    }
}

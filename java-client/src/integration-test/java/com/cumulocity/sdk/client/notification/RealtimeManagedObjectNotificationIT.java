package com.cumulocity.sdk.client.notification;

import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import com.cumulocity.rest.representation.builder.ManagedObjectRepresentationBuilder;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.cumulocity.sdk.client.common.TestSubscriptionListener;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeDeleteMessage;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeManagedObjectMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anMoRepresentationLike;
import static com.cumulocity.rest.representation.builder.SampleManagedObjectRepresentation.MO_REPRESENTATION;
import static com.cumulocity.sdk.client.common.Subscribers.getSubscriberForType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RealtimeManagedObjectNotificationIT extends JavaSdkITBase {
    private static final String MANAGEDOBJECTS = "/managedobjects/";

    final InventoryApi inventoryApi = platform.getInventoryApi();

    //MTM-45152
    @Disabled
    @Test
    public void shouldReceiveCreateMONotification() {
        // given
        Subscriber<String, AbstractExtensibleRepresentation> subscriber = getSubscriberForType(AbstractExtensibleRepresentation.class, platform);
        TestSubscriptionListener<AbstractExtensibleRepresentation> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(MANAGEDOBJECTS + "*", subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        ManagedObjectRepresentation managedObjectRep = inventoryApi.create(aSampleMo().build());

        // then
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(subscriptionListener.getNotifications())
                .anySatisfy(notification -> assertCreateManagedObjectNotification(notification, managedObjectRep)));

        // cleanup
        subscriber.disconnect();
    }

    @Test
    public void shouldReceiveUpdateMONotification() {
        // given
        Subscriber<String, RealtimeManagedObjectMessage> subscriber = getSubscriberForType(RealtimeManagedObjectMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        ManagedObjectRepresentation managedObjectRepresentation = new ManagedObjectRepresentation();
        managedObjectRepresentation.setName("New Name");
        managedObjectRepresentation.setId(mo.getId());
        TestSubscriptionListener<RealtimeManagedObjectMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(MANAGEDOBJECTS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        ManagedObjectRepresentation updatedMO = inventoryApi.update(managedObjectRepresentation);

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeManagedObjectMessage.class);
        assertUpdateManagedObjectNotification(subscriptionListener.getNotification(), updatedMO);

        // cleanup
        subscriber.disconnect();
    }

    @Test
    public void shouldReceiveDeleteMONotification() {
        // given
        Subscriber<String, RealtimeDeleteMessage> subscriber = getSubscriberForType(RealtimeDeleteMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        TestSubscriptionListener<RealtimeDeleteMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(MANAGEDOBJECTS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        inventoryApi.delete(mo.getId());

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeDeleteMessage.class);
        assertDeleteManagedObjectNotification(subscriptionListener.getNotification(), mo);

        // cleanup
        subscriber.disconnect();
    }

    private static ManagedObjectRepresentationBuilder aSampleMo() {
        return anMoRepresentationLike(MO_REPRESENTATION);
    }

    @SuppressWarnings({"rawtypes"})
    private void assertCreateManagedObjectNotification(AbstractExtensibleRepresentation notification, ManagedObjectRepresentation source) {
        assertThat(notification.getAttrs().get("data")).isNotNull();
        assertThat(notification.getAttrs().get("data").getClass()).isEqualTo(HashMap.class);
        assertThat(((HashMap) notification.getAttrs().get("data")).get("id")).isEqualTo(source.getId().getValue());
        assertThat(((HashMap) notification.getAttrs().get("data")).get("type")).isEqualTo(source.getType());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo("CREATE");
    }

    private void assertUpdateManagedObjectNotification(RealtimeManagedObjectMessage notification, ManagedObjectRepresentation source) {
        assertThat(notification.getData().getId()).isEqualTo(source.getId());
        assertThat(notification.getData().getType()).isEqualTo(source.getType());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo("UPDATE");
    }

    private void assertDeleteManagedObjectNotification(RealtimeDeleteMessage notification, ManagedObjectRepresentation source) {
        assertThat(notification.getData()).isEqualTo(source.getId().getValue());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo("DELETE");
    }
}

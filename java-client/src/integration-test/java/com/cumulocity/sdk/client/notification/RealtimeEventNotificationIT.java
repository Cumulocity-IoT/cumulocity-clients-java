package com.cumulocity.sdk.client.notification;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.builder.ManagedObjectRepresentationBuilder;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.cumulocity.sdk.client.common.TestSubscriptionListener;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeDeleteMessage;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeEventMessage;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anMoRepresentationLike;
import static com.cumulocity.rest.representation.builder.SampleManagedObjectRepresentation.MO_REPRESENTATION;
import static com.cumulocity.sdk.client.common.Subscribers.getSubscriberForType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RealtimeEventNotificationIT extends JavaSdkITBase {
    private static final String EVENTS = "/events/";
    private static final String EVENTS_WITH_CHILDREN = "/eventsWithChildren/";

    final InventoryApi inventoryApi = platform.getInventoryApi();
    final EventApi eventApi = platform.getEventApi();

    private Subscriber<String, RealtimeEventMessage> subscriber;

    @AfterEach
    public void cleanUp() {
        if (subscriber != null) {
            subscriber.disconnect();
        }
    }

    @Test
    public void shouldReceiveCreateEventNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeEventMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        TestSubscriptionListener<RealtimeEventMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(EVENTS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        EventRepresentation event = eventApi.create(createEventRep(mo));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeEventMessage.class);
        assertEventNotification(subscriptionListener.getNotification(), event, "CREATE");
    }

    @Test
    public void shouldReceiveUpdateEventNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeEventMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        EventRepresentation event = eventApi.create(createEventRep(mo));
        TestSubscriptionListener<RealtimeEventMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(EVENTS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        EventRepresentation updatedEvent = eventApi.update(updateEventRep(event.getId()));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeEventMessage.class);
        assertEventNotification(subscriptionListener.getNotification(), updatedEvent, "UPDATE");
    }

    @Test
    public void shouldReceiveDeleteEventNotification() {
        // given
        Subscriber<String, RealtimeDeleteMessage> subscriber = getSubscriberForType(RealtimeDeleteMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        EventRepresentation event = eventApi.create(createEventRep(mo));
        TestSubscriptionListener<RealtimeDeleteMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(EVENTS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        eventApi.delete(event);

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeDeleteMessage.class);
        assertDeleteEventNotification(subscriptionListener.getNotification(), event);

        // cleanup
        subscriber.disconnect();
    }

    @Test
    public void shouldReceiveCreateChildEventNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeEventMessage.class, platform);
        ManagedObjectRepresentation parentMO = inventoryApi.create(aSampleMo().build());
        ManagedObjectRepresentation childMo = aSampleChildMo(parentMO.getId());
        TestSubscriptionListener<RealtimeEventMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(EVENTS_WITH_CHILDREN + parentMO.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        EventRepresentation event = eventApi.create(createEventRep(childMo));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeEventMessage.class);
        assertEventNotification(subscriptionListener.getNotification(), event, "CREATE");
    }

    @Test
    public void shouldReceiveUpdateChildEventNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeEventMessage.class, platform);
        ManagedObjectRepresentation parentMO = inventoryApi.create(aSampleMo().build());
        ManagedObjectRepresentation childMo = aSampleChildMo(parentMO.getId());
        EventRepresentation event = eventApi.create(createEventRep(childMo));
        TestSubscriptionListener<RealtimeEventMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(EVENTS_WITH_CHILDREN + parentMO.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        EventRepresentation updatedEvent = eventApi.update(updateEventRep(event.getId()));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeEventMessage.class);
        assertEventNotification(subscriptionListener.getNotification(), updatedEvent, "UPDATE");
    }

    @Test
    public void shouldReceiveDeleteChildEventNotification() {
        // given
        Subscriber<String, RealtimeDeleteMessage> subscriber = getSubscriberForType(RealtimeDeleteMessage.class, platform);
        ManagedObjectRepresentation parentMO = inventoryApi.create(aSampleMo().build());
        ManagedObjectRepresentation childMo = aSampleChildMo(parentMO.getId());
        EventRepresentation event = eventApi.create(createEventRep(childMo));
        TestSubscriptionListener<RealtimeDeleteMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(EVENTS_WITH_CHILDREN + parentMO.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        eventApi.delete(event);

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeDeleteMessage.class);
        assertDeleteEventNotification(subscriptionListener.getNotification(), event);

        // cleanup
        subscriber.disconnect();
    }

    private static ManagedObjectRepresentationBuilder aSampleMo() {
        return anMoRepresentationLike(MO_REPRESENTATION);
    }

    private ManagedObjectRepresentation aSampleChildMo(GId parentMoId) {
        ManagedObjectRepresentation child1 = inventoryApi.create(aSampleMo().withName("child1").build());
        return inventoryApi.getManagedObjectApi(parentMoId).addChildDevice(child1.getId()).getManagedObject();
    }

    private EventRepresentation createEventRep(ManagedObjectRepresentation source) {
        EventRepresentation eventRepresentation = new EventRepresentation();
        eventRepresentation.setText("Event text");
        eventRepresentation.setSource(source);
        eventRepresentation.setType("Event_type");
        eventRepresentation.setDateTime(new DateTime());
        return eventRepresentation;
    }

    private EventRepresentation updateEventRep(GId eventId) {
        EventRepresentation eventRepresentation = new EventRepresentation();
        eventRepresentation.setId(eventId);
        eventRepresentation.setText("New event text");
        return eventRepresentation;
    }

    private void assertEventNotification(RealtimeEventMessage notification, EventRepresentation source, String realtimeAction) {
        assertThat(notification.getData().getId()).isEqualTo(source.getId());
        assertThat(notification.getData().getType()).isEqualTo(source.getType());
        assertThat(notification.getData().getSource()).isEqualToComparingFieldByField(source.getSource());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo(realtimeAction);
    }

    private void assertDeleteEventNotification(RealtimeDeleteMessage notification, EventRepresentation source) {
        assertThat(notification.getData()).isEqualTo(source.getId().getValue());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo("DELETE");
    }
}

package com.cumulocity.sdk.client.notification;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.builder.ManagedObjectRepresentationBuilder;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.cumulocity.sdk.client.common.TestSubscriptionListener;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeAlarmMessage;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anAlarmRepresentationLike;
import static com.cumulocity.rest.representation.builder.RestRepresentationObjectMother.anMoRepresentationLike;
import static com.cumulocity.rest.representation.builder.SampleAlarmRepresentation.ALARM_REPRESENTATION;
import static com.cumulocity.rest.representation.builder.SampleManagedObjectRepresentation.MO_REPRESENTATION;
import static com.cumulocity.sdk.client.common.Subscribers.getSubscriberForType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RealtimeAlarmNotificationIT extends JavaSdkITBase {
    private static final String ALARMS = "/alarms/";
    private static final String ALARMS_WITH_CHILDREN = "/alarmsWithChildren/";

    final InventoryApi inventoryApi = platform.getInventoryApi();
    final AlarmApi alarmApi = platform.getAlarmApi();

    Subscriber<String, RealtimeAlarmMessage> subscriber;

    @AfterEach
    public void cleanUp() {
        if (subscriber != null) {
            subscriber.disconnect();
        }
    }

    @Test
    public void shouldReceiveCreateAlarmNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeAlarmMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        TestSubscriptionListener<RealtimeAlarmMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(ALARMS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        AlarmRepresentation alarm = alarmApi.create(createAlarmRep(mo));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeAlarmMessage.class);
        assertAlarmNotification(subscriptionListener.getNotification(), alarm, "CREATE");
    }

    // Test is failing because source fields in received notification are not the same as in updated alarm
    // https://cumulocity.atlassian.net/browse/MTM-38784
    @Disabled
    @Test
    public void shouldReceiveUpdateAlarmNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeAlarmMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        AlarmRepresentation alarm = alarmApi.create(createAlarmRep(mo));
        TestSubscriptionListener<RealtimeAlarmMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(ALARMS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        AlarmRepresentation updatedAlarm = alarmApi.update(updateAlarmRep(alarm.getId()));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeAlarmMessage.class);
        assertAlarmNotification(subscriptionListener.getNotification(), updatedAlarm, "UPDATE");
    }

    @Test
    public void shouldReceiveCreateChildAlarmNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeAlarmMessage.class, platform);
        ManagedObjectRepresentation parentMO = inventoryApi.create(aSampleMo().build());
        ManagedObjectRepresentation childMo = aSampleChildMo(parentMO.getId());
        TestSubscriptionListener<RealtimeAlarmMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(ALARMS_WITH_CHILDREN + parentMO.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        AlarmRepresentation alarm = alarmApi.create(createAlarmRep(childMo));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeAlarmMessage.class);
        assertAlarmNotification(subscriptionListener.getNotification(), alarm, "CREATE");
    }

    // Test is failing because source fields in received notification are not the same as in updated alarm
    // https://cumulocity.atlassian.net/browse/MTM-38784
    @Disabled
    @Test
    public void shouldReceiveUpdateChildAlarmNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeAlarmMessage.class, platform);
        ManagedObjectRepresentation parentMO = inventoryApi.create(aSampleMo().build());
        ManagedObjectRepresentation childMo = aSampleChildMo(parentMO.getId());
        AlarmRepresentation alarm = alarmApi.create(createAlarmRep(childMo));
        TestSubscriptionListener<RealtimeAlarmMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(ALARMS_WITH_CHILDREN + parentMO.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        AlarmRepresentation updatedAlarm = alarmApi.update(updateAlarmRep(alarm.getId()));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeAlarmMessage.class);
        assertAlarmNotification(subscriptionListener.getNotification(), updatedAlarm, "UPDATE");
    }

    private static ManagedObjectRepresentationBuilder aSampleMo() {
        return anMoRepresentationLike(MO_REPRESENTATION);
    }

    private ManagedObjectRepresentation aSampleChildMo(GId parentMoId) {
        ManagedObjectRepresentation child1 = inventoryApi.create(aSampleMo().withName("child1").build());
        return inventoryApi.getManagedObjectApi(parentMoId).addChildDevice(child1.getId()).getManagedObject();
    }

    private AlarmRepresentation createAlarmRep(ManagedObjectRepresentation source) {
        return anAlarmRepresentationLike(ALARM_REPRESENTATION)
                .withType("com_nsn_bts_TrxFaulty")
                .withStatus("ACTIVE")
                .withSeverity("major")
                .withSource(source)
                .withText("Alarm for mo")
                .withDateTime(new DateTime()).build();
    }

    private AlarmRepresentation updateAlarmRep(GId alarmId) {
        AlarmRepresentation alarmRepresentation = new AlarmRepresentation();
        alarmRepresentation.setId(alarmId);
        alarmRepresentation.setText("New alarm text");
        alarmRepresentation.setSeverity("minor");
        return alarmRepresentation;
    }

    private void assertAlarmNotification(RealtimeAlarmMessage notification, AlarmRepresentation source, String realtimeAction) {
        assertThat(notification.getData().getId()).isEqualTo(source.getId());
        assertThat(notification.getData().getType()).isEqualTo(source.getType());
        assertThat(notification.getData().getSource()).isEqualToComparingFieldByField(source.getSource());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo(realtimeAction);
    }
}

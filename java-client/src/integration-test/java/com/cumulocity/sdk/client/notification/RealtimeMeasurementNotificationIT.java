package com.cumulocity.sdk.client.notification;

import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.builder.ManagedObjectRepresentationBuilder;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.cumulocity.sdk.client.common.TestSubscriptionListener;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeDeleteMessage;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeMeasurementMessage;
import com.cumulocity.sdk.client.option.TenantOptionApi;
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

public class RealtimeMeasurementNotificationIT extends JavaSdkITBase {
    private static final String MEASUREMENTS = "/measurements/";

    final InventoryApi inventoryApi = platform.getInventoryApi();
    final MeasurementApi measurementApi = platform.getMeasurementApi();
    final TenantOptionApi tenantOptionApi = platform.getTenantOptionApi();

    @AfterEach
    public void deleteMeasurements() {
        deleteTenantOptionWhenPresent();
    }

    private void deleteTenantOptionWhenPresent() {
        try {
            tenantOptionApi.delete(new OptionPK("configuration", "timeseries.mongodb.collections.mode"));
        } catch (SDKException e) {

        }
    }

    @Test
    public void shouldReceiveCreateMeasurementNotification() throws Exception {
        // given
        Subscriber<String, RealtimeMeasurementMessage> subscriber = getSubscriberForType(RealtimeMeasurementMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        TestSubscriptionListener<RealtimeMeasurementMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(MEASUREMENTS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        MeasurementRepresentation measurement = measurementApi.create(createMeasurementRep(mo));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeMeasurementMessage.class);
        assertMeasurementNotification(subscriptionListener.getNotification(), measurement, "CREATE");

        subscriber.disconnect();
    }

    @Test
    public void shouldReceiveDeleteMeasurementNotification() throws Exception {
        // given
        disableTimeseries();
        Subscriber<String, RealtimeDeleteMessage> subscriber = getSubscriberForType(RealtimeDeleteMessage.class, platform);
        ManagedObjectRepresentation mo = inventoryApi.create(aSampleMo().build());
        MeasurementRepresentation measurement = measurementApi.create(createMeasurementRep(mo));
        TestSubscriptionListener<RealtimeDeleteMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(MEASUREMENTS + mo.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        measurementApi.delete(measurement);

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeDeleteMessage.class);
        assertDeleteMeasurementNotification(subscriptionListener.getNotification(), measurement);

        subscriber.disconnect();
    }

    public void disableTimeseries() {
        OptionRepresentation disableTimeseries = OptionRepresentation.asOptionRepresentation("configuration", "timeseries.mongodb.collections.mode", "DISABLED");
            tenantOptionApi.save(disableTimeseries);
    }

    private static ManagedObjectRepresentationBuilder aSampleMo() {
        return anMoRepresentationLike(MO_REPRESENTATION);
    }

    private MeasurementRepresentation createMeasurementRep(ManagedObjectRepresentation source) {
        MeasurementRepresentation rep = new MeasurementRepresentation();
        rep.setType("com.type1");
        rep.setDateTime(new DateTime());
        rep.setSource(source);
        return rep;
    }

    private void assertMeasurementNotification(RealtimeMeasurementMessage notification, MeasurementRepresentation source, String realtimeAction) {
        assertThat(notification.getData().getId()).isEqualTo(source.getId());
        assertThat(notification.getData().getType()).isEqualTo(source.getType());
        assertThat(notification.getData().getSource()).isEqualToComparingFieldByField(source.getSource());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo(realtimeAction);
    }

    private void assertDeleteMeasurementNotification(RealtimeDeleteMessage notification, MeasurementRepresentation source) {
        assertThat(notification.getData()).isEqualTo(source.getId().getValue());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo("DELETE");
    }
}

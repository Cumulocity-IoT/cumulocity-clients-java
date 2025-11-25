package com.cumulocity.sdk.client.notification;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.cumulocity.sdk.client.common.TestSubscriptionListener;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.notification.wrappers.RealtimeOperationMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static com.cumulocity.sdk.client.common.Subscribers.getSubscriberForType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RealtimeOperationNotificationIT extends JavaSdkITBase {
    private static final String OPERATIONS = "/operations/";

    private final InventoryApi inventoryApi = platform.getInventoryApi();
    private final DeviceControlApi operationsApi = platform.getDeviceControlApi();

    private Subscriber<String, RealtimeOperationMessage> subscriber;

    @AfterEach
    public void cleanUp() {
        if (subscriber != null) {
            subscriber.disconnect();
        }
    }

    @Test
    public void shouldReceiveCreateOperationNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeOperationMessage.class, platform);
        ManagedObjectRepresentation device = inventoryApi.create(aMoDevice());
        TestSubscriptionListener<RealtimeOperationMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        subscriber.subscribe(OPERATIONS + device.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        OperationRepresentation operation = operationsApi.create(aDeviceOperation(device.getId()));

        // then
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeOperationMessage.class);
        assertOperationNotification(subscriptionListener.getNotification(), operation, "CREATE");
    }

    @Test
    public void shouldReceiveUpdateOperationNotification() {
        // given
        subscriber = getSubscriberForType(RealtimeOperationMessage.class, platform);
        ManagedObjectRepresentation device = inventoryApi.create(aMoDevice());
        OperationRepresentation operation = operationsApi.create(aDeviceOperation(device.getId()));
        TestSubscriptionListener<RealtimeOperationMessage> subscriptionListener = new TestSubscriptionListener<>();

        // when
        assertThat(operation.getStatus()).isEqualTo("PENDING");
        subscriber.subscribe(OPERATIONS + device.getId().getValue(), subscriptionListener, subscriptionListener, true);
        await().atMost(TEN_SECONDS).until(subscriptionListener::isSubscribed);
        waitToEnsureCoreSubscriptionCacheIsInvalidated();
        OperationRepresentation updatedOperation = operationsApi.update(aDeviceOperationUpdate(operation.getId()));

        // then
        assertThat(updatedOperation.getStatus()).isEqualTo("EXECUTING");
        await().atMost(TEN_SECONDS).until(subscriptionListener::notificationReceived);
        assertThat(subscriptionListener.getNotification()).isExactlyInstanceOf(RealtimeOperationMessage.class);
        assertOperationNotification(subscriptionListener.getNotification(), updatedOperation, "UPDATE");
    }

    private ManagedObjectRepresentation aMoDevice() {
        ManagedObjectRepresentation mo;
        mo = new ManagedObjectRepresentation();
        mo.setName("Test Device");
        mo.setType("NotificationsDevice");
        mo.setProperty("c8y_IsDevice", new HashMap<>());
        mo.setProperty("com_cumulocity_model_Agent", new HashMap<>());
        return mo;
    }

    private OperationRepresentation aDeviceOperation(GId deviceId) {
        OperationRepresentation operation = new OperationRepresentation();
        operation.setDeviceId(deviceId);
        operation.setProperty("Restart", new HashMap<>());
        return operation;
    }

    private OperationRepresentation aDeviceOperationUpdate(GId operationId) {
        OperationRepresentation operation = new OperationRepresentation();
        operation.setId(operationId);
        operation.setStatus("EXECUTING");
        return operation;
    }

    private void assertOperationNotification(RealtimeOperationMessage notification, OperationRepresentation source, String realtimeAction) {
        assertThat(notification.getData().getId()).isEqualTo(source.getId());
        assertThat(notification.getData().getStatus()).isEqualTo(source.getStatus());
        assertThat(notification.getData().getDeviceId()).isEqualTo(source.getDeviceId());
        assertThat(notification.getAttrs().get("realtimeAction")).isEqualTo(realtimeAction);
    }
}

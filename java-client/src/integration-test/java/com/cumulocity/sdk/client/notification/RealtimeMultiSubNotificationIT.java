package com.cumulocity.sdk.client.notification;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.common.JavaSdkITBase;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cumulocity.sdk.client.notification.SubscriberBuilder.REALTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

@Slf4j
public class RealtimeMultiSubNotificationIT extends JavaSdkITBase {
    private static final int CONCURRENT_JOBS = 10;

    InventoryApi inventoryApi = platform.getInventoryApi();
    DeviceControlApi operationsApi = platform.getDeviceControlApi();

    @Test
    public void shouldNotReceiveDuplicatedOperations() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_JOBS);

        List<Callable<Boolean>> jobs = new ArrayList<>();
        for (int jobNo = 0; jobNo < CONCURRENT_JOBS; ++jobNo) {
            jobs.add(new SubscriberJob(jobNo));
        }

        boolean failed = false;
        for (Future<Boolean> result : executor.invokeAll(jobs)) {
            if (!result.get()) {
                failed = true;
                break;
            }
        }

        assertThat(failed)
                .withFailMessage("Test failed. See the logs to investigate the issue.")
                .isFalse();
    }

    private class SubscriberJob implements Callable<Boolean> {
        private final int jobNo;
        private final Subscriber<String, RealtimeMessage> subscriber;

        public SubscriberJob(int jobNo) {
            this.jobNo = jobNo;
            this.subscriber = SubscriberBuilder.<String, RealtimeMessage>anSubscriber()
                    .withParameters(platform)
                    .withEndpoint(REALTIME)
                    .withSubscriptionNameResolver(name -> name)
                    .withDataType(RealtimeMessage.class)
                    .build();
        }

        @Override
        public Boolean call() throws InterruptedException {
            ManagedObjectRepresentation device = inventoryApi.create(aMoDevice());

            AtomicInteger opCounter = new AtomicInteger(0);
            List<Subscription<String>> subscriptions = subscribe(device.getId(), opCounter);
            waitToEnsureCoreSubscriptionCacheIsInvalidated();
            operationsApi.create(aDeviceOperation(device.getId()));

            try {
                // check counter as fast as possible and then wait 1s to verify it didn't go up
                await().atMost(TEN_SECONDS).until(() -> opCounter.get() == 1);
                await().pollDelay(ONE_SECOND).atMost(TEN_SECONDS).until(() -> opCounter.get() == 1);
            } catch (ConditionTimeoutException ex) {
                log.error("Subscription test failed with notifications count {} (should be 1)", opCounter.get());
                return false;
            }

            subscriptions.forEach(Subscription::unsubscribe);
            subscriber.disconnect();

            return opCounter.get() == 1;
        }

        private List<Subscription<String>> subscribe(GId deviceId, AtomicInteger opCounter) throws InterruptedException {
            CountDownLatch subOpLatch = new CountDownLatch(5);

            List<Subscription<String>> subscriptions = Stream.of("measurements", "alarms", "events", "managedobjects")
                    .map(channel -> subscriber.subscribe(
                            "/" + channel + "/*",
                            new CountDownLatchSubscriptionOperationListener(jobNo, subOpLatch),
                            new CountingSubscriptionListener(jobNo, opCounter, deviceId),
                            true))
                    .collect(Collectors.toList());

            subscriptions.add(subscriber.subscribe(
                    "/operations/*",
                    new CountDownLatchSubscriptionOperationListener(jobNo, subOpLatch),
                    new CountingSubscriptionListener(jobNo, opCounter, deviceId),
                    true));


            assertThat(subOpLatch.await(10, TimeUnit.SECONDS))
                    .withFailMessage("There are %d not successful subscriptions left)", subOpLatch.getCount())
                    .isTrue();

            return subscriptions;
        }

        private ManagedObjectRepresentation aMoDevice() {
            ManagedObjectRepresentation mo;
            mo = new ManagedObjectRepresentation();
            mo.setName("Test Device " + jobNo);
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
    }

    @RequiredArgsConstructor
    private static class CountingSubscriptionListener implements SubscriptionListener<String, RealtimeMessage> {
        private final int jobNo;
        private final AtomicInteger notificationCount;
        private final GId deviceId;

        @SuppressWarnings("unchecked")
        @Override
        public void onNotification(Subscription<String> subscription, RealtimeMessage notification) {
            log.info("SubscriberJob-{}: Received job for {}, notification.realtimeAction={}, notification.data={}",
                    jobNo, subscription.getObject(), notification.getRealtimeAction(), notification.getData());
            if (subscription.getObject().equals("/operations/*")) {
                Object data = notification.getData();
                if (data instanceof Map) {
                    String notificationDeviceId = (String) ((Map<String, Object>) data).get("deviceId");
                    if (deviceId.getValue().equals(notificationDeviceId)) {
                        log.info("SubscriberJob-{}: Received operation notification for deviceId = {}", jobNo, deviceId.getValue());
                        notificationCount.incrementAndGet();
                    }
                }
            }
        }

        @Override
        public void onError(Subscription<String> subscription, Throwable ex) {
            log.error("SubscriberJob-{}: Received error for {}", jobNo, subscription.getObject(), ex);
        }
    }

    @RequiredArgsConstructor
    private static class CountDownLatchSubscriptionOperationListener implements SubscribeOperationListener {
        private final int jobNo;
        private final CountDownLatch subscriptionLatch;

        @Override
        public void onSubscribingSuccess(String channelId) {
            subscriptionLatch.countDown();
            log.info("SubscriberJob-{}: Successfully subscribed: {}", jobNo, channelId);
        }

        @Override
        public void onSubscribingError(String channelId, String message, Throwable throwable) {
            log.warn("SubscriberJob-{}: Error when subscribing channel: {}, error: {}", jobNo, channelId, message, throwable);
        }
    }

    @Getter
    @Setter
    public static class RealtimeMessage extends AbstractExtensibleRepresentation {
        private Object data;
        private String realtimeAction;
    }
}

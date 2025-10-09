package com.cumulocity.sdk.client.notification2;

import com.cumulocity.sdk.client.notification2.exception.Notifications2FieldInvalidException;
import com.cumulocity.sdk.client.notification2.exception.Notifications2FieldRequiredException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class SubscriptionBuilderTest {
    @Test
    public void testId() {
        // no ID
        Notifications2FieldRequiredException e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                () -> Subscription.Builder.get().withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1").build());
        assertEquals("subscriptionName", e.getField());

        // null or blank name
        String[] samples = {"", null, " ", "\t\t "};
        for (String name : samples) {
            e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                    () -> Subscription.Builder.get().withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1")
                            .withId(name, "subscriber").build());
            assertEquals("subscriptionName", e.getField());
        }
        // null or blank subscriber
        for (String subscriber : samples) {
            e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                    () -> Subscription.Builder.get().withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1")
                            .withId("name", subscriber).build());
            assertEquals("subscriber", e.getField());
        }

        // invalid characters in subscriber
        samples = new String[]{"$%%^^#@#$", "ThisIsInvalidSubscriber!"};
        for (String subscriber : samples) {
            Notifications2FieldInvalidException e2 = Assertions.assertThrows(Notifications2FieldInvalidException.class,
                    () -> Subscription.Builder.get().withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1")
                            .withId("name", subscriber).build());
            assertEquals("subscriber", e2.getField());
        }

        // valid
        Subscription s = Subscription.Builder.get().withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1")
                .withId("name", "subscriber").build();
        assertEquals("name", s.getId().getName());
        assertEquals("subscriber", s.getId().getSubscriber());
        assertEquals(TenantContextTargetApi.ALARMS.getTarget(), s.getTargetApis().toArray()[0]);
        assertEquals("tenant1", s.getTenantId());
        assertNull(s.getDeviceId());
        assertEquals(AckMode.IMMEDIATE, s.getAckMode());
        assertEquals("tenant1", s.getTenantId());
        assertNull(s.getTypeFilter());
        assertFalse(s.isPersistent());
        assertFalse(s.isShared());
        assertTrue(s.isTenantSubscription());
    }

    @Test
    public void testTenantId() {
        Notifications2FieldRequiredException e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                () -> Subscription.Builder.get().withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withId("name", "subscriber").build());
        assertEquals("tenantId", e.getField());

        // blank
        String[] samples = {"", null, " ", "\t\t "};
        for (String tenantId : samples) {
            e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                    () -> Subscription.Builder.get().withTenantId(tenantId));
            assertEquals("tenantId", e.getField());
        }

        // invalid
        samples = new String[] {"asd!!!333", "%$^$%^$"};
        for (String tenantId : samples) {
            Notifications2FieldInvalidException e2 = Assertions.assertThrows(Notifications2FieldInvalidException.class,
                    () -> Subscription.Builder.get().withTenantId(tenantId));
            assertEquals("tenantId", e2.getField());
        }

        // valid
        Subscription s = Subscription.Builder.get().withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withId("name", "subscriber")
                .withTenantId("tenant1").build();
        assertEquals("name", s.getId().getName());
        assertEquals("subscriber", s.getId().getSubscriber());
        assertEquals(TenantContextTargetApi.ALARMS.getTarget(), s.getTargetApis().toArray()[0]);
        assertEquals("tenant1", s.getTenantId());
        assertNull(s.getDeviceId());
        assertEquals(AckMode.IMMEDIATE, s.getAckMode());
        assertEquals("tenant1", s.getTenantId());
        assertNull(s.getTypeFilter());
        assertFalse(s.isPersistent());
        assertFalse(s.isShared());
        assertTrue(s.isTenantSubscription());
    }

    @Test
    public void testTenantTarget() {
        Notifications2FieldRequiredException e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                () -> Subscription.Builder.get().withId("name", "subscriber")
                .withTenantId("tenant1").build());
        assertEquals("target", e.getField());

        e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                () -> Subscription.Builder.get().withId("name", "subscriber")
                        .withTenantContextTargetApis(null).withTenantId("tenant1").build());
        assertEquals("target", e.getField());

        Subscription s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1").build();
        assertEquals(TenantContextTargetApi.ALARMS.getTarget(), s.getTargetApis().toArray()[0]);
        assertNull(s.getDeviceId());

        s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1")
                .withDeviceContextTargetApis("dev1", DeviceContextTargetApi.EVENTS)
                .withTenantContextTargetApis(TenantContextTargetApi.MANAGED_OBJECTS).build();
        assertEquals(TenantContextTargetApi.MANAGED_OBJECTS.getTarget(), s.getTargetApis().toArray()[0]);
        assertNull(s.getDeviceId());
        assertTrue(s.isTenantSubscription());
    }

    @Test
    public void testDeviceTarget() {
        Notifications2FieldRequiredException e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                () -> Subscription.Builder.get().withId("name", "subscriber")
                        .withDeviceContextTargetApis("dev1", null).withTenantId("tenant1").build());
        assertEquals("target", e.getField());

        String[] samples = {"", null, " ", "\t\t "};
        for (String sample : samples) {
            e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                    () -> Subscription.Builder.get().withId("name", "subscriber")
                            .withDeviceContextTargetApis(sample, DeviceContextTargetApi.EVENTS).withTenantId("tenant1").build());
            assertEquals("deviceId", e.getField());
        }

        Subscription s = Subscription.Builder.get().withId("name", "subscriber")
                .withDeviceContextTargetApis("dev1", DeviceContextTargetApi.OPERATIONS).withTenantId("tenant1").build();
        assertEquals(DeviceContextTargetApi.OPERATIONS.getTarget(), s.getTargetApis().toArray()[0]);
        assertNotNull(s.getDeviceId());

        s = Subscription.Builder.get().withId("name", "subscriber")
                .withDeviceContextTargetApis("dev1", DeviceContextTargetApi.EVENTS)
                .withTenantContextTargetApis(TenantContextTargetApi.ALARMS).withTenantId("tenant1")
                .withDeviceContextTargetApis("dev1", DeviceContextTargetApi.EVENTS)
                .build();
        assertEquals(DeviceContextTargetApi.EVENTS.getTarget(), s.getTargetApis().toArray()[0]);
        assertNotNull(s.getDeviceId());
        assertFalse(s.isTenantSubscription());
    }

    @Test
    public void testAckMode() {
        Notifications2FieldRequiredException e = Assertions.assertThrows(Notifications2FieldRequiredException.class,
                () -> Subscription.Builder.get().withId("name", "subscriber")
                        .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                        .withAckMode(null).build());
        assertEquals("ackMode", e.getField());

        for (AckMode ackMode : AckMode.values()) {
            Subscription s = Subscription.Builder.get().withId("name", "subscriber")
                    .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                    .withAckMode(ackMode).build();
            assertEquals(ackMode, s.getAckMode());
        }
    }

    @Test
    public void testTypeFilter() {
        String[] samples = {"", " ", null, "\t\t "};
        for (String sample : samples) {
            Subscription s = Subscription.Builder.get().withId("name", "subscriber")
                    .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                    .withTypeFilter(sample).build();
            assertNull(s.getTypeFilter());
        }

        Subscription s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                .withTypeFilter("c8y_test").build();
        assertEquals("c8y_test", s.getTypeFilter());
    }

    @Test
    public void testShared() {
        Subscription s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                .withShared(true).build();
        assertTrue(s.isShared());

        s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                .withShared(false).build();
        assertFalse(s.isShared());

        // default false
        s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                .build();
        assertFalse(s.isShared());
    }

    @Test
    public void testPersistent() {
        Subscription s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                .withPersistent(true).build();
        assertTrue(s.isPersistent());

        s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                .withPersistent(false).build();
        assertFalse(s.isPersistent());

        // default false
        s = Subscription.Builder.get().withId("name", "subscriber")
                .withTenantContextTargetApis(TenantContextTargetApi.EVENTS).withTenantId("tenant1")
                .build();
        assertFalse(s.isPersistent());
    }

    @Test
    public void testCustomTargets() {
        Subscription s = Subscription.builder().withId("mySub", "mySubscriber")
                .withTenantId("myTenant")
                // first we mess it up...
                .withTenantContextTargetApis(TenantContextTargetApi.ALL)
                .withDeviceContextTargetApis("1", DeviceContextTargetApi.OPERATIONS)
                // then make sure that other ones are overridden
                .withCustomContextTargetApis(SubscriptionContext.TENANT, null, List.of("customApi1", "Api3"))
                .build();

        assertTrue(s.isTenantSubscription());
        assertThat(s.getTargetApis()).containsExactlyInAnyOrder("customApi1", "Api3");
    }
}

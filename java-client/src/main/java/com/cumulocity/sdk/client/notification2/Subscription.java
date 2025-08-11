package com.cumulocity.sdk.client.notification2;

import com.cumulocity.sdk.client.notification2.exception.Notifications2FieldInvalidException;
import com.cumulocity.sdk.client.notification2.exception.Notifications2FieldRequiredException;
import com.cumulocity.sdk.client.util.StringUtils;
import com.google.common.collect.ImmutableSet;
import lombok.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represents a subscription definition. It can be created only using a {@link Builder} object.
 * <br/><br/>
 * We assume that there are 2 types of targets:
 * <ul>
 *     <li>
 *         <b>{@link TenantContextTargetApi}</b> - notifies about all objects of specific type in tenant scope
 *     </li>
 *     <li>
 *         <b>{@link DeviceContextTargetApi}</b> - notifies about all objects of specific type in device scope
 *     </li>
 * </ul>
 * <br/><br/>
 * A valid subscription definition must contain:
 * <ul>
 *     <li><b>subscriptionName</b> - name of the subscription; it must be unique in the scope of given subscriber (see below)</li>
 *     <li><b>subscriber</b> - name of service that uses notifications API.
 *     If the service has many instances, there can be 2 strategies:
 *          <ul>
 *              <li>subscriber name can be shared between instances - this is perfect for SHARED subscriptions, where notifications will be automatically load-balanced between service instances</li>
 *              <li>subscriber name can be unique - perfect for NON-SHARED subscriptions - every instance will be notified about every notification</li>
 *          </ul>
 *     </li>
 *     <li><b>targetApis</b> - at least one device or tenant target API must be provided</li>
 *     <li><b>deviceId</b> - required only when device context is set</li>
 *     <li><b>ackMode</b> - see {@link AckMode} - default value is {@link AckMode#IMMEDIATE}</li>
 *     <li><b>tenantId</b> - tenant ID</li>
 *     <li><b>shared</b> - default false - if subscription is shared between many cluster members (same subscriber and subscription definition)
 *     notifications will be balanced between those members (single notification will be sent to only one member). Not shared subscription will always
 *     receive all notifications. Shared members <b>MUST have the same subscriber name</b></li>
 *     <li><b>typeFilter</b> - optional type of the object (i.e. event type) - notifications should come only for objects of this type.
 *     It can be a single value, or a limited (supporting only or) OData expression i.e. 'c8y_Temperature' or 'c8y_Pressure'</li>
 * </ul>
 */
@ToString
@EqualsAndHashCode(of = {"id"})
public class Subscription {
    @Getter
    private final ID id;
    private final ImmutableSet<TenantContextTargetApi> tenantContextTargetApis;
    private final ImmutableSet<DeviceContextTargetApi> deviceContextTargetApis;
    @Getter
    private final String deviceId;
    @Getter
    private final String typeFilter;
    @Getter
    private final AckMode ackMode;
    @Getter
    private final boolean shared;
    @Getter
    private final String tenantId;
    @Getter
    private final boolean persistent;

    private Subscription(ID id, Set<TenantContextTargetApi> tenantContextTargetApis, Set<DeviceContextTargetApi> deviceContextTargetApis, String deviceId, String typeFilter, AckMode ackMode, boolean shared, boolean persistent, String tenantId) {
        this.id = id;
        if (tenantContextTargetApis != null) {
            this.deviceContextTargetApis = null;
            this.tenantContextTargetApis = ImmutableSet.copyOf(tenantContextTargetApis);
        }
        else {
            this.tenantContextTargetApis = null;
            this.deviceContextTargetApis = ImmutableSet.copyOf(deviceContextTargetApis);
        }
        this.deviceId = deviceId;
        this.typeFilter = typeFilter;
        this.ackMode = ackMode;
        this.shared = shared;
        this.persistent = persistent;
        this.tenantId = tenantId;
    }

    public boolean isTenantSubscription() {
        return tenantContextTargetApis != null;
    }

    public Set<String> getTargetApis() {
        if (isTenantSubscription()) {
            return tenantContextTargetApis.stream().map(TenantContextTargetApi::getTarget).collect(Collectors.toSet());
        }
        return deviceContextTargetApis.stream().map(DeviceContextTargetApi::getTarget).collect(Collectors.toSet());
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class ID {
        private String name;
        private String subscriber;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AckMode ackMode = AckMode.IMMEDIATE;
        private String subscriber;
        private String deviceId;
        private Set<DeviceContextTargetApi> deviceContextTargetApis = null;
        private boolean shared = false;
        private boolean persistent = false;
        private String subscriptionName;
        private Set<TenantContextTargetApi> tenantContextTargetApis = null;
        private String typeFilter;
        private String tenantId;

        private Builder() {
        }

        public static Builder get() {
            return new Builder();
        }

        public Builder withAckMode(AckMode ackMode) {
            if (ackMode == null) {
                throw new Notifications2FieldRequiredException("ackMode");
            }
            this.ackMode = ackMode;
            return this;
        }

        public Builder withDeviceContextTargetApis(String deviceId, DeviceContextTargetApi... targets) {
            if (targets == null || targets.length == 0) {
                throw new Notifications2FieldRequiredException("target");
            }
            if (deviceId == null || StringUtils.isBlank(deviceId)) {
                throw new Notifications2FieldRequiredException("deviceId");
            }
            this.deviceContextTargetApis = new HashSet<>();
            this.deviceContextTargetApis.addAll(Arrays.stream(targets).toList());
            this.deviceId = deviceId;
            this.tenantContextTargetApis = null;
            return this;
        }

        public Builder withTenantContextTargetApis(TenantContextTargetApi... targets) {
            if (targets == null || targets.length == 0) {
                throw new Notifications2FieldRequiredException("target");
            }
            this.deviceContextTargetApis = null;
            this.deviceId = null;
            this.tenantContextTargetApis = new HashSet<>();
            this.tenantContextTargetApis.addAll(Arrays.stream(targets).toList());
            return this;
        }

        public Builder withTypeFilter(String typeFilter) {
            if (StringUtils.isBlank(typeFilter)) {
                this.typeFilter = null;
            } else {
                this.typeFilter = typeFilter;
            }
            return this;
        }

        public Builder withShared(boolean shared) {
            this.shared = shared;
            return this;
        }

        public Builder withPersistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        public Builder withTenantId(String tenantId) {
            if (StringUtils.isBlank(tenantId)) {
                throw new Notifications2FieldRequiredException("tenantId");
            }
            String validTenantId = tenantId.replaceAll("[^a-zA-Z\\d-]", "");
            if (!validTenantId.equals(tenantId)) {
                throw new Notifications2FieldInvalidException("tenantId", tenantId);
            }
            this.tenantId = tenantId;
            return this;
        }

        public Builder withId(String subscriptionName, String subscriber) {
            if (subscriptionName == null || StringUtils.isBlank(subscriptionName)) {
                throw new Notifications2FieldRequiredException("subscriptionName");
            }
            this.subscriptionName = subscriptionName;

            if (subscriber == null || StringUtils.isBlank(subscriber)) {
                throw new Notifications2FieldRequiredException("subscriber");
            }
            this.subscriber = subscriber.replaceAll("[^a-zA-Z\\d]", "");
            if (StringUtils.isBlank(this.subscriber) || !subscriber.equals(this.subscriber)) {
                throw new Notifications2FieldInvalidException("subscriber", subscriber);
            }
            return this;
        }

        public Subscription build() {
            if (deviceContextTargetApis == null && tenantContextTargetApis == null) {
                throw new Notifications2FieldRequiredException("target");
            }
            if (subscriptionName == null) {
                throw new Notifications2FieldRequiredException("subscriptionName");
            }
            if (tenantId == null) {
                throw new Notifications2FieldRequiredException("tenantId");
            }
            return new Subscription(new ID(this.subscriptionName, this.subscriber), this.tenantContextTargetApis, this.deviceContextTargetApis,
                    this.deviceId, this.typeFilter, this.ackMode, this.shared, this.persistent, this.tenantId);
        }
    }
}

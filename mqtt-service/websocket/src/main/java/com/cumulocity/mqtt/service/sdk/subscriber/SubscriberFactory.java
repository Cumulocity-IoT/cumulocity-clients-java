package com.cumulocity.mqtt.service.sdk.subscriber;

@Deprecated(forRemoval = true)
public interface SubscriberFactory {

    Subscriber build(SubscriberConfig config);

}

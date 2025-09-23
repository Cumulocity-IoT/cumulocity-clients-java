package com.cumulocity.mqtt.service.sdk.publisher;

@Deprecated(forRemoval = true)
public interface PublisherFactory {

    Publisher build(PublisherConfig config);

}

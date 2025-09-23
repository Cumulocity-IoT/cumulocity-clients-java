package com.cumulocity.mqtt.service.sdk;

import com.cumulocity.mqtt.service.sdk.publisher.Publisher;
import com.cumulocity.mqtt.service.sdk.publisher.PublisherConfig;
import com.cumulocity.mqtt.service.sdk.publisher.PublisherFactory;
import com.cumulocity.mqtt.service.sdk.subscriber.Subscriber;
import com.cumulocity.mqtt.service.sdk.subscriber.SubscriberConfig;
import com.cumulocity.mqtt.service.sdk.subscriber.SubscriberFactory;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
@Deprecated(forRemoval = true)
public class MqttServiceApiImpl implements MqttServiceApi {

    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();

    private final PublisherFactory publisherFactory;
    private final SubscriberFactory subscriberFactory;

    @Override
    public Publisher buildPublisher(final PublisherConfig config) {
        final Publisher publisher = publisherFactory.build(config);
        publishers.put(config.getId(), publisher);
        return publisher;
    }

    @Override
    public Subscriber buildSubscriber(final SubscriberConfig config) {
        final Subscriber subscriber = subscriberFactory.build(config);
        subscribers.put(config.getId(), subscriber);
        return subscriber;
    }

    @Override
    public Optional<Publisher> getPublisher(final String id) {
        return ofNullable(publishers.get(id));
    }

    @Override
    public Optional<Subscriber> getSubscriber(final String id) {
        return ofNullable(subscribers.get(id));
    }

    @Override
    public void closePublisher(final String id) {
        ofNullable(publishers.remove(id)).ifPresent(Publisher::close);
    }

    @Override
    public void closeSubscriber(final String id) {
        ofNullable(subscribers.remove(id)).ifPresent(Subscriber::close);
    }

    @Override
    public void close() {
        publishers.values().stream()
                .filter(Publisher::isConnected)
                .forEach(Publisher::close);
        publishers.clear();

        subscribers.values().stream()
                .filter(Subscriber::isConnected)
                .forEach(Subscriber::close);
        subscribers.clear();
    }
}

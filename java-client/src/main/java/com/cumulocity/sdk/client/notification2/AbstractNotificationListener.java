package com.cumulocity.sdk.client.notification2;

import lombok.extern.slf4j.Slf4j;

/**
 * Base listener that automatically parses payload to specific type
 *
 * @param <T> expected payload type i.e. {@link com.cumulocity.rest.representation.event.EventRepresentation}
 */
@Slf4j
@SuppressWarnings("unused")
public abstract class AbstractNotificationListener<T> implements NotificationListener {
    private final Class<T> clazz;


    protected AbstractNotificationListener(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void onMessage(Notification message, String subscriptionName, String tenantId, String deviceId) {
        try {
            onMessage(message.parseJson(clazz), message.getAction(), tenantId, deviceId);
        } catch (Exception e) {
            onParsingError(message, subscriptionName, e);
        }
    }

    public abstract void onMessage(T message, Action action, String tenantId, String deviceId);

    /**
     * Called when there's an exception while parsing payload to target object type
     *
     * @param message          original message
     * @param subscriptionName subscription name
     * @param exception        exception thrown during parsing
     */
    public void onParsingError(Notification message, String subscriptionName, Throwable exception) {
        log.error("Error parsing notification payload to " + clazz.getName());
        log.error(subscriptionName + ":" + message, exception);
    }
}

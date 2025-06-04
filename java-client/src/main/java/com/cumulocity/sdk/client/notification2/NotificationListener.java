package com.cumulocity.sdk.client.notification2;

/**
 * Listener provided by message subscribers to {@link Notifications2Api}.
 */
public interface NotificationListener {
    /**
     * Method invoked whenever there's a new notification received. <br/>
     * <b>CAUTION:</b> If this method throws an exception and {@link AckMode} is
     * {@link AckMode#SYNCHRONOUS}, then ACK packet won't be sent to the server which should cause the server to
     * retransmit the message
     *
     * @param message          received {@link Notification} message
     * @param subscriptionName system name of the subscription for debugging purposes
     * @param tenantId         tenant id
     * @param deviceId         device id (optional)
     */
    void onMessage(Notification message, String subscriptionName, String tenantId, String deviceId);
}

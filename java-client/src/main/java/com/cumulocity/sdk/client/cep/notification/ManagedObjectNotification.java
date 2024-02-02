package com.cumulocity.sdk.client.cep.notification;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;

/**
 * @deprecated This class is superseded by {@link
 * com.cumulocity.rest.representation.notification.NotificationRepresentation.ManagedObjectNotificationRepresentation}.
 * Use it based on {@link com.cumulocity.sdk.client.notification.NotificationSubscriberProducer}.
 */
@Deprecated
public class ManagedObjectNotification {
    
    public ManagedObjectNotification() {}
    
    public ManagedObjectNotification(ManagedObjectRepresentation data, String realtimeAction) {
        this.data = data;
        this.realtimeAction = realtimeAction;
    }

    private static final String UPDATE_ACTION = "UPDATE";

    private ManagedObjectRepresentation data;
    private String realtimeAction;
    
    public ManagedObjectRepresentation getData() {
        return data;
    }
    public void setData(ManagedObjectRepresentation data) {
        this.data = data;
    }
    public String getRealtimeAction() {
        return realtimeAction;
    }
    public void setRealtimeAction(String realtimeAction) {
        this.realtimeAction = realtimeAction;
    }
    
    public boolean isUpdateNotification() {
        return UPDATE_ACTION.equals(realtimeAction);
    }
}

package com.cumulocity.sdk.client.cep.notification;

/**
 * CREATE and UPDATE notifications have ManagedObjectRepresentation in "data" field, 
 * however DELETE notification has just String value. 
 * That is why data has to be of type Object to support both.
 *
 * @deprecated This class is superseded by {@link
 * com.cumulocity.rest.representation.notification.NotificationRepresentation.ManagedObjectNotificationRepresentation}.
 * Use it based on {@link com.cumulocity.sdk.client.notification.NotificationSubscriberProducer}.
 */
@Deprecated
public class ManagedObjectDeleteAwareNotification {
    
    public ManagedObjectDeleteAwareNotification() {}
    
    public ManagedObjectDeleteAwareNotification(Object data, String realtimeAction) {
        this.data = data;
        this.realtimeAction = realtimeAction;
    }

    private static final String UPDATE_ACTION = "UPDATE";

    private Object data;
    private String realtimeAction;
    
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
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

package com.cumulocity.rest.representation.notification;

import org.svenson.JSON;
import org.svenson.JSONProperty;
import org.svenson.JSONable;
import org.svenson.converter.JSONConverter;

import com.cumulocity.model.NotificationConverters.*;
import com.cumulocity.model.JSONBase;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.AbstractExtensibleRepresentation;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * This class is the Java representation of objects returned by the real-time
 * notifications API of Cumulocity IoT.
 *
 * For each type of domain model objects (Measurements, Events, etc.) that supports
 * real-time notifications there is a specific subclass of this class available:
 * <ul>
 * <li>{@link AlarmNotificationRepresentation}</li>
 * <li>{@link EventNotificationRepresentation}</li>
 * <li>{@link ManagedObjectNotificationRepresentation}</li>
 * <li>{@link MeasurementNotificationRepresentation}</li>
 * <li>{@link OperationNotificationRepresentation}</li>
 * </ul>
 *
 * Each object returned as real-time notification has two attributes:
 * <dl>
 * <dt>{@code realtimeAction}</dt>
 * <dd>The action can be one of {@link Action#CREATE CREATE}, {@link Action#UPDATE UPDATE},
 *     or {@link Action#DELETE DELETE}.</dd>
 * <dt>{@code data}</dt>
 * <dd>The data contains the actual object which this notification is about.
 *     It is an object from Cumulocity's domain model (e.&thinsp;g. an Alarm).
 *     However, in case of a {@code DELETE} action, the data only contains
 *     the ID of the deleted object. Thus this Java representation exposes
 *     two separate accessors for proper type handling:
 *     {@link #getData()} and {@link #getDeletedDataId()}</dd>
 * </dl>
 *
 * @param <T>  the type of domain model object for which notifications are created
 *     (e.&thinsp;g. {@link AlarmRepresentation})
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationRepresentation<T extends AbstractExtensibleRepresentation>
        implements JSONable {

    /**
     * Enumeration of the possible action types reported in real-time notifications.
     * <ul>
     * <li>{@link #CREATE}</li>
     * <li>{@link #UPDATE}</li>
     * <li>{@link #DELETE}</li>
     * </ul>
     */
    public enum Action {
        /** The {@code CREATE} action. */
        CREATE,
        /** The {@code UPDATE} action. */
        UPDATE,
        /**
         * The {@code DELETE} action is special in the sense that a real-time
         * notification for this action does not contain a data object,
         * but only the ID of the deleted object as
         * {@link NotificationRepresentation#getData() data} attribute.
         */
        DELETE;
    }

    private static final JSON JSON_GENERATOR = JSONBase.getJSONGenerator();

    /**
     * -- SETTER --
     * Setter for the {@code realtimeAction} attribute of this real-time notification object.
     * This method should be rarely needed by users because notification objects
     * are usually created by the SDK (not by users) and consumed by users.
     *
     * @param realtimeAction  the {@link Action} which is reported by this
     *     real-time notification
     *
     * -- GETTER --
     * Getter for the {@code realtimeAction} attribute of this real-time notification object.
     *
     * @return the {@link Action} which is reported by this real-time notification
     */
    @Setter
    @Getter(onMethod_=@JSONProperty)
    @EqualsAndHashCode.Include
    private Action realtimeAction;
    
    private T data; // Usually of type T, but if realtimeAction is DELETE,
                    // the JSON 'data' attribute is just a string holding the ID
                    // of the deleted object. We store the latter as dataId.
    private GId dataId;

    /**
     * Setter for the {@code data} attribute of this real-time notification object.
     * This method should be rarely needed by users because notification objects
     * are usually created by the SDK (not by users) and consumed by users.
     * However, if users want to construct a notification for a {@link Action#DELETE DELETE}
     * action, the method {@link #setDeletedDataId(String)} or {@link #setDeletedDataId(GId)}
     * should be used instead of this method.
     *
     * @param data  the domain model object (e.&thinsp;g. an Alarm)
     *     which this notification is about
     */
    public void setData(T data) {
        this.data = data;
        if (data != null)
            this.dataId = data.get(GId.class); // Users should set this ID with setDeletedDataId()
                // and not via setData(), but this step is needed here for the JSON parsing process.
    }

    /**
     * Getter for the {@code data} attribute of this real-time notification object.
     *
     * @return the domain model object (e.&thinsp;g. an Alarm) transported by
     *     this notification object
     * @throws IllegalStateException  if this method is used on a
     *     {@link Action#DELETE DELETE} notification
     */
    public T getData() { // Note: This method must be overridden in appropriate subclasses
                         // for proper JSON parsing because if we annotated @JSONProperty
                         // here, Svenson's logic would resolve to the base type
                         // AbstractExtensibleRepresentation, not to type T,
                         // due to type erasure.
        if (Action.CREATE.equals(realtimeAction) || Action.UPDATE.equals(realtimeAction))
            return data;
        else
            throw new IllegalStateException("This method must only be used if \"realtimeAction\" is \"CREATE\" or \"UPDATE\".");
    }

    /**
     * Setter for the {@code data} attribute of this real-time notification object
     * in case of a {@link Action#DELETE DELETE} action.
     * This method should be rarely needed by users because notification objects
     * are usually created by the SDK (not by users) and consumed by users.
     *
     * @param id  the ID of the deleted object
     * @throws IllegalStateException  if this method is <i>not</i> used on a
     *     {@link Action#DELETE DELETE} notification
     */
    public void setDeletedDataId(String id) {
        setDeletedDataId(new GId(id));
    }

    /**
     * Setter for the {@code data} attribute of this real-time notification object
     * in case of a {@link Action#DELETE DELETE} action.
     * This method should be rarely needed by users because notification objects
     * are usually created by the SDK (not by users) and consumed by users.
     *
     * @param id  the ID of the deleted object
     * @throws IllegalStateException  if this method is <i>not</i> used on a
     *     {@link Action#DELETE DELETE} notification
     */
    public void setDeletedDataId(GId id) {
        if (Action.DELETE.equals(realtimeAction))
            this.dataId = id;
        else
            throw new IllegalStateException("This method must only be used if \"realtimeAction\" is \"DELETE\".");
    }

    /**
     * Getter for the {@code data} attribute of this real-time notification object
     * in case of a {@link Action#DELETE DELETE} action.
     *
     * @return the ID of the deleted object transported by this notification object
     * @throws IllegalStateException  if this method is <i>not</i> used on a
     *     {@link Action#DELETE DELETE} notification
     */
    @JSONProperty(ignore = true)
    public GId getDeletedDataId() {
        if (Action.DELETE.equals(realtimeAction))
            return dataId;
        else
            throw new IllegalStateException("This method must only be used if \"realtimeAction\" is \"DELETE\".");
    }

    /**
     * Serializes this notification object into a JSON object equivalent to the JSON
     * notification objects generated by Cumulocity's real-time notification API.
     * Depending on the {@linkplain #getRealtimeAction() real-time action}, the
     * {@code data} attribute of the JSON object is populated with different content:
     * <ul>
     * <li>In case of a {@link Action#CREATE CREATE} or {@link Action#UPDATE UPDATE}
     *     action, the {@linkplain #getData() data object} is serialized into the
     *     {@code data} attribute.</li>
     * <li>In case of a {@link Action#DELETE DELETE} action, the
     *     {@linkplain #getDeletedDataId() ID of the deleted object}
     *     is serialized into the {@code data} attribute.</li>
     * </ul>
     */
    @Override
    public String toJSON() {
        return "{\"realtimeAction\":" + JSON_GENERATOR.forValue(realtimeAction)
                + ",\"data\":" + JSON_GENERATOR.forValue(getDataOrId()) + "}";
    }

    /**
     * Converts this notification object to a JSON string as returned by {@link #toJSON()}.
     */
    @Override
    public String toString() {
        return toJSON();
    }

    @EqualsAndHashCode.Include
    private Object getDataOrId() {
        return Action.DELETE.equals(realtimeAction)
                ? dataId // use ID in case of DELETE action
                : data; // use data object in case of all other actions (CREATE and UPDATE)
                        // and in case of realtimeAction == null
    }

    /**
     * This class is the Java representation of real-time notifications for
     * {@linkplain ManagedObjectRepresentation Managed Objects}
     * returned by the real-time notification API.
     *
     * @see NotificationRepresentation
     */
    public static class ManagedObjectNotificationRepresentation
            extends NotificationRepresentation<ManagedObjectRepresentation> {

        @Override
        @JSONProperty
        @JSONConverter(type = DeletedManagedObjectConverter.class)
        public ManagedObjectRepresentation getData() {
            return super.getData();
        }

    }

    /**
     * This class is the Java representation of real-time notifications for
     * {@linkplain MeasurementRepresentation Measurements}
     * returned by the real-time notification API.
     *
     * @see NotificationRepresentation
     */
    public static class MeasurementNotificationRepresentation
            extends NotificationRepresentation<MeasurementRepresentation> {

        @Override
        @JSONProperty
        @JSONConverter(type = DeletedMeasurementConverter.class)
        public MeasurementRepresentation getData() {
            return super.getData();
        }

    }

    /**
     * This class is the Java representation of real-time notifications for
     * {@linkplain EventRepresentation Events}
     * returned by the real-time notification API.
     *
     * @see NotificationRepresentation
     */
    public static class EventNotificationRepresentation
            extends NotificationRepresentation<EventRepresentation> {

        @Override
        @JSONProperty
        @JSONConverter(type = DeletedEventConverter.class)
        public EventRepresentation getData() {
            return super.getData();
        }

    }

    /**
     * This class is the Java representation of real-time notifications for
     * {@linkplain AlarmRepresentation Alarms}
     * returned by the real-time notification API.
     *
     * @see NotificationRepresentation
     */
    public static class AlarmNotificationRepresentation
            extends NotificationRepresentation<AlarmRepresentation> {

        @Override
        @JSONProperty
        @JSONConverter(type = DeletedAlarmConverter.class)
        public AlarmRepresentation getData() {
            return super.getData();
        }

    }

    /**
     * This class is the Java representation of real-time notifications for
     * {@linkplain OperationRepresentation Operations}
     * returned by the real-time notification API.
     *
     * @see NotificationRepresentation
     */
    public static class OperationNotificationRepresentation
            extends NotificationRepresentation<OperationRepresentation> {

        @Override
        @JSONProperty
        @JSONConverter(type = DeletedOperationConverter.class)
        public OperationRepresentation getData() {
            return super.getData();
        }

    }

}

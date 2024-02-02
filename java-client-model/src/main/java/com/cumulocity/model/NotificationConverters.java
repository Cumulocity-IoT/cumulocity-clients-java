package com.cumulocity.model;

import org.svenson.converter.TypeConverter;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;

public class NotificationConverters {

    public static class DeletedManagedObjectConverter implements TypeConverter {

        @Override
        public Object fromJSON(Object o) {
            if (o instanceof String) {
                ManagedObjectRepresentation wrapper = new ManagedObjectRepresentation();
                wrapper.set(new GId((String)o));
                o = wrapper;
            }
            return o;
        }

        @Override
        public Object toJSON(Object o) { // not needed at all
            return o;
        }

    }

    public static class DeletedMeasurementConverter implements TypeConverter {

        @Override
        public Object fromJSON(Object o) {
            if (o instanceof String) {
                MeasurementRepresentation wrapper = new MeasurementRepresentation();
                wrapper.set(new GId((String)o));
                o = wrapper;
            }
            return o;
        }

        @Override
        public Object toJSON(Object o) { // not needed at all
            return o;
        }

    }

    public static class DeletedEventConverter implements TypeConverter {

        @Override
        public Object fromJSON(Object o) {
            if (o instanceof String) {
                EventRepresentation wrapper = new EventRepresentation();
                wrapper.set(new GId((String)o));
                o = wrapper;
            }
            return o;
        }

        @Override
        public Object toJSON(Object o) { // not needed at all
            return o;
        }

    }

    public static class DeletedAlarmConverter implements TypeConverter {

        @Override
        public Object fromJSON(Object o) {
            if (o instanceof String) {
                AlarmRepresentation wrapper = new AlarmRepresentation();
                wrapper.set(new GId((String)o));
                o = wrapper;
            }
            return o;
        }

        @Override
        public Object toJSON(Object o) { // not needed at all
            return o;
        }

    }

    public static class DeletedOperationConverter implements TypeConverter {

        @Override
        public Object fromJSON(Object o) {
            if (o instanceof String) {
                OperationRepresentation wrapper = new OperationRepresentation();
                wrapper.set(new GId((String)o));
                o = wrapper;
            }
            return o;
        }

        @Override
        public Object toJSON(Object o) { // not needed at all
            return o;
        }

    }

}

package com.cumulocity.model;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.svenson.JSONParser;

import com.cumulocity.model.JSONBase;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.rest.representation.notification.NotificationRepresentation;
import com.cumulocity.rest.representation.notification.NotificationRepresentation.*;

public class NotificationConversionTest {

    private static final JSONParser PARSER = JSONBase.getJSONParser();

    public void shallowJsonRoundtripCheck(String json) {
        GeneralNotificationRepresentation parsed =
                PARSER.parse(GeneralNotificationRepresentation.class, json);
        String json2 = parsed.toJSON(); // This one might differ from the input JSON object
                                        // because the input might not be in the canonical
                                        // form (whitespace normalization).

        // check for equality in terms of JSON structure
        assertEquals(PARSER.parse(json), PARSER.parse(json2));
        // Note: We can't compare objects of type GeneralNotificationRepresentation
        // because the underlying AbstractExtensibleRepresentation does not have
        // a semantic "equals" method.
    }

    public <T extends NotificationRepresentation<?>> void semanticJsonRoundtripCheck(String json, Class<T> type) {
        T parsed = PARSER.parse(type, json);
        String json2 = parsed.toJSON(); // This one might differ from the input JSON object
                                        // because the input might not be in the canonical
                                        // form (whitespace, but also date-time strings,
                                        // and maybe other things).
        T parsed2 = PARSER.parse(type, json2);

        // check for equality in terms of target object type
        assertEquals(parsed, parsed2);
    }

    @Test
    public void testOperationNotification() {
        String operationNotification =
                "{ \"realtimeAction\": \"CREATE\", \"data\": { \"id\": \"0815\", "
                + "\"self\": \"https://TENANT.DOMAIN/devicecontrol/operation/0815\", "
                + "\"creationTime\": \"2019-12-17T09:46:45.435+01:00\", \"deviceId\": "
                + "\"42\", \"deviceName\": \"My Device\", \"status\": \"PENDING\", "
                + "\"time\": \"2020-01-01T00:00:00+01:00\", \"c8y_Restart\": {} } }";
        shallowJsonRoundtripCheck(operationNotification);
        semanticJsonRoundtripCheck(operationNotification,
                OperationNotificationRepresentation.class);
    }

    @Test
    public void testDeletedOperationNotification() {
        String deletedOperationNotification =
                "{ \"realtimeAction\": \"DELETE\", \"data\": \"0815\" }";
        shallowJsonRoundtripCheck(deletedOperationNotification);
        semanticJsonRoundtripCheck(deletedOperationNotification,
                OperationNotificationRepresentation.class);
    }

    @Test
    public void testEventNotification() {
        String eventNotification =
                "{ \"realtimeAction\": \"CREATE\", \"data\": { \"id\": \"1234\", "
                + "\"self\": \"https://TENANT.DOMAIN/event/events/1234\", "
                + "\"creationTime\": \"2020-01-01T00:00:01.184+01:00\", "
                + "\"lastUpdated\": \"2020-01-01T00:00:01.184+01:00\", "
                + "\"source\": { \"id\": \"42\", \"self\": "
                + "\"https://TENANT.DOMAIN/inventory/managedObjects/42\" }, "
                + "\"type\": \"CustomEvent\", \"time\": \"2020-01-01T00:00:00+01:00\", "
                + "\"text\": \"This event occurred.\" } }";
        shallowJsonRoundtripCheck(eventNotification);
        semanticJsonRoundtripCheck(eventNotification,
                EventNotificationRepresentation.class);
    }

}

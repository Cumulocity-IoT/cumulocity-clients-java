package com.cumulocity.model;

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

    public <T extends NotificationRepresentation<?>> void jsonRoundtrip(String json, Class<T> type) {
        T parsed = PARSER.parse(type, json);
        String json2 = parsed.toJSON(); // might differ from input JSON object because
                                        // input might not be in the canonical form
        T parsed2 = PARSER.parse(type, json2);
        assertEquals(parsed, parsed2);
    }

    @Test
    public void testOperationNotification() {
        jsonRoundtrip("{ \"realtimeAction\": \"CREATE\", \"data\": { \"id\": \"0815\","
                + "\"self\": \"https://TENANT.DOMAIN/devicecontrol/operation/0815\","
                + "\"creationTime\": \"2019-12-17T09:46:45.435+01:00\", \"deviceId\": \"42\","
                + "\"deviceName\": \"My Device\", \"status\": \"PENDING\", \"time\":"
                + "\"2020-01-01T00:00:00+01:00\", \"c8y_Restart\": {} } }",
                OperationNotificationRepresentation.class);
    }
    
    @Test
    public void testDeletedOperationNotification() {
        jsonRoundtrip("{ \"realtimeAction\": \"DELETE\", \"data\": \"0815\" }",
                OperationNotificationRepresentation.class);
    }

    @Test
    public void testEventNotificationToJson() {
        jsonRoundtrip("{ \"realtimeAction\": \"CREATE\", \"data\": { \"id\": \"1234\","
                + "\"self\": \"https://TENANT.DOMAIN/event/events/1234\", \"creationTime\":"
                + "\"2020-01-01T00:00:01.184+01:00\", \"lastUpdated\":"
                + "\"2020-01-01T00:00:01.184+01:00\", \"source\": { \"id\": \"42\","
                + "\"self\": \"https://TENANT.DOMAIN/inventory/managedObjects/42\" },"
                + "\"type\": \"CustomEvent\", \"time\": \"2020-01-01T00:00:00+01:00\","
                + "\"text\": \"This even occurred.\" } }",
                EventNotificationRepresentation.class);
    }

}

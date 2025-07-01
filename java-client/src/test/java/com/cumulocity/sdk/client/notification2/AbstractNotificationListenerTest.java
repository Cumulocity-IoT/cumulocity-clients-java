package com.cumulocity.sdk.client.notification2;

import c8y.Command;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.operation.OperationStatus;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractNotificationListenerTest {

    private Gson gson() {
        return Converters.registerDateTime(new GsonBuilder()).create();
    }

    @Test
    public void testObjectParsing() {
        OperationRepresentation source = new OperationRepresentation();
        source.setDeviceId(GId.asGId("dev1"));
        source.setCreationDateTime(DateTime.now());
        source.setStatus(OperationStatus.PENDING.name());
        Command cmd = new Command("my command");
        source.set(cmd);
        String sourceAsJson = gson().toJson(source);
        String notificationString = "HEADER\n" + sourceAsJson;
        Notification notification = Notification.parse(notificationString);

        final AtomicReference<OperationRepresentation> target = new AtomicReference<>();
        AbstractNotificationListener<OperationRepresentation> listener = new AbstractNotificationListener<>(OperationRepresentation.class) {

            @Override
            public void onMessage(OperationRepresentation message, Action action, String tenantId, String deviceId) {
                target.set(message);
            }
        };

        listener.onMessage(notification, "test", "tenant1", "device1");
        assertNotNull(target.get());
        assertEquals(source.getCreationDateTime().toLocalDateTime(), target.get().getCreationDateTime().toLocalDateTime());
        assertEquals(source.getDeviceId(), target.get().getDeviceId());
        assertEquals(source.getStatus(), target.get().getStatus());
        Command command = target.get().get(Command.class);
        assertNotNull(command);
        assertEquals(cmd.getText(), command.getText());
    }


    @Test
    public void testParseError() {
        final AtomicReference<OperationRepresentation> target = new AtomicReference<>();
        final AtomicBoolean parsingError = new AtomicBoolean(false);
        AbstractNotificationListener<OperationRepresentation> listener = new AbstractNotificationListener<>(OperationRepresentation.class) {
            @Override
            public void onMessage(OperationRepresentation message, Action action, String tenantId, String deviceId) {
                target.set(message);
            }

            @Override
            public void onParsingError(Notification message, String subscriptionName, Throwable exception) {
                super.onParsingError(message, subscriptionName, exception);
                parsingError.set(true);
            }
        };

        String notificationString = "HEADER\n{someinvalidgarbage...";
        Notification notification = Notification.parse(notificationString);
        listener.onMessage(notification, "test", "tenant1", "device1");

        assertNull(target.get());
        assertTrue(parsingError.get());
    }
}

package com.cumulocity.sdk.client.notification2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class NotificationTest {
    @Test
    public void testSingleLine(){
        Notification n = Notification.parse("payload");
        assertNotNull(n);
        assertNull(n.getAckHeader());
        assertTrue(n.getHeaders().isEmpty());
        assertEquals("payload", n.getPayload());
    }

    @Test
    public void testTwoLines() {
        String s = """
                ACK
                payload
                """;
        Notification n = Notification.parse(s);
        assertNotNull(n);
        assertEquals("ACK", n.getAckHeader());
        assertTrue(n.getHeaders().isEmpty());
        assertEquals("payload", n.getPayload());
    }

    @Test
    public void testMultipleLines() {
        String s = """
                ACK
                HEAD1
                HEAD2
                HEAD3
                payload
                """;
        Notification n = Notification.parse(s);
        assertNotNull(n);
        assertEquals("ACK", n.getAckHeader());
        assertEquals(3, n.getHeaders().size());
        assertEquals("payload", n.getPayload());
        assertEquals("HEAD1", n.getHeaders().get(0));
        assertEquals("HEAD2", n.getHeaders().get(1));
        assertEquals("HEAD3", n.getHeaders().get(2));
    }
}

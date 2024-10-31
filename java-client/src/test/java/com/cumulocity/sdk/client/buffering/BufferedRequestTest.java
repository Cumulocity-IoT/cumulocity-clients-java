package com.cumulocity.sdk.client.buffering;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.HttpMethod;

import org.junit.jupiter.api.Test;

import com.cumulocity.rest.representation.alarm.AlarmMediaType;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.builder.AlarmRepresentationBuilder;
import com.cumulocity.rest.representation.builder.EventRepresentationBuilder;
import com.cumulocity.rest.representation.event.EventMediaType;
import com.cumulocity.rest.representation.event.EventRepresentation;

public class BufferedRequestTest {

    @Test
    public void shouldParseEventRepresentationRequest() {
        String csvString = BufferedRequest.create(HttpMethod.POST, "http://test.cumulocity.com/event/events", EventMediaType.EVENT, eventRepresentation())
                .toCsvString();
        
        assertThat(csvString).isEqualTo("POST##http://test.cumulocity.com/event/events##E##E##{\"text\":\"text##\"}");
    }
    
    @Test
    public void shouldParseAlarmRepresentationRequest() {
        String csvString = BufferedRequest.create(HttpMethod.POST, "http://test.cumulocity.com/alarm/alarms", AlarmMediaType.ALARM, alarmRepresentation())
                .toCsvString();
        
        assertThat(csvString).isEqualTo("POST##http://test.cumulocity.com/alarm/alarms##A##A##{\"text\":\"text\"}");
    }
    
    @Test
    public void shouldParseCsvStringToAlarmRepresentation() {
        String csvString = "POST##http://test.cumulocity.com/alarm/alarms##A##A##{\"text\":\"text\"}";
        
        BufferedRequest request = BufferedRequest.parseCsvString(csvString);
        
        assertThat(request.getPath()).isEqualTo("http://test.cumulocity.com/alarm/alarms");
        assertThat(request.getMediaType()).isEqualTo(AlarmMediaType.ALARM);
        assertThat(request.getRepresentation().getClass()).isEqualTo(AlarmRepresentation.class);
        assertThat(((AlarmRepresentation)request.getRepresentation()).getText()).isEqualTo("text");
    }
    
    @Test
    public void shouldParseCsvStringToEventRepresentation() {
        String csvString = "POST##http://test.cumulocity.com/event/events##E##E##{\"text\":\"text##\"}";
        
        BufferedRequest request = BufferedRequest.parseCsvString(csvString);
        
        assertThat(request.getPath()).isEqualTo("http://test.cumulocity.com/event/events");
        assertThat(request.getMediaType()).isEqualTo(EventMediaType.EVENT);
        assertThat(request.getRepresentation().getClass()).isEqualTo(EventRepresentation.class);
        assertThat(((EventRepresentation)request.getRepresentation()).getText()).isEqualTo("text##");
    }

    private EventRepresentation eventRepresentation() {
        return new EventRepresentationBuilder()
            .withText("text##")
            .build();
    }
    
    private AlarmRepresentation alarmRepresentation() {
        return new AlarmRepresentationBuilder()
            .withText("text")
            .withHistory(null)
            .build();
    }
}

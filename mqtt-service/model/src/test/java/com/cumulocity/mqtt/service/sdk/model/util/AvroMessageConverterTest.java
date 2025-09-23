package com.cumulocity.mqtt.service.sdk.model.util;

import com.cumulocity.mqtt.service.sdk.model.MqttServiceMessage;
import com.cumulocity.mqtt.service.sdk.model.MqttServiceMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@Deprecated(forRemoval = true)
class AvroMessageConverterTest {

    private final MessageConverter converter = new AvroMessageConverter();

    @Test
    void shouldEncodeAndDecodeCorrectly() {
        // Given
        final MqttServiceMetadata metadata = MqttServiceMetadata.builder()
                .clientId("test")
                .messageId(100)
                .topic("topic")
                .build();
        final MqttServiceMessage message = new MqttServiceMessage("message".getBytes(), metadata);

        // When
        final byte[] encoded = converter.encode(message);
        final MqttServiceMessage decodedMessage = converter.decode(encoded);

        // Then
        assertThat(new String(decodedMessage.getPayload())).isEqualTo(new String(message.getPayload()));
        assertThat(decodedMessage.getMetadata().getClientId()).isEqualTo(message.getMetadata().getClientId());
        assertThat(decodedMessage.getMetadata().getMessageId()).isEqualTo(message.getMetadata().getMessageId());
    }
}

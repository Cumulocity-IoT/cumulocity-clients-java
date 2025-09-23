package com.cumulocity.mqtt.service.sdk.model.util;

import com.cumulocity.mqtt.service.sdk.model.MqttServiceMessage;
import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.apache.commons.compress.utils.IOUtils.closeQuietly;

@Deprecated(forRemoval = true)
public class AvroMessageConverter implements MessageConverter {

    private static final Schema SCHEMA = ReflectData.get().getSchema(MqttServiceMessage.class);

    @Override
    public byte[] encode(MqttServiceMessage message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            DatumWriter<MqttServiceMessage> writer = new ReflectDatumWriter<>(SCHEMA);
            writer.write(message, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error encoding message", e);
        } finally {
            closeQuietly(out);
        }
    }

    @Override
    public MqttServiceMessage decode(byte[] bytes) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
            DatumReader<MqttServiceMessage> reader = new ReflectDatumReader<>(SCHEMA);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Error decoding message", e);
        } finally {
            closeQuietly(in);
        }
    }

}

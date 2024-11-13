/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customdecoders.api.util;

import java.nio.ByteBuffer;

public class ValueTranslator {
    /**
     * Translates the given input byte array based on the provided options
     * @param raw the input byte array
     * @param valueType Enum REGISTER_DATA_TYPE with Integer or Float
     * @param signed signed or unsigned mode
     * @param reverseBitEndianness flag for endian mode (Big/Little Endian)
     * @param order Bits, Bytes, Byte Word or Words
     * @param multiplier multiplier
     * @param divisor divisor
     * @param rightShift - rightShift
     * @return double the translated output
     */
    public static double translate(byte[] raw, REGISTER_DATA_TYPE valueType, boolean signed, boolean reverseBitEndianness, BIT_BYTE_WORD_ORDER order, int multiplier, int divisor, int rightShift) {
        byte[] reordered = DecoderUtils.reorder(DecoderUtils.reverseBitEndianness(raw, reverseBitEndianness), order);
        double value = 0.0;
        if(valueType == REGISTER_DATA_TYPE.Integer) {
            value = DecoderUtils.parseAsLong(ByteBuffer.wrap(reordered), signed);
        } else if(valueType == REGISTER_DATA_TYPE.Float) {
            value = DecoderUtils.parseAsFloatingPoint(ByteBuffer.wrap(reordered));
        }
        if(multiplier != 0) {
            value *= multiplier;
        }
        if(divisor != 0) {
            value /= divisor;
        }
        if(rightShift != 0) {
            double shiftMultiplier = 1.0d;
            if(rightShift>0) {
                for(int i=0;i<rightShift;i++) {
                    shiftMultiplier *= 0.1d;
                }
            } else {
                for(int i=0;i>rightShift;i--) {
                    shiftMultiplier *= 10.0d;
                }
            }
            value *= shiftMultiplier;
        }
        return value;
    }
}

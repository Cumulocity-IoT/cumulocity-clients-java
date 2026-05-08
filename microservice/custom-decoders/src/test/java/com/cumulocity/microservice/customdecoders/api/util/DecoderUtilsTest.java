package com.cumulocity.microservice.customdecoders.api.util;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecoderUtilsTest {

    @Test
    void toHexString() {
        String input1 = "a";
        byte[] inputByteArray = input1.getBytes(StandardCharsets.UTF_8);
        assertEquals("61", DecoderUtils.toHexString(inputByteArray));

        String input2 = "Geek";
        byte[] inputByteArray2 = input2.getBytes(StandardCharsets.UTF_8);
        assertEquals("4765656b", DecoderUtils.toHexString(inputByteArray2));

        byte[] inputByteArray3 = new byte[] {47, 74, 51};
        assertEquals("2f4a33", DecoderUtils.toHexString(inputByteArray3));
    }

    @Test
    void hexStringToByteArray() {
        String hexString = "2f4a33";
        byte[] resultantByteArray = new byte[] {47, 74, 51};
        Assertions.assertArrayEquals(resultantByteArray, DecoderUtils.hexStringToByteArray(hexString));

        String hexString2 = "3b4a11";
        byte[] resultantByteArray2 = new byte[] {59, 74, 17};
        Assertions.assertArrayEquals(resultantByteArray2, DecoderUtils.hexStringToByteArray(hexString2));
    }

    @Test
    void toDateTime() {
        long timestamp = 1640756573;
        DateTime dateTime = DecoderUtils.toDateTime(timestamp);
        assertEquals(2021, dateTime.getYear());
        assertEquals(12, dateTime.getMonthOfYear());
        assertEquals(29, dateTime.getDayOfMonth());
        assertEquals(5, dateTime.getHourOfDay());
        assertEquals(42, dateTime.getMinuteOfHour());
        assertEquals(53, dateTime.getSecondOfMinute());
        assertEquals("UTC", dateTime.getZone().toString());
    }

    @Test
    void convertHexToBinary() {
        String hexInput1 = "2f4a33";
        assertEquals("001011110100101000110011", DecoderUtils.convertHexToBinary(hexInput1));
        String hexInput2 = "3B4A11";
        assertEquals("001110110100101000010001", DecoderUtils.convertHexToBinary(hexInput2));
    }

    @Test
    void toFloatFromHexString() {
        String hexInput1 = "3f800000";
        Float floatFromHex = DecoderUtils.toFloatFromHexString(hexInput1);
        assertEquals(1.0, floatFromHex.floatValue());

        String hexInput2 = "41700001";
        Float floatFromHex2 = DecoderUtils.toFloatFromHexString(hexInput2);
        assertEquals(15.0, floatFromHex2.floatValue());
    }

    @Test
    void reverseBitEndianness() {
        byte[] inputByteArray = new byte[] {47, 74, 51};
        byte[] reversedByteArray = DecoderUtils.reverseBitEndianness(inputByteArray, true);
        assertEquals(-12, reversedByteArray[0]);
        assertEquals(82, reversedByteArray[1]);
        assertEquals(-52, reversedByteArray[2]);
    }

    @Test
    void extractBits() {
        assertEquals(21, DecoderUtils.extractBits(171, 5, 2));
        assertEquals(2, DecoderUtils.extractBits(20, 3, 2));
    }

    @Test
    void toFloat() {
        short hbits = 10;
        assertEquals(5.960464477539062E-7, DecoderUtils.toFloat(hbits));
        short hbits2 = 200;
        assertEquals(1.1920928955078125E-5, DecoderUtils.toFloat(hbits2));
    }

    @Test
    void limitTo16Bits() {
        assertEquals(65535, DecoderUtils.limitTo16Bits(78987));
        assertEquals(4567, DecoderUtils.limitTo16Bits(4567));
        assertEquals(0, DecoderUtils.limitTo16Bits(-876));
    }

    @Test
    void reverse() {
        assertEquals(40, DecoderUtils.reverse((byte) 20));
        assertEquals(-48, DecoderUtils.reverse((byte) 11));
    }

    @Test
    void parseAsFloatingPoint() {
        byte[] inputArray1 = new byte[] {82, -12, -20, -100};
        assertEquals(5.25970833408E11, DecoderUtils.parseAsFloatingPoint(ByteBuffer.wrap(inputArray1)));
        byte[] inputArray2 = new byte[] {10, 20, 30, 40};
        assertEquals(7.13161255447549E-33, DecoderUtils.parseAsFloatingPoint(ByteBuffer.wrap(inputArray2)));
    }

    @Test
    void parseAsLong() {
        byte[] inputArray1 = new byte[] {82, -12, -20, -100};
        assertEquals(1.391783068E9, DecoderUtils.parseAsLong(ByteBuffer.wrap(inputArray1), true));
        byte[] inputArray2 = new byte[] {82, -12, -20, -100};
        assertEquals(1.391783068E9, DecoderUtils.parseAsLong(ByteBuffer.wrap(inputArray2), false));
    }

    @Test
    void reorder() {
        byte[] givenArray = new byte[] {47, 74, 57, 55};
        byte[] reorderedArray = new byte[] {-20, -100, 82, -12};
        Assertions.assertArrayEquals(reorderedArray, DecoderUtils.reorder(givenArray, BIT_BYTE_WORD_ORDER.Reverse_Bits));
        reorderedArray = new byte[] {74, 47, 55, 57};
        Assertions.assertArrayEquals(reorderedArray, DecoderUtils.reorder(givenArray, BIT_BYTE_WORD_ORDER.Reverse_Bytes));
        givenArray = new byte[] {47, 74, 57, 55};
        reorderedArray = new byte[] {55, 57, 74, 47};
        Assertions.assertArrayEquals(reorderedArray, DecoderUtils.reorder(givenArray, BIT_BYTE_WORD_ORDER.Reverse_Byte_Words));
        givenArray = new byte[] {47, 74, 57, 55};
        reorderedArray = new byte[] {57, 55, 47, 74};
        Assertions.assertArrayEquals(reorderedArray, DecoderUtils.reorder(givenArray, BIT_BYTE_WORD_ORDER.Reverse_Words));
        givenArray = new byte[] {47, 74, 57, 55};
        Assertions.assertArrayEquals(givenArray, DecoderUtils.reorder(givenArray, BIT_BYTE_WORD_ORDER.Unchanged));
    }
}

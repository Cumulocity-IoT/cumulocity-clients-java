/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */
package com.cumulocity.microservice.customdecoders.api.util;

import com.cumulocity.microservice.customdecoders.api.exception.DecoderServiceException;
import org.joda.time.DateTime;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Formatter;

public final class DecoderUtils {

    /**
     * Converts the given byte array to a hexadecimal string
     * @param bytes the input byte array
     * @return String the resultant hexadecimal string
     */
    public static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    /**
     * Converts a hexadecimal string to a byte array
     * @param str the input hexadecimal string
     * @return byte[] the resultant byte array
     */
    public static byte[] hexStringToByteArray(String str) {
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Converts epoch UNIX timestamp to DateTime
     * @param timestamp the input epoch timestamp
     * @return DateTime
     */
    public static DateTime toDateTime(long timestamp) {
        Date date = new Date(timestamp * 1000);
        return new DateTime(date);
    }

    /**
     * Converts hexadecimal string to binary
     * @param hex the input hexadecimal string
     * @return String the resultant binary string
     */
    public static String convertHexToBinary(String hex) {
        Integer binaryLength = hex.length() * 4;
        String binaryString = new BigInteger(hex, 16).toString(2);
        Integer paddingLength = binaryLength - binaryString.length();
        StringBuilder stringBuilder = new StringBuilder(binaryString);
        for (int i = paddingLength; i > 0; --i) {
            stringBuilder.insert(0, "0");
        }
        return stringBuilder.toString();
    }

    /**
     * Converts hexadecimal string to Float
     * @param input the input hexadecimal string
     * @return Float
     */
    public static Float toFloatFromHexString(String input) {
        Long i = Long.parseLong(input, 16);
        Float f = Float.intBitsToFloat(i.intValue());
        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        return Float.valueOf(decimalFormat.format(f));
    }

    /**
     * Converts byte[] from Big Endian to Little Endian
     * @param raw input byte array
     * @param reverseBitEndianness flag for endian mode (Big/Little Endian)
     * @return byte[]
     */
    public static byte[] reverseBitEndianness(byte[] raw, boolean reverseBitEndianness) {
        if(reverseBitEndianness) {
            byte[] reversed = new byte[raw.length];
            for (int i = 0; i < raw.length; i++) {
                reversed[i] = reverse(raw[i]);
            }
            return reversed;
        }
        return raw;
    }


    /**
     * Reverse the given input byte value
     * @param value the input byte value
     * @return byte
     */
    public static byte reverse(byte value) {
        byte y = 0;
        for (int position = 7; position >= 0; position--) {
            y += ((value & 1) << position);
            value >>= 1;
        }
        return y;
    }

    /**
     * Converts short to float
     * @param hbits input short number
     * @return float
     */
    public static float toFloat(short hbits) {
        int mant = hbits & 0x03ff;
        int exp =  hbits & 0x7c00;
        if( exp == 0x7c00 ) {
            exp = 0x3fc00;
        } else if( exp != 0 ){
            exp += 0x1c000;
            if( mant == 0 && exp > 0x1c400 ) {
                return Float.intBitsToFloat( ( hbits & 0x8000 ) << 16 | exp << 13 | 0x3ff );
            }
        } else if( mant != 0 ) {
            exp = 0x1c400;
            do {
                mant <<= 1;
                exp -= 0x400;
            } while( ( mant & 0x400 ) == 0 );
            mant &= 0x3ff;
        }
        return Float.intBitsToFloat(( hbits & 0x8000 ) << 16 | ( exp | mant ) << 13 );
    }

    /**
     * Extract a particular number of bits from a particular position in a number
     * @param number the input number
     * @param numOfBits the number of bits to be extracted
     * @param fromIndex the position from where the bits are to be extracted
     * @return byte
     */
    public static byte extractBits(int number, int numOfBits, int fromIndex) {
        return (byte)(((1 << numOfBits) - 1) & (number >> (fromIndex - 1)));
    }

    /**
     * Limits the given integer to 16 bits
     * @param inputValue the input integer
     * @return int
     */
    public static int limitTo16Bits(int inputValue) {
        if(inputValue > 65535) {
            return 65535;
        } else if(inputValue < 0) {
            return 0;
        }
        return inputValue;
    }

    private static String reorderfromLSBtoMSB(String input) {
        String[] subIndividualPayloadEncodedEnergyIndexIdentifier = input.split("(?<=\\G.{2})");
        String reArranged = subIndividualPayloadEncodedEnergyIndexIdentifier[3] + subIndividualPayloadEncodedEnergyIndexIdentifier[2] + subIndividualPayloadEncodedEnergyIndexIdentifier[1] + subIndividualPayloadEncodedEnergyIndexIdentifier[0];
        return reArranged;
    }

    private static byte[] reorderReverseBits(byte[] rawInputData) {
        if(rawInputData.length == 1 || rawInputData.length == 2 || rawInputData.length == 4 || rawInputData.length == 8) {
            byte[] reorderedArray = new byte[rawInputData.length];
            int index = 0;
            for(int currentIndex = rawInputData.length -1; currentIndex >= 0; currentIndex--) {
                reorderedArray[index++] = DecoderUtils.reverse(rawInputData[currentIndex]);
            }
            return reorderedArray;
        }
        return rawInputData;
    }

    private static void swap(byte[] inputArray, int index1, int index2) {
        byte temp = inputArray[index1];
        inputArray[index1] = inputArray[index2];
        inputArray[index2] = temp;
    }

    private static byte[] reorderReverseBytes(byte[] rawInputData) {
        if(rawInputData.length == 2 || rawInputData.length == 4 || rawInputData.length == 8) {
            for(int currentIndex = 0; currentIndex < rawInputData.length; currentIndex +=2) {
                swap(rawInputData, currentIndex, currentIndex + 1);
            }
        }
        return rawInputData;
    }

    private static byte[] reorderReverseByteWords(byte[] rawInputData) {
        if(rawInputData.length == 2 || rawInputData.length == 4 || rawInputData.length == 8) {
            for(int currentIndex = 0; currentIndex < rawInputData.length; currentIndex += 4) {
                int left = currentIndex;
                int right = Math.min(currentIndex + 3, rawInputData.length - 1);
                while(left < right) {
                    swap(rawInputData, left, right);
                    left++;
                    right--;
                }
            }
        }
        return rawInputData;
    }

    private static byte[] reorderReverseWords(byte[] rawInputData) {
        if(rawInputData.length == 4 || rawInputData.length == 8) {
            int index = 0;
            while(index < rawInputData.length) {
                for(int currentIndex = index; currentIndex < index + 2; currentIndex++) {
                    swap(rawInputData, currentIndex, currentIndex+2);
                }
                index += 4;
            }
        }
        return rawInputData;
    }

    /**
     * Parses the ByteBuffer as floating point and converts it to double
     * @param buffer the input ByteBuffer
     * @return double
     */
    public static double parseAsFloatingPoint(ByteBuffer buffer) {
        switch(buffer.remaining()) {
            case 8:
                return buffer.getDouble();
            case 4:
                return buffer.getFloat();
            case 2:
                return DecoderUtils.toFloat(buffer.getShort());
        }
        return 0;
    }

    /**
     * Parses the ByteBuffer as long and converts it to long
     * @param buffer the input ByteBuffer
     * @param signed flag for signed/unsigned mode
     * @return long
     */
    public static long parseAsLong(ByteBuffer buffer, boolean signed) {
        switch(buffer.remaining()) {
            case 8:
                return buffer.getLong();
            case 4:
                if(signed) {
                    return buffer.getInt();
                } else {
                    return Integer.toUnsignedLong(buffer.getInt());
                }
            case 2:
                if(signed) {
                    return buffer.getShort();
                } else {
                    return Short.toUnsignedLong(buffer.getShort());
                }
            case 1:
                if(signed) {
                    return buffer.get();
                }
                else {
                    return Byte.toUnsignedLong(buffer.get());
                }
        }
        return 0;
    }

    /**
     * Reorder the input byte array based on Bits, Bytes, Byte Word or Words
     * @param raw input byte array
     * @param order the parameter based on which the byte array needs to be ordered
     * @return byte[] the reordered byte array
     */
    public static byte[] reorder(byte[] raw, BIT_BYTE_WORD_ORDER order) {
        switch (order) {
            case Reverse_Bits:
                return reorderReverseBits(raw);
            case Reverse_Bytes:
                return reorderReverseBytes(raw);
            case Reverse_Byte_Words:
                return reorderReverseByteWords(raw);
            case Reverse_Words:
                return reorderReverseWords(raw);
            case Unchanged:
            default:
                return raw;
        }
    }
}

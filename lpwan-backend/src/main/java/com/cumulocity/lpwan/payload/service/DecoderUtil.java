/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.payload.service;

import java.math.BigInteger;

import com.cumulocity.lpwan.payload.exception.PayloadDecodingFailedException;

public class DecoderUtil {

    public static final int BIGINTEGER_MAX_SIZE = 32;

    public static int extractDecimalFromHex(String hex, int startBit, int numberOfBits) throws PayloadDecodingFailedException {
        String binary = convertHexToBinary(hex);
        if (binary.length() < startBit + numberOfBits) {
            throw new PayloadDecodingFailedException("Length of payload is not sufficient");
        }
        String payloadPart = binary.substring(startBit, startBit + numberOfBits);
        if(payloadPart.length() > BIGINTEGER_MAX_SIZE) {
            throw new PayloadDecodingFailedException(String.format("Length of payload part should not exceed %s bits.", BIGINTEGER_MAX_SIZE));
        }
        return new BigInteger(payloadPart, 2).intValue();
    }
    
    public static int extractBCDFromHex(String hex, int startBit, int numberOfBits) throws PayloadDecodingFailedException { //applies to bytes
        if (!(startBit % 4 == 0 && numberOfBits % 4 == 0)) {
            throw new PayloadDecodingFailedException("BCD encoding is not successful. StartBit and noBits should be modulo 4");
        }
        
        int startByte = startBit / 4;
        int numberOfBytes = numberOfBits / 4;
        try {
            if (hex.length() < startByte + numberOfBytes) {
                throw new PayloadDecodingFailedException("Length of payload is not sufficient");
            }
            String value = hex.substring(startByte, startByte + numberOfBytes);
            return new BigInteger(value).intValue();
        } catch (NumberFormatException e) {
            throw new PayloadDecodingFailedException("Illegal BCD value at position byte" + startByte);
        }
    }

    public static int extractSignedDecimalFromHex(String hex, int startBit, int numberOfBits) throws PayloadDecodingFailedException {
        String binary = convertHexToBinary(hex);

        if (binary.length() < startBit + numberOfBits) {
            throw new PayloadDecodingFailedException("Length of payload is not sufficient");
        }

        String payloadPart = binary.substring(startBit + 1, startBit + numberOfBits);
        String sign = binary.substring(startBit, startBit + 1);

        // if payloadPart is to big to be contained in int BigInteger.intValue() return the low order 32 bits
        if(payloadPart.length() > BIGINTEGER_MAX_SIZE) {
            throw new PayloadDecodingFailedException(String.format("Length of payload part should not exceed %s bits.", BIGINTEGER_MAX_SIZE));
        }

        if (sign.equals("1")) {
            BigInteger compVal = new BigInteger(sign.concat(payloadPart), 2);
            for (int i = 0; i < numberOfBits; i++) {
                compVal = compVal.flipBit(i);
            }
            return -(compVal.intValue() + 1);
        } else {
            return new BigInteger(payloadPart, 2).intValue();
        }
    }
    
    private static String convertHexToBinary(String hex) throws PayloadDecodingFailedException {
        if(hex == null) {
            throw new PayloadDecodingFailedException("Payload is undefined.");
        }

        Integer binaryLength = hex.length() * 4;
        String binaryString = new BigInteger(hex, 16).toString(2);
        Integer paddingLength = binaryLength - binaryString.length();
        StringBuilder stringBuilder = new StringBuilder(binaryString);
        for (int i = paddingLength; i > 0; --i) { 
           stringBuilder.insert(0, "0");
        }
        return stringBuilder.toString();
    }

    public static Double multiply(Double value, Double multiplication) {
        return value * multiplication;
    }

    public static Double offset(Double value, Double offset) {
        return value + offset; 
    }

    public static int extractSignedBCDFromHex(String hex, Integer startBit, Integer numberOfBits) throws PayloadDecodingFailedException {
        int bcdValue = extractBCDFromHex(hex, startBit, numberOfBits);
        int bitLength = numberOfBits - startBit;
        int bcdMaxCount = (int) Math.pow(10, (bitLength / 4));
        if (bcdValue < bcdMaxCount/2) {
            return bcdValue;
        } else {
            return bcdValue - bcdMaxCount;
        }
    }
    
    public static String convertHexToBigEndianOrdering(String hex, int startBit, int numberOfBits) throws PayloadDecodingFailedException {
        if (!(startBit % 4 == 0 && numberOfBits % 4 == 0)) {
            throw new PayloadDecodingFailedException("Conversion to little endian to big endian failed. StartBit and noBits should be modulo 4");
        }
        int startHexDigitAt = startBit/4;
        int numberOfHexCDigitCount = numberOfBits/4;
        if (hex.length() < startHexDigitAt + numberOfHexCDigitCount) {
            throw new PayloadDecodingFailedException("Length of payload is not sufficient");
        }
        hex = hex.substring(startHexDigitAt, startHexDigitAt + numberOfHexCDigitCount);
        StringBuilder bigEndianSBuilder = new StringBuilder();
        for (int i = numberOfHexCDigitCount; i >= 2; i -= 2 ) {
            String hexDigit = hex.substring(i - 2, i);
            bigEndianSBuilder.append(hexDigit);
        }
        return bigEndianSBuilder.toString();
    }

}

/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.tenant.option;

import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;

@Component
public class EncryptionService {
    public static final String CIPHER = "{cipher}";

    private static final Charset CHARSET = Charsets.UTF_8;
    private static final StringKeyGenerator KEY_GENERATOR = KeyGenerators.string();

    public String decryptString(String encrypted, String password) throws DecryptFailedException {
        if (encrypted == null) {
            throw new DecryptFailedException("Cannot decrypt empty string");
        }
        if (!isEncrypted(encrypted)) {
            throw new DecryptFailedException("Cannot decrypt unencrypted string");
        }
        String withoutPrefix = withoutPrefix(encrypted);
        String salt = extractSalt(withoutPrefix);
        String key = extractKey(withoutPrefix);
        try {
            return Encryptors.text(password, salt).decrypt(key);
        } catch (IllegalStateException e) {
            throw new DecryptFailedException("Decrypting of string failed", e);
        }
    }

    public String encryptString(String plain, String password) {
        String salt = KEY_GENERATOR.generateKey();
        String hash = Encryptors.text(password, salt).encrypt(plain);
        return CIPHER + mergeSalt(hash, salt);
    }

    public boolean isEncrypted(String encrypted) {
        if (encrypted.startsWith(CIPHER)) {
            return true;
        }
        return false;
    }

    private String mergeSalt(String encrypted, String salt) {
        String merged = encrypted + salt;
        return new String(Base64.encode(merged.getBytes(CHARSET)), CHARSET);
    }

    private String extractSalt(String encrypted) {
        String decoded = new String(Base64.decode(encrypted.getBytes(CHARSET)));
        return StringUtils.right(decoded, 16);
    }

    private String extractKey(String encrypted) {
        String decoded = new String(Base64.decode(encrypted.getBytes(CHARSET)));
        return decoded.substring(0, decoded.length() - 16);
    }

    private String withoutPrefix(String prefixedString) {
        return prefixedString.substring(CIPHER.length());
    }
}

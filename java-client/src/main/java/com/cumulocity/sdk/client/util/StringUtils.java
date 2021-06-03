package com.cumulocity.sdk.client.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}

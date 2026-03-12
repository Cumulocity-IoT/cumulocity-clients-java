package com.cumulocity.sdk.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SvensonConfig {
    /**
     * This config option enables stripping of control characters from JSON strings during deserialization.
     * The current implementation filters out characters in the character class 'javaIdentifierIgnorable'.
     * The default value is 'false' to maintain backward compatibility.
     */
    private boolean stripControlCharacters = false;

}

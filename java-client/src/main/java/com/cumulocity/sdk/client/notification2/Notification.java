package com.cumulocity.sdk.client.notification2;

import com.cumulocity.model.JSONBase;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.EnumUtils;

import java.util.Collections;
import java.util.List;

/**
 * Represents data packet received from Notifications 2.0 through WebSocket.
 * The packet is always either just a payload line or multiple lines. In case of multiple lines, first line is always
 * an ACK header and last line is a payload. All lines between are additional headers.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString(of = {"headers", "payload"})
public class Notification {
    private final String ackHeader;
    private final List<String> headers;
    private final String payload;

    /**
     * Uses {@link JSONBase#getJSONParser()} to parse payload to expected object type
     *
     * @param clazz expected type
     * @param <T>   expected type param
     * @return parsed object
     */
    public <T> T parseJson(Class<T> clazz) {
        return JSONBase.getJSONParser().parse(clazz, payload);
    }

    public Action getAction() {
        if (headers == null || headers.size() < 2) {
            return Action.NONE;
        }
        return EnumUtils.getEnumIgnoreCase(Action.class, headers.get(1), Action.NONE);
    }

    public static Notification parse(String message) {
        List<String> lines = message.lines().toList();

        // last line is a message content and all previous lines are optional headers. First header is ACK, other are notification headers.
        if (lines.size() == 1) { // just message - no headers
            return new Notification(null, Collections.emptyList(), message);
        }

        if (lines.size() == 2) { // just ACK header and message
            return new Notification(lines.get(0), Collections.emptyList(), lines.get(1));
        }

        List<String> headers = Collections.unmodifiableList(lines.subList(1, lines.size() - 1));

        // ACK + other headers + message
        return new Notification(lines.get(0), headers, lines.get(lines.size() - 1));
    }
}

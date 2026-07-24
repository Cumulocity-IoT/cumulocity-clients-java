package com.cumulocity.sdk.client.notification;

import org.apache.commons.io.input.ReaderInputStream;
import org.cometd.bayeux.Message;
import org.cometd.common.JSONContext.Generator;
import org.cometd.common.JSONContext.Parser;
import org.svenson.JSON;
import org.svenson.JSONParser;
import org.svenson.SvensonRuntimeException;
import org.svenson.matcher.EqualsPathMatcher;
import org.svenson.tokenize.InputStreamSource;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public abstract class BaseSvensonJSONContext<T extends Message.Mutable> {

    private final JSON jsonGenerator;

    private final JSONParser jsonParser;

    public BaseSvensonJSONContext(JSON jsonGenerator, JSONParser jsonParser) {
        this.jsonGenerator = jsonGenerator;
        this.jsonParser = jsonParser;
    }

    public BaseSvensonJSONContext(JSON jsonGenerator, JSONParser jsonParser, Class<?> dataType) {
        this(jsonGenerator, jsonParser);
        jsonParser.addTypeHint(new EqualsPathMatcher("[].data"), dataType);
        jsonParser.addTypeHint(new EqualsPathMatcher("[].data[]"), dataType);
    }

    public T[] parse(Reader reader) throws ParseException {
        return parse(new ReaderInputStream(reader));
    }

    // CometD 9's JSONContext.parse(String) returns List<T> (CometD 3 returned an array).
    public List<T> parse(String json) throws ParseException {
        return Arrays.asList(parse(new StringReader(json)));
    }

    public String generate(T message) {
        return getGenerator().generate(message);
    }

    public String generate(List<T> messages) {
        return getGenerator().generate(messages);
    }

    public T[] parse(InputStream stream) throws ParseException {
        try {
            final Object unserializedObject = jsonParser.parse(new InputStreamSource(stream, true));

            final Collection<Map<String, Object>> parsed = asCollection(unserializedObject);
            return asArrayOfMessages(parsed);
        } catch (SvensonRuntimeException x) {
            throw (ParseException) new ParseException("Parsing of message failed - " + x.getMessage(), -1).initCause(x);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Map<String, Object>> asCollection(final Object unserializedObject) throws ParseException {
        if (unserializedObject instanceof Collection) {
            return (Collection<Map<String, Object>>) unserializedObject;
        } else if (unserializedObject instanceof Map) {
            return singletonList((Map<String, Object>) unserializedObject);
        } else {
            throw new ParseException("Invalid Json structure. The root object should be collection or object", -1);
        }
    }

    @SuppressWarnings("unchecked")
    protected T[] asArrayOfMessages(Collection<Map<String, Object>> collection) {
        ArrayList<T> messages = new ArrayList<T>(collection.size());
        for (Map<String, Object> input : collection) {
            messages.add(toMessage(input));
        }
        return messages.toArray((T[]) Array.newInstance(targetClass(), messages.size()));
    }

    @SuppressWarnings("rawtypes")
    protected abstract Class targetClass();

    protected abstract T toMessage(Map<String, Object> messageProperties);

    public Generator getGenerator() {
        return new Generator() {

            @Override
            public String generate(Object object) {
                return jsonGenerator.forValue(object);
            }
        };

    }

    public Parser getParser() {
        return new Parser() {

            @Override
            public <T> T parse(Reader reader, Class<T> type) throws ParseException {
                return jsonParser.parse(type, new InputStreamSource(new ReaderInputStream(reader), true));
            }
        };
    }
}

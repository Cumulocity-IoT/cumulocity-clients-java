package com.cumulocity.model;

import com.cumulocity.model.NotificationConverters.*;
import com.cumulocity.model.audit.AuditChangeValueConverter;
import org.joda.time.DateTime;
import org.svenson.*;
import org.svenson.converter.DefaultTypeConverterRepository;
import org.svenson.converter.TypeConverter;
import org.svenson.tokenize.JSONCharacterSource;
import org.svenson.util.ExceptionWrapper;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static com.cumulocity.model.JSONBase.JSONGeneratorBuilder.jsonGenerator;

/**
 * Base class for all the core classes represented in JSON.
 *
 * @author Sameer Babu KK
 */
public class JSONBase extends AbstractDynamicProperties {

    public static final String ID_TYPE_CONVERTER_NAME = "id-converter";

    /**
     * Marshall the object representation to JSON using svenson library. There
     * different type converters defined and conversion uses the type converter
     * repository.
     *
     * @return the JSON representation.
     */
    public String toJSON() {
        return getJSONGenerator().forValue(this);
    }
    /**
     * Unmarshall the JSON representation to an object of the specified type.
     * There different type converters defined and conversion uses the type
     * converter repository.
     *
     * @param type The class type
     * @param json the json string
     * @return the object of the given type
     */
    public static <T> T fromJSON(String json, Class<T> type) {
        JSONParser parser = getJSONParser();
        return parser.parse(type, json);
    }

    /**
     * Unmarshall the JSON representation to an object of the specified type.
     * There different type converters defined and conversion uses the type
     * converter repository.
     *
     * @param type The class type
     * @param json the json reader
     * @return the object of the given type
     */
    public static <T> T fromJSON(final Reader json, Class<T> type) {
        JSONParser parser = getJSONParser();
        return parser.parse(type, new ReaderJSONCharacterSource(json));
    }

    /**
     * Returns the list of converters used in the entity model.
     */
    @JSONProperty(ignore = true)
    private static List<TypeConverter> defaultTypeConverters() {
        ArrayList<TypeConverter> converters = new ArrayList<>();
        converters.add(new DateTimeConverter());
        converters.add(new IDListTypeConverter());
        converters.add(new DateConverter());
        converters.add(new AuditChangeValueConverter());
        converters.add(new DeletedManagedObjectConverter());
        converters.add(new DeletedMeasurementConverter());
        converters.add(new DeletedEventConverter());
        converters.add(new DeletedAlarmConverter());
        converters.add(new DeletedOperationConverter());
        return converters;
    }

    @JSONProperty(ignore = true)
    private static TypeMapper defaultTypeMapper() {
        return new ClassNameMapper();
    }

    @JSONProperty(ignore = true)
    public static JSON getJSONGenerator() {
        return jsonGenerator()
                .withDefaults()
                .build();
    }

    @JSONProperty(ignore = true)
    public static JSON getJSONGeneratorWithUTF8Encoding() {
        return jsonGenerator()
                .withDefaults()
                .escapeUnicodeChars(false)
                .build();
    }

    @JSONProperty(ignore = true)
    public static JSONParser getJSONParser() {
        return JSONParserBuilder.jsonParser()
                .withDefaults()
                .build();
    }

    public static class ReaderJSONCharacterSource implements JSONCharacterSource {
        private final Reader json;
        private int index;

        public ReaderJSONCharacterSource(Reader json) {
            this.json = json;
            index = 0;
        }

        @Override
        public int nextChar() {
            try {
                int result = json.read();
                index++;
                return (char) result;
            } catch (IOException e) {
                throw ExceptionWrapper.wrap(e);
            }
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public void destroy() {
            try {
                json.close();
            } catch (IOException e) {
                throw ExceptionWrapper.wrap(e);
            }
        }
    }

    public static class JSONParserBuilder {

        private DefaultTypeConverterRepository typeConverterRepository = new DefaultTypeConverterRepository();
        private List<TypeMapper> typeMappers = new ArrayList<>();

        private JSONParserBuilder() {}

        public static JSONParserBuilder jsonParser() {
            return new JSONParserBuilder();
        }

        public JSONParserBuilder withDefaults() {
            this.typeConverters(defaultTypeConverters());
            this.typeConverter(ID_TYPE_CONVERTER_NAME, new IDTypeConverter());
            this.typeMappers.add(defaultTypeMapper());
            return this;
        }

        public JSONParserBuilder typeConverter(TypeConverter converter) {
            typeConverterRepository.addTypeConverter(converter);
            return this;
        }

        public JSONParserBuilder typeConverter(String name, TypeConverter converter) {
            typeConverterRepository.addTypeConverter(name, converter);
            return this;
        }

        public JSONParserBuilder typeMapper(TypeMapper mapper) {
            typeMappers.add(mapper);
            return this;
        }

        public JSONParser build() {
            JSONParser parser = new JSONParser();
            parser.setTypeConverterRepository(typeConverterRepository);
            buildTypeMapper(parser);
            return parser;
        }

        private void buildTypeMapper(JSONParser parser) {
            if (typeMappers.size() == 1) {
                parser.setTypeMapper(typeMappers.get(0));
            } else {
                CompositeTypeMapper compositeTypeMapper = new CompositeTypeMapper();
                compositeTypeMapper.setTypeMappers(typeMappers);
                parser.setTypeMapper(compositeTypeMapper);
            }
        }

        public JSONParserBuilder typeConverters(List<TypeConverter> converters) {
            for (TypeConverter converter :converters) {
                typeConverterRepository.addTypeConverter(converter);
            }
            return this;
        }
    }

    public static class JSONGeneratorBuilder {

        private DefaultTypeConverterRepository typeConverterRepository = new DefaultTypeConverterRepository();
        private Boolean escapeUnicodeChars;

        private JSONGeneratorBuilder(){}

        public static JSONGeneratorBuilder jsonGenerator() {
            return new JSONGeneratorBuilder();
        }

        public JSONGeneratorBuilder withDefaults() {
            this.typeConverters(defaultTypeConverters());
            this.typeConverter(ID_TYPE_CONVERTER_NAME, new IDTypeConverter());
            return this;
        }

        public JSONGeneratorBuilder escapeUnicodeChars(boolean value) {
            escapeUnicodeChars = value;
            return this;
        }

        public JSONGeneratorBuilder typeConverter(TypeConverter converter) {
            typeConverterRepository.addTypeConverter(converter);
            return this;
        }

        public JSONGeneratorBuilder typeConverter(String name, TypeConverter converter) {
            typeConverterRepository.addTypeConverter(name, converter);
            return this;
        }

        public JSON build() {
            JSON generator = new JSON();
            generator.setTypeConverterRepository(typeConverterRepository);
            generator.registerTypeConversion(DateTime.class, new DateTimeConverter());
            if (escapeUnicodeChars != null) {
                generator.setEscapeUnicodeChars(escapeUnicodeChars);
            }
            return generator;
        }

        public void typeConverters(List<TypeConverter> converters) {
            for (TypeConverter converter :converters) {
                typeConverterRepository.addTypeConverter(converter);
            }
        }
    }

}

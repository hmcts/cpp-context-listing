package uk.gov.moj.cpp.listing.domain.json;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Jackson 2 support for {@code jakarta.json} values, replacing the framework's
 * {@code InclusionAwareJSR353Module} (which did the same for {@code jakarta.json}): values are
 * written as JSON (not as beans), {@code null} members of a {@link JsonObject} are left out (the
 * NON_ABSENT inclusion the framework mapper uses), and {@link JsonValue} properties are read back.
 */
public class JakartaJsonModule extends SimpleModule {

    public JakartaJsonModule() {
        super("JakartaJsonModule");
        final JsonValueSerializer serializer = new JsonValueSerializer();
        addSerializer(JsonValue.class, serializer);
        addSerializer(JsonObject.class, serializer);
        addSerializer(JsonArray.class, serializer);
        addSerializer(JsonNumber.class, serializer);
        addSerializer(JsonString.class, serializer);
        addDeserializer(JsonValue.class, new JsonValueDeserializer<>(JsonValue.class));
        addDeserializer(JsonObject.class, new JsonValueDeserializer<>(JsonObject.class));
        addDeserializer(JsonArray.class, new JsonValueDeserializer<>(JsonArray.class));
    }

    private static final class JsonValueSerializer extends JsonSerializer<JsonValue> {
        @Override
        public void serialize(final JsonValue value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
            write(value, gen);
        }

        private static void write(final JsonValue value, final JsonGenerator gen) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            switch (value.getValueType()) {
                case TRUE -> gen.writeBoolean(true);
                case FALSE -> gen.writeBoolean(false);
                case NULL -> gen.writeNull();
                case STRING -> gen.writeString(((JsonString) value).getString());
                case NUMBER -> {
                    final JsonNumber num = (JsonNumber) value;
                    if (num.isIntegral()) {
                        gen.writeNumber(num.bigIntegerValue());
                    } else {
                        gen.writeNumber(num.bigDecimalValue());
                    }
                }
                case OBJECT -> {
                    gen.writeStartObject();
                    for (final var entry : ((JsonObject) value).entrySet()) {
                        if (entry.getValue() == null || entry.getValue().getValueType() == JsonValue.ValueType.NULL) {
                            continue;
                        }
                        gen.writeFieldName(entry.getKey());
                        write(entry.getValue(), gen);
                    }
                    gen.writeEndObject();
                }
                case ARRAY -> {
                    gen.writeStartArray();
                    for (final JsonValue v : (JsonArray) value) {
                        write(v, gen);
                    }
                    gen.writeEndArray();
                }
            }
        }
    }

    private static final class JsonValueDeserializer<T extends JsonValue> extends JsonDeserializer<T> {
        private final Class<T> type;

        private JsonValueDeserializer(final Class<T> type) {
            this.type = type;
        }

        @Override
        public T deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
            final JsonNode node = parser.readValueAsTree();
            try (JsonReader reader = Json.createReader(new StringReader(node.toString()))) {
                return type.cast(reader.readValue());
            }
        }

        @Override
        public T getNullValue(final DeserializationContext context) {
            return type.isAssignableFrom(JsonValue.class) ? type.cast(JsonValue.NULL) : null;
        }
    }
}

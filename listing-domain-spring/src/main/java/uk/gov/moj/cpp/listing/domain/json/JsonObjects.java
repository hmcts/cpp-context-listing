package uk.gov.moj.cpp.listing.domain.json;

import static java.lang.String.format;

import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Framework-free replacement for {@code uk.gov.justice.services.messaging.JsonObjects} (same
 * method names and semantics, on {@code jakarta.json}), so ported query code keeps its shape.
 * Only the helpers the ported code uses are provided.
 */
public final class JsonObjects {

    private static final String FIELD_IS_NOT_A_TYPE = "Field %s is not a %s";

    private JsonObjects() {
    }

    public static JsonArrayBuilder createArrayBuilder() {
        return Json.createArrayBuilder();
    }

    public static JsonObjectBuilder createObjectBuilder() {
        return Json.createObjectBuilder();
    }

    public static JsonReader createReader(final Reader reader) {
        return Json.createReader(reader);
    }

    public static JsonReader createReader(final InputStream inputStream) {
        return Json.createReader(inputStream);
    }

    public static Optional<JsonArray> getJsonArray(final JsonObject object, final String... names) {
        return getJsonValue(object, JsonValue.ValueType.ARRAY, JsonObject::getJsonArray, names);
    }

    public static Optional<JsonObject> getJsonObject(final JsonObject object, final String... names) {
        return getJsonValue(object, JsonValue.ValueType.OBJECT, JsonObject::getJsonObject, names);
    }

    public static Optional<JsonNumber> getJsonNumber(final JsonObject object, final String... names) {
        return getJsonValue(object, JsonValue.ValueType.NUMBER, JsonObject::getJsonNumber, names);
    }

    public static Optional<JsonString> getJsonString(final JsonObject object, final String... names) {
        return getJsonValue(object, JsonValue.ValueType.STRING, JsonObject::getJsonString, names);
    }

    public static Optional<String> getString(final JsonObject object, final String... names) {
        return getJsonString(object, names).map(JsonString::getString);
    }

    public static Optional<UUID> getUUID(final JsonObject object, final String... names) {
        return getString(object, names).map(UUID::fromString);
    }

    public static Optional<Long> getLong(final JsonObject object, final String... names) {
        return getJsonNumber(object, names).map(JsonNumber::longValue);
    }

    public static Optional<Boolean> getBoolean(final JsonObject object, final String name) {
        return object.containsKey(name) && !object.isNull(name) ? Optional.of(object.getBoolean(name)) : Optional.empty();
    }

    public static List<UUID> getUUIDs(final JsonObject object, final String... names) {
        return getJsonArray(object, names)
                .map(array -> array.getValuesAs(JsonString.class).stream().map(s -> UUID.fromString(s.getString())).toList())
                .orElse(List.of());
    }

    public static JsonObjectBuilder createObjectBuilderWithFilter(final JsonObject source, final Function<String, Boolean> filter) {
        final JsonObjectBuilder builder = createObjectBuilder();
        source.entrySet().stream()
                .filter(entry -> filter.apply(entry.getKey()))
                .forEach(entry -> builder.add(entry.getKey(), entry.getValue()));
        return builder;
    }

    public static JsonObjectBuilder createObjectBuilder(final JsonObject source) {
        return createObjectBuilderWithFilter(source, key -> true);
    }

    public static <T> JsonArray toJsonArray(final Collection<T> collection, final Function<T, JsonValue> mapper) {
        final JsonArrayBuilder builder = createArrayBuilder();
        collection.forEach(item -> builder.add(mapper.apply(item)));
        return builder.build();
    }

    private static <T extends JsonValue> Optional<T> getJsonValue(final JsonObject object,
                                                                  final JsonValue.ValueType expectedType,
                                                                  final BiFunction<JsonObject, String, T> getter,
                                                                  final String... names) {
        if (object == null || names == null || names.length == 0) {
            throw new IllegalArgumentException("Object and at least one field name are required");
        }
        if (names.length == 1) {
            if (!object.containsKey(names[0])) {
                return Optional.empty();
            }
            final JsonValue.ValueType type = object.get(names[0]).getValueType();
            if (JsonValue.ValueType.NULL.equals(type)) {
                return Optional.empty();
            }
            if (!expectedType.equals(type)) {
                throw new IllegalStateException(format(FIELD_IS_NOT_A_TYPE, names[0], expectedType));
            }
            return Optional.of(getter.apply(object, names[0]));
        }
        final String[] tail = new String[names.length - 1];
        System.arraycopy(names, 1, tail, 0, tail.length);
        return getJsonObject(object, names[0]).flatMap(child -> getJsonValue(child, expectedType, getter, tail));
    }
}

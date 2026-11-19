package uk.gov.moj.cpp.listing.messaging.envelope;

import static java.util.Objects.requireNonNull;

import uk.gov.moj.cpp.listing.domain.json.ZonedDateTimes;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * The {@code _metadata} object of a framework JSON envelope (MBD refactor P2.2, ADR 013).
 *
 * <p>Framework-free replacement for {@code uk.gov.justice.services.messaging.Metadata} /
 * {@code DefaultJsonMetadata} (17.104): the metadata <em>is</em> its JSON object, so fields this
 * class has no accessor for (e.g. {@code event.eventNumber}, {@code source}) survive a parse →
 * serialise round trip unchanged, in their original order. Field paths are the framework's
 * ({@code JsonMetadata} constants):
 * <pre>
 *   id                     UUID string (required)
 *   name                   string (required, non-empty)
 *   createdAt              yyyy-MM-dd'T'HH:mm:ss.SSS'Z' (UTC)
 *   causation              array of UUID strings
 *   context.user           user id (CJSCPPUID)
 *   context.session        session id
 *   correlation.client     client correlation id
 *   stream.id / version    stream (former aggregate) id and version
 *   source                 event source name
 *   event.eventNumber / previousEventNumber
 * </pre>
 * Validation on parse matches {@code DefaultJsonMetadata.metadataFrom}: missing {@code id} or
 * {@code name}, or an empty name, is rejected.
 */
public final class Metadata {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String CREATED_AT = "createdAt";
    public static final String CAUSATION = "causation";
    public static final String CONTEXT = "context";
    public static final String USER = "user";
    public static final String SESSION = "session";
    public static final String CORRELATION = "correlation";
    public static final String CLIENT = "client";
    public static final String STREAM = "stream";
    public static final String STREAM_ID = "id";
    public static final String VERSION = "version";
    public static final String SOURCE = "source";

    private final JsonObject json;
    private final UUID id;
    private final String name;

    private Metadata(final JsonObject json) {
        this.json = requireNonNull(json, "metadata");
        this.id = parseId(json);
        this.name = parseName(json);
    }

    /** Wraps (and validates) a {@code _metadata} JSON object. */
    public static Metadata fromJson(final JsonObject json) {
        return new Metadata(json);
    }

    /** A new, empty builder; {@code id} and {@code name} must be set before {@link Builder#build()}. */
    public static Builder builder() {
        return new Builder(Json.createObjectBuilder().build());
    }

    /** A builder starting from a copy of {@code metadata} (the framework's {@code metadataFrom}). */
    public static Builder from(final Metadata metadata) {
        return new Builder(metadata.json);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Optional<ZonedDateTime> createdAt() {
        return string(json, CREATED_AT).map(ZonedDateTimes::fromString);
    }

    public List<UUID> causation() {
        final JsonValue value = json.get(CAUSATION);
        if (value == null || value.getValueType() != JsonValue.ValueType.ARRAY) {
            return List.of();
        }
        final List<UUID> ids = new ArrayList<>();
        for (final JsonValue element : (JsonArray) value) {
            ids.add(UUID.fromString(((JsonString) element).getString()));
        }
        return Collections.unmodifiableList(ids);
    }

    public Optional<String> userId() {
        return nested(CONTEXT, USER);
    }

    public Optional<String> sessionId() {
        return nested(CONTEXT, SESSION);
    }

    public Optional<String> clientCorrelationId() {
        return nested(CORRELATION, CLIENT);
    }

    public Optional<UUID> streamId() {
        return nested(STREAM, STREAM_ID).map(UUID::fromString);
    }

    public Optional<Long> version() {
        final JsonValue stream = json.get(STREAM);
        if (stream instanceof JsonObject object && object.get(VERSION) instanceof JsonNumber number) {
            return Optional.of(number.longValue());
        }
        return Optional.empty();
    }

    public Optional<String> source() {
        return string(json, SOURCE);
    }

    /** The metadata as JSON, exactly as parsed or built (all fields, original order). */
    public JsonObject asJsonObject() {
        return json;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Metadata metadata && json.equals(metadata.json);
    }

    @Override
    public int hashCode() {
        return json.hashCode();
    }

    @Override
    public String toString() {
        return json.toString();
    }

    private Optional<String> nested(final String object, final String field) {
        final JsonValue value = json.get(object);
        return value instanceof JsonObject nested ? string(nested, field) : Optional.empty();
    }

    private static Optional<String> string(final JsonObject object, final String field) {
        final JsonValue value = object.get(field);
        return value instanceof JsonString string ? Optional.of(string.getString()) : Optional.empty();
    }

    private static UUID parseId(final JsonObject json) {
        final String value = string(json, ID).orElseThrow(() -> new EnvelopeFormatException("Missing id field"));
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException e) {
            throw new EnvelopeFormatException("Metadata id is not a UUID: " + value, e);
        }
    }

    private static String parseName(final JsonObject json) {
        final String value = string(json, NAME).orElseThrow(() -> new EnvelopeFormatException("Missing name field"));
        if (value.isEmpty()) {
            throw new EnvelopeFormatException("Name field cannot be empty");
        }
        return value;
    }

    /**
     * Builds metadata the way the framework's {@code MetadataBuilder} does: each setter writes one
     * JSON path, keeping every other field (and the field order) of the starting metadata.
     */
    public static final class Builder {

        private final Map<String, JsonValue> fields;

        private Builder(final JsonObject start) {
            this.fields = new LinkedHashMap<>(start);
        }

        public Builder withId(final UUID id) {
            return put(ID, Json.createValue(id.toString()));
        }

        public Builder withName(final String name) {
            return put(NAME, Json.createValue(name));
        }

        public Builder createdAt(final ZonedDateTime createdAt) {
            return put(CREATED_AT, Json.createValue(ZonedDateTimes.toString(createdAt)));
        }

        public Builder withCausation(final List<UUID> causation) {
            final JsonArrayBuilder array = Json.createArrayBuilder();
            causation.forEach(id -> array.add(id.toString()));
            return put(CAUSATION, array.build());
        }

        public Builder withUserId(final String userId) {
            return putNested(CONTEXT, USER, Json.createValue(userId));
        }

        public Builder withSessionId(final String sessionId) {
            return putNested(CONTEXT, SESSION, Json.createValue(sessionId));
        }

        public Builder withClientCorrelationId(final String clientCorrelationId) {
            return putNested(CORRELATION, CLIENT, Json.createValue(clientCorrelationId));
        }

        public Builder withStreamId(final UUID streamId) {
            return putNested(STREAM, STREAM_ID, Json.createValue(streamId.toString()));
        }

        public Builder withVersion(final long version) {
            return putNested(STREAM, VERSION, Json.createValue(version));
        }

        public Builder withSource(final String source) {
            return put(SOURCE, Json.createValue(source));
        }

        public Metadata build() {
            return new Metadata(Json.createObjectBuilder(fields).build());
        }

        private Builder put(final String field, final JsonValue value) {
            fields.put(field, value);
            return this;
        }

        private Builder putNested(final String object, final String field, final JsonValue value) {
            final JsonValue existing = fields.get(object);
            final Map<String, JsonValue> nested = existing instanceof JsonObject nestedObject
                    ? new LinkedHashMap<>(nestedObject) : new LinkedHashMap<>();
            nested.put(field, value);
            fields.put(object, Json.createObjectBuilder(nested).build());
            return this;
        }
    }
}

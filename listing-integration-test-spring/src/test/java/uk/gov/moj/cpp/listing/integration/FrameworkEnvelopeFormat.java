package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * The framework's acceptance rules for a {@code public.event} message body, re-stated for the ITs
 * (MBD refactor P2.2, ADR 013), so a message the Spring app publishes is checked the way a
 * downstream framework consumer reads it: {@code DefaultEnvelopeConverter.fromMessage} →
 * {@code DefaultJsonObjectEnvelopeConverter.asEnvelope} → {@code DefaultJsonMetadata.metadataFrom}
 * (framework 17.104, read with javap): a JSON object with a {@code _metadata} object whose
 * {@code id} is a UUID and whose {@code name} is non-empty; every other top-level field is payload.
 * It also compares the shape with the message captured from the JEE app
 * ({@code legacy/public.listing.created-listing-note.json}).
 */
final class FrameworkEnvelopeFormat {

    static final String CPPNAME = "CPPNAME";
    private static final Pattern CREATED_AT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
    /** Field paths the framework's metadata knows (JsonMetadata constants). */
    private static final List<String> FRAMEWORK_METADATA_FIELDS =
            List.of("id", "name", "createdAt", "causation", "context", "correlation", "stream", "source", "event");

    private FrameworkEnvelopeFormat() {
    }

    /** Parses as the framework does; fails the test if the framework would reject the body. */
    static JsonObject accept(final String body) {
        final JsonObject envelope;
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            envelope = reader.readObject();
        }
        assertThat(envelope.get("_metadata")).as("_metadata object").isInstanceOf(JsonObject.class);
        final JsonObject metadata = envelope.getJsonObject("_metadata");
        assertThat(UUID.fromString(metadata.getString("id"))).isNotNull();
        assertThat(metadata.getString("name")).isNotEmpty();
        return envelope;
    }

    /** The published body has the legacy message's structure: same field order rule and JSON types. */
    static void assertSameShapeAsLegacy(final JsonObject published) {
        final JsonObject legacy = accept(legacyMessage());
        assertThat(published.keySet().iterator().next()).as("_metadata is the first field").isEqualTo("_metadata");
        final JsonObject metadata = published.getJsonObject("_metadata");
        assertThat(FRAMEWORK_METADATA_FIELDS).containsAll(metadata.keySet());
        for (final Map.Entry<String, JsonValue> field : legacy.getJsonObject("_metadata").entrySet()) {
            assertThat(metadata).as("metadata field " + field.getKey()).containsKey(field.getKey());
            assertThat(metadata.get(field.getKey()).getValueType()).as("type of " + field.getKey())
                    .isEqualTo(field.getValue().getValueType());
        }
        assertThat(metadata.getString("createdAt")).matches(CREATED_AT);
        assertThat(metadata.getJsonObject("context").getString("user")).isNotEmpty();
    }

    static String legacyMessage() {
        try (InputStream in = FrameworkEnvelopeFormat.class.getClassLoader()
                .getResourceAsStream("legacy/public.listing.created-listing-note.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

package uk.gov.moj.cpp.listing.testutil;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Test-only stand-in for the framework's envelope builders, used by the ported legacy tests:
 * the Spring query services take the payload only, so {@code envelopeFrom(metadata, payload)}
 * just returns the payload and the metadata builder accepts and ignores everything.
 */
public final class Envelopes {

    private Envelopes() {
    }

    public static JsonObject envelopeFrom(final Object metadata, final JsonObject payload) {
        return payload;
    }

    public static JsonObject envelopeFrom(final Object metadata, final JsonObjectBuilder payload) {
        return payload.build();
    }

    public static JsonObject envelopeFrom(final Object metadata, final JsonValue payload) {
        return payload instanceof JsonObject json ? json : null;
    }

    public static JsonObject createEnvelope(final String name, final JsonObject payload) {
        return payload;
    }

    public static Metadata metadataBuilder() {
        return new Metadata();
    }

    public static Metadata metadataOf(final UUID id, final String name) {
        return new Metadata();
    }

    public static Metadata metadataWithRandomUUID(final String name) {
        return new Metadata();
    }

    public static Metadata metadataWithRandomUUIDAndName() {
        return new Metadata();
    }

    public static Metadata metadataWithDefaults() {
        return new Metadata();
    }

    /** Fluent no-op metadata builder. */
    public static final class Metadata {
        public Metadata withId(final UUID id) { return this; }
        public Metadata withName(final String name) { return this; }
        public Metadata withUserId(final String userId) { return this; }
        public Metadata withClientCorrelationId(final String id) { return this; }
        public Metadata withSessionId(final String id) { return this; }
        public Metadata withCausation(final UUID... ids) { return this; }
        public Metadata createdAt(final ZonedDateTime createdAt) { return this; }
        public Metadata build() { return this; }
    }
}

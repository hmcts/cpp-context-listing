package uk.gov.moj.cpp.listing.command.hearing.flow;

import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * The envelope-building idioms of the JEE processor and command API
 * ({@code envelopeFrom(metadataFrom(event.metadata()).withName(…), payload)},
 * {@code envelop(payload).withName(…).withMetadataFrom(event)}) on the framework-free
 * {@link Envelope}/{@link Metadata} (MBD refactor P2.3.2), so the ported code keeps its shape.
 */
public final class Envelopes {

    private static final ObjectToJsonObjectConverter TO_JSON = new ObjectToJsonObjectConverter();

    private Envelopes() {
    }

    public static Metadata.Builder metadataFrom(final Metadata metadata) {
        return Metadata.from(metadata);
    }

    public static Envelope envelopeFrom(final Metadata.Builder metadata, final JsonValue payload) {
        return envelopeFrom(metadata.build(), payload);
    }

    public static Envelope envelopeFrom(final Metadata metadata, final JsonValue payload) {
        return new Envelope(metadata, asObject(payload));
    }

    /** A payload object (a POJO or JSON) as the framework serialised it. */
    public static Envelope envelopeFrom(final Metadata metadata, final Object payload) {
        if (payload == null || payload instanceof JsonValue) {
            return new Envelope(metadata, asObject((JsonValue) payload));
        }
        return new Envelope(metadata, TO_JSON.convert(payload));
    }

    public static Envelope envelopeFrom(final Metadata.Builder metadata, final Object payload) {
        return envelopeFrom(metadata.build(), payload);
    }

    public static Enveloper envelop(final Object payload) {
        return new Enveloper(payload);
    }

    private static JsonObject asObject(final JsonValue payload) {
        if (payload == null || payload == JsonValue.NULL) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }
        return payload instanceof JsonObject object ? object : payload.asJsonObject();
    }

    /** {@code envelop(payload).withName(name).withMetadataFrom(cause)}. */
    public static final class Enveloper {
        private final Object payload;
        private String name;

        private Enveloper(final Object payload) {
            this.payload = payload;
        }

        public Enveloper withName(final String name) {
            this.name = name;
            return this;
        }

        public Envelope withMetadataFrom(final Envelope cause) {
            return envelopeFrom(Metadata.from(cause.metadata()).withName(name), payload);
        }

        public Envelope withMetadataFrom(final RaisedEvent<?> cause) {
            return envelopeFrom(Metadata.from(cause.metadata()).withName(name), payload);
        }
    }
}

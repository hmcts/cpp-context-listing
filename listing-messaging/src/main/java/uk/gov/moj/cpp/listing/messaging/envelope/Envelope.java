package uk.gov.moj.cpp.listing.messaging.envelope;

import static java.util.Objects.requireNonNull;

import jakarta.json.JsonObject;

/**
 * A framework JSON envelope (MBD refactor P2.2, ADR 013): {@link Metadata} plus a JSON object
 * payload. On the wire it is one JSON object, {@code _metadata} first, followed by the payload's
 * fields at top level (see {@link EnvelopeCodec}).
 *
 * <p>Framework-free replacement for {@code uk.gov.justice.services.messaging.JsonEnvelope}. Only
 * object payloads exist on {@code public.event}; an envelope without payload fields has an empty
 * payload object (the framework's {@code JsonValue.NULL} payload serialises to the same JSON).
 *
 * @param metadata the {@code _metadata} object
 * @param payload  the payload fields, never {@code null}
 */
public record Envelope(Metadata metadata, JsonObject payload) {

    public Envelope {
        requireNonNull(metadata, "metadata");
        requireNonNull(payload, "payload");
    }

    public String name() {
        return metadata.name();
    }
}

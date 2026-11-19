package uk.gov.moj.cpp.listing.command.hearing.flow;

import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import jakarta.json.JsonObject;

/**
 * A decision of the ported Hearing rules, in memory only (MBD refactor P2.3.2, ADR 015): what the
 * JEE aggregate raised as a private event. It is handed to the listener port (the viewstore writes)
 * and the processor port (the public messages and follow-up commands) within the command's
 * transaction, and then discarded: it is never stored or published.
 *
 * <p>{@code metadata} is what the JEE event carried: the command's metadata with a new id, the
 * event name, {@code createdAt}, the command id appended to the causation and {@code stream.id}
 * (the hearing id); the processor's public messages take theirs from it.
 */
public record RaisedEvent<T>(Metadata metadata, T payload) {

    private static final ObjectToJsonObjectConverter TO_JSON = new ObjectToJsonObjectConverter();

    public String name() {
        return metadata.name();
    }

    /** The payload as the JEE event store held it (framework JSON conventions). */
    public JsonObject payloadAsJsonObject() {
        return TO_JSON.convert(payload);
    }

    /** As the JEE processor saw it: a JSON envelope. */
    public Envelope asJsonEnvelope() {
        return new Envelope(metadata, payloadAsJsonObject());
    }
}

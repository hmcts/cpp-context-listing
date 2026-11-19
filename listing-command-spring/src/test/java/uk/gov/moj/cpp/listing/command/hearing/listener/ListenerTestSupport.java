package uk.gov.moj.cpp.listing.command.hearing.listener;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import jakarta.json.JsonObject;

/** The framework test helpers the ported listener tests used ({@code envelopeFrom}, {@code metadataWithRandomUUID}). */
public final class ListenerTestSupport {

    private ListenerTestSupport() {
    }

    public static Metadata metadataWithRandomUUID(final String name) {
        return Metadata.builder().withId(randomUUID()).withName(name).build();
    }

    public static Metadata metadataWithDefaults() {
        return metadataWithRandomUUID("listing.test-event");
    }

    public static <T> RaisedEvent<T> envelopeFrom(final Metadata metadata, final T payload) {
        return new RaisedEvent<>(metadata, payload);
    }

    public static Envelope jsonEnvelopeFrom(final Metadata metadata, final JsonObject payload) {
        return new Envelope(metadata, payload);
    }
}

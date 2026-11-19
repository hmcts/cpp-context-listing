package uk.gov.moj.cpp.listing.query.view;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Stands in for the framework's {@code envelopeFrom(metadata, payload)} where the JEE query view
 * wrapped its result in a response envelope: the Spring port returns the payload only (the HTTP
 * layer never sent the envelope metadata, see listing-api's EnvelopeResponseBodyAdvice).
 */
public final class Payloads {

    private Payloads() {
    }

    public static JsonObject payload(final JsonObject payload) {
        return payload;
    }

    public static JsonObject payload(final JsonObjectBuilder payload) {
        return payload.build();
    }
}

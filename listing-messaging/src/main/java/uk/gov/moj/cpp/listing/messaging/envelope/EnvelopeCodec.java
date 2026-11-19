package uk.gov.moj.cpp.listing.messaging.envelope;

import uk.gov.moj.cpp.listing.domain.json.JakartaJsonModule;

import java.io.StringReader;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Reads and writes the framework's JSON envelope wire format (MBD refactor P2.2, ADR 013).
 *
 * <p>Same rules as {@code DefaultJsonObjectEnvelopeConverter} (framework 17.104, read with
 * {@code javap}):
 * <ul>
 *     <li><b>write</b> ({@code fromEnvelope} + {@code asJsonString}): one JSON object, first the
 *     {@code _metadata} object, then every payload field at top level in payload order; serialised
 *     compactly by Jackson with a JSR-353 module that leaves out {@code null} members
 *     (the framework's {@code InclusionAwareJSR353Module}; here listing's {@link JakartaJsonModule});</li>
 *     <li><b>read</b> ({@code asEnvelope}): {@code _metadata} becomes the metadata (id and name
 *     required), every other top-level field is the payload.</li>
 * </ul>
 * Used for the outbox {@code envelope} column, the JMS {@code TextMessage} body on
 * {@code public.event}, and inbound messages.
 */
public class EnvelopeCodec {

    public static final String METADATA = "_metadata";

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JakartaJsonModule());

    public Envelope parse(final String json) {
        final JsonObject object;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            object = reader.readObject();
        } catch (final JsonException | IllegalStateException e) {
            throw new EnvelopeFormatException("Message body is not a JSON object", e);
        }
        return fromJsonObject(object);
    }

    public Envelope fromJsonObject(final JsonObject object) {
        final JsonValue metadata = object.get(METADATA);
        if (!(metadata instanceof JsonObject metadataObject)) {
            throw new EnvelopeFormatException("Message body has no " + METADATA + " object");
        }
        final JsonObjectBuilder payload = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> field : object.entrySet()) {
            if (!METADATA.equals(field.getKey())) {
                payload.add(field.getKey(), field.getValue());
            }
        }
        return new Envelope(Metadata.fromJson(metadataObject), payload.build());
    }

    public JsonObject toJsonObject(final Envelope envelope) {
        final JsonObjectBuilder object = Json.createObjectBuilder().add(METADATA, envelope.metadata().asJsonObject());
        envelope.payload().forEach(object::add);
        return object.build();
    }

    public String toJson(final Envelope envelope) {
        try {
            return MAPPER.writeValueAsString(toJsonObject(envelope));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize JSON envelope " + envelope.metadata().id(), e);
        }
    }
}

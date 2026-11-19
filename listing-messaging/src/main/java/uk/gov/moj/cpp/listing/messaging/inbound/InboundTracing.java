package uk.gov.moj.cpp.listing.messaging.inbound;

import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.slf4j.MDC;

/**
 * The MDC ids of one consumed {@code public.event} message (MBD refactor P2.4; D24;
 * context/logging-standards.md "Required MDC fields"), the JMS counterpart of the HTTP
 * {@code TracingFilter}, with the same keys ({@code Tracing} in listing-common-spring):
 * <ul>
 *     <li>{@value #CORRELATION_ID}: the envelope's {@code _metadata.correlation.client}, else its
 *     {@code _metadata.id}, else the JMS message id, else a new UUID. Only a plain token is taken
 *     from the message (the value is logged); anything else falls through to the next source.
 *     Outbound REST calls made while handling the message propagate it ({@code CppRestClientConfig});</li>
 *     <li>{@value #REQUEST_ID}: a new UUID per delivery (a redelivery gets a new one).</li>
 * </ul>
 * The body is read leniently here: a body that is not an envelope still gets ids, and the parse
 * that decides (and rejects it) is {@code InboundEventProcessor}'s.
 */
public final class InboundTracing {

    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private InboundTracing() {
    }

    /** Puts both ids in the MDC; pair with {@link #end()} in a {@code finally}. */
    public static void begin(final String body, final String jmsMessageId) {
        MDC.put(CORRELATION_ID, correlationIdOf(body, jmsMessageId));
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
    }

    public static void end() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(REQUEST_ID);
    }

    static String correlationIdOf(final String body, final String jmsMessageId) {
        final Optional<JsonObject> metadata = metadataOf(body);
        return metadata.flatMap(m -> safeString(m.get("correlation") instanceof JsonObject correlation ? correlation.get("client") : null))
                .or(() -> metadata.flatMap(m -> safeString(m.get("id"))))
                .or(() -> safe(jmsMessageId))
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    private static Optional<JsonObject> metadataOf(final String body) {
        if (body == null) {
            return Optional.empty();
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            final JsonValue value = reader.readValue();
            if (value instanceof JsonObject envelope && envelope.get("_metadata") instanceof JsonObject metadata) {
                return Optional.of(metadata);
            }
        } catch (final JsonException | IllegalStateException e) {
            // not JSON: no ids from the body
        }
        return Optional.empty();
    }

    private static Optional<String> safeString(final JsonValue value) {
        return value instanceof JsonString string ? safe(string.getString()) : Optional.empty();
    }

    private static Optional<String> safe(final String value) {
        return value != null && SAFE_ID.matcher(value.trim()).matches() ? Optional.of(value.trim()) : Optional.empty();
    }
}

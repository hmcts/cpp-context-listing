package uk.gov.moj.cpp.listing.envelope;

import jakarta.json.JsonValue;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import uk.gov.moj.cpp.listing.config.JsonValueConverter;

/**
 * Reproduces the {@code _metadata} envelope structure of the framework's {@code JsonEnvelope}.
 * Ported from courtscheduler. Only used when {@code listing.envelope.enabled=true}; see
 * {@link EnvelopeResponseBodyAdvice} for why listing defaults to payload-only responses.
 */
public final class JsonEnvelopeWrapper {

    private JsonEnvelopeWrapper() { }

    public static Map<String, Object> wrap(final Object payload, final String id, final String name, final String userId) {
        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", id == null ? UUID.randomUUID().toString() : id);
        metadata.put("name", name);
        metadata.put("createdAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        if (userId != null && !userId.isBlank()) {
            metadata.put("userId", userId);
        }

        final Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("_metadata", metadata);
        if (payload instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((k, v) -> envelope.put(String.valueOf(k), unwrapJsonValue(v)));
        } else if (payload instanceof JsonValue jv) {
            final Object converted = JsonValueConverter.toJava(jv);
            if (converted instanceof Map<?, ?> convertedMap) {
                convertedMap.forEach((k, v) -> envelope.put(String.valueOf(k), v));
            } else {
                envelope.put("payload", converted);
            }
        } else if (payload != null) {
            envelope.put("payload", payload);
        }
        return envelope;
    }

    /**
     * Spring Boot 4 uses Jackson 3 for HTTP message conversion; reflecting on {@link JsonValue}
     * subtypes emits bean fields like {@code {"integral":true,"valueType":"NUMBER"}}. Deep-convert
     * here so the output matches the legacy wire shape.
     */
    private static Object unwrapJsonValue(final Object value) {
        if (value instanceof JsonValue jv) {
            return JsonValueConverter.toJava(jv);
        }
        return value;
    }
}

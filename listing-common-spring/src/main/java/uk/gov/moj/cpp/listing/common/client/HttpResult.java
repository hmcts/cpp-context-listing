package uk.gov.moj.cpp.listing.common.client;

import jakarta.json.JsonObject;

/**
 * Status and body of an outbound HTTP call, in place of the JAX-RS {@code Response} the JEE
 * services returned. {@code entity} is a {@link JsonObject} for a parseable JSON body, otherwise
 * the raw body {@link String} (as the legacy code did).
 */
public record HttpResult(int status, Object entity) {

    public boolean isOk() {
        return status == 200;
    }

    public boolean hasEntity() {
        return entity != null;
    }

    public JsonObject jsonEntity() {
        return entity instanceof JsonObject json ? json : null;
    }
}

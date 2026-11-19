package uk.gov.moj.cpp.listing.controllers;

import java.util.Map;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import uk.gov.moj.cpp.listing.config.JsonValueConverter;

/**
 * Builds the query payload the way the framework's REST adapter did: every request parameter
 * that is present becomes a payload field, typed by its RAML type (string and date → JSON string,
 * boolean → JSON boolean, integer → JSON number); absent optional parameters are left out (RAML
 * defaults were not injected).
 */
final class QueryParameters {

    static final String CJSCPPUID = "CJSCPPUID";

    private final JsonObjectBuilder builder = Json.createObjectBuilder();

    static QueryParameters query() {
        return new QueryParameters();
    }

    QueryParameters string(final String name, final String value) {
        if (value != null) {
            builder.add(name, value);
        }
        return this;
    }

    QueryParameters bool(final String name, final Boolean value) {
        if (value != null) {
            builder.add(name, value);
        }
        return this;
    }

    QueryParameters number(final String name, final Integer value) {
        if (value != null) {
            builder.add(name, value);
        }
        return this;
    }

    JsonObject build() {
        return builder.build();
    }

    /** The caller's CJSCPPUID (the framework's metadata userId); {@code null} when absent. */
    static UUID userId(final HttpServletRequest request) {
        final String header = request.getHeader(CJSCPPUID);
        return header == null || header.isBlank() ? null : UUID.fromString(header.trim());
    }

    static Map<String, Object> body(final JsonObject payload) {
        return JsonValueConverter.toMap(payload);
    }
}

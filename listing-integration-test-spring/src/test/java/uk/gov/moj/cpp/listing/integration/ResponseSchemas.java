package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import uk.gov.moj.cpp.listing.validation.LegacySchemaCatalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Validates query responses against the legacy RAML response schemas
 * (listing-query-api/src/raml/json/schema), loaded from listing-api's classpath copy
 * ({@code request-schemas/}) with its schema catalog for the remote {@code $ref}s — the same
 * everit draft-04 set-up as request validation (P2.0).
 */
final class ResponseSchemas {

    private static final LegacySchemaCatalog CATALOG = LegacySchemaCatalog.loadFromClasspath();

    /**
     * listing-json's court-schedule-judiciary.json (used by the hearing-slots response schema) has a
     * broken {@code "$ref": "boolean"}, which resolves to this id; the framework never compiled that
     * schema for a response. It is served as {@code {"type":"boolean"}} here so the rest of the
     * response can be validated (legacy schema defect, reported, schema file unchanged).
     */
    private static final String BROKEN_BOOLEAN_REF = "http://justice.gov.uk/listing/events/boolean";
    private static final SchemaClient CLIENT = url -> BROKEN_BOOLEAN_REF.equals(url)
            ? new ByteArrayInputStream("{\"type\":\"boolean\"}".getBytes(StandardCharsets.UTF_8))
            : CATALOG.get(url);
    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>();

    private ResponseSchemas() {
    }

    static void assertValid(final String schemaFile, final String body) {
        assertThat(body).as("response body").isNotBlank();
        try {
            schema(schemaFile).validate(new JSONObject(body));
        } catch (final ValidationException e) {
            fail("Response does not match " + schemaFile + ": " + String.join("; ", e.getAllMessages()) + "\nBody: " + body);
        }
    }

    private static Schema schema(final String file) {
        return SCHEMAS.computeIfAbsent(file, name -> {
            try (InputStream in = ResponseSchemas.class.getClassLoader().getResourceAsStream("request-schemas/" + name)) {
                if (in == null) {
                    throw new IllegalStateException("Schema " + name + " not on the classpath");
                }
                return SchemaLoader.builder()
                        .schemaClient(CLIENT)
                        .schemaJson(new JSONObject(new JSONTokener(in)))
                        .build()
                        .load()
                        .build();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}

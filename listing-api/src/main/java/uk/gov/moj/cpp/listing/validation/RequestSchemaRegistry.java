package uk.gov.moj.cpp.listing.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Maps each request vendor media type to its compiled legacy JSON schema.
 *
 * <p>The OpenAPI spec is the single source of the mapping: every request-body media type that
 * RAML gave a schema carries {@code x-json-schema: <file>} (the file the RAML
 * {@code schema: !include json/schema/<file>} pointed at). The mapping cannot be derived from the
 * media type alone, because several listing media types and their schema files differ (for
 * example {@code application/vnd.listing.command.edit-listing-note+json} →
 * {@code listing.command.edit-note-for-listing.json}), unlike courtscheduler where
 * {@code <token>.json} always matched.
 *
 * <p>All schemas are compiled at startup, so a missing file or an unresolvable {@code $ref} stops
 * the application instead of silently disabling validation.
 */
@Component
public class RequestSchemaRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSchemaRegistry.class);
    static final String SCHEMA_EXTENSION = "x-json-schema";

    private final Map<String, String> schemaFileByMediaType;
    private final Map<String, Schema> schemaByMediaType;

    @Autowired
    public RequestSchemaRegistry(
            @Value("${audit.http.openapi-rest-spec:openapi/listing-api.openapi.yml}") final String specLocation) {
        this(specLocation, LegacySchemaCatalog.loadFromClasspath());
    }

    RequestSchemaRegistry(final String specLocation, final LegacySchemaCatalog catalog) {
        this.schemaFileByMediaType = Collections.unmodifiableMap(readRequestSchemaMapping(specLocation));
        final Map<String, Schema> compiled = new LinkedHashMap<>();
        schemaFileByMediaType.forEach((mediaType, file) -> compiled.put(mediaType, compile(file, catalog)));
        this.schemaByMediaType = Collections.unmodifiableMap(compiled);
        LOG.info("Request schema validation enabled for {} vendor media types", schemaByMediaType.size());
    }

    /** The compiled schema for a request media type (type/subtype only, case-insensitive), if any. */
    public Optional<Schema> schemaFor(final String mediaType) {
        return Optional.ofNullable(schemaByMediaType.get(mediaType.toLowerCase(Locale.ROOT)));
    }

    public Map<String, String> schemaFileByMediaType() {
        return schemaFileByMediaType;
    }

    private static Schema compile(final String file, final LegacySchemaCatalog catalog) {
        final ClassPathResource resource = new ClassPathResource(LegacySchemaCatalog.SCHEMA_ROOT + file);
        try (InputStream in = resource.getInputStream()) {
            final JSONObject json = new JSONObject(new JSONTokener(in));
            // The draft (04) is detected from each schema's $schema, as in the framework.
            return SchemaLoader.builder()
                    .schemaClient(catalog)
                    .schemaJson(json)
                    .build()
                    .load()
                    .build();
        } catch (final IOException e) {
            throw new UncheckedIOException("Request schema " + file + " not found on the classpath", e);
        } catch (final RuntimeException e) {
            throw new IllegalStateException("Request schema " + file + " could not be compiled: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> readRequestSchemaMapping(final String specLocation) {
        final Map<String, Object> spec;
        try (InputStream in = new ClassPathResource(specLocation).getInputStream()) {
            spec = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
        } catch (final IOException e) {
            throw new UncheckedIOException("OpenAPI spec " + specLocation + " not found on the classpath", e);
        }
        final Map<String, String> mapping = new LinkedHashMap<>();
        final Map<String, Object> paths = (Map<String, Object>) spec.getOrDefault("paths", Map.of());
        paths.values().forEach(pathItem -> ((Map<String, Object>) pathItem).values().forEach(operation -> {
            if (!(operation instanceof Map<?, ?> op) || !(op.get("requestBody") instanceof Map<?, ?> body)
                    || !(body.get("content") instanceof Map<?, ?> content)) {
                return;
            }
            content.forEach((mediaType, mediaTypeObject) -> {
                if (mediaTypeObject instanceof Map<?, ?> mto && mto.get(SCHEMA_EXTENSION) instanceof String file) {
                    final String key = String.valueOf(mediaType).toLowerCase(Locale.ROOT);
                    final String previous = mapping.putIfAbsent(key, file);
                    if (previous != null && !previous.equals(file)) {
                        throw new IllegalStateException("Media type " + key + " maps to both " + previous + " and " + file);
                    }
                }
            });
        }));
        return mapping;
    }
}

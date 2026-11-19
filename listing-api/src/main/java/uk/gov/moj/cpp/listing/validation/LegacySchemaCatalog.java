package uk.gov.moj.cpp.listing.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.everit.json.schema.loader.SchemaClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Classpath replacement for the framework's JSON schema catalog ({@code schema_catalog.json} in
 * listing-json and the core-domain schema jars; the core-domain schemas reached are vendored under
 * {@code request-schemas/catalog/vendored-core-domain}, D22). The legacy request schemas reference shared
 * definitions by absolute id ({@code "$ref": "http://justice.gov.uk/core/courts/courtsDefinitions.json#/definitions/uuid"});
 * the framework resolved those ids from the catalog, never over the network.
 *
 * <p>This class indexes every {@code request-schemas/**}{@code /*.json} resource by its draft-04
 * {@code id} and serves it to everit as a {@link SchemaClient}. An id that is not on the
 * classpath fails loudly: no remote fetch is ever attempted.
 */
public final class LegacySchemaCatalog implements SchemaClient {

    private static final Logger LOG = LoggerFactory.getLogger(LegacySchemaCatalog.class);
    static final String SCHEMA_ROOT = "request-schemas/";

    private final Map<String, byte[]> schemasById;

    private LegacySchemaCatalog(final Map<String, byte[]> schemasById) {
        this.schemasById = Collections.unmodifiableMap(schemasById);
    }

    public static LegacySchemaCatalog loadFromClasspath() {
        final Map<String, byte[]> byId = new HashMap<>();
        try {
            final Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:" + SCHEMA_ROOT + "**/*.json");
            for (final Resource resource : resources) {
                index(resource, byId);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not scan classpath for " + SCHEMA_ROOT, e);
        }
        LOG.info("Indexed {} legacy JSON schemas by id", byId.size());
        return new LegacySchemaCatalog(byId);
    }

    private static void index(final Resource resource, final Map<String, byte[]> byId) throws IOException {
        final byte[] bytes;
        try (InputStream in = resource.getInputStream()) {
            bytes = in.readAllBytes();
        }
        final Object parsed;
        try {
            parsed = new JSONTokener(new ByteArrayInputStream(bytes)).nextValue();
        } catch (final JSONException notJson) {
            LOG.debug("Skipping non-JSON resource {}", resource);
            return;
        }
        if (!(parsed instanceof JSONObject json)) {
            return;
        }
        final String id = json.optString("id", json.optString("$id", null));
        if (id == null || id.isBlank()) {
            return;
        }
        // First one wins, as in the framework catalog; a clash is worth knowing about.
        if (byId.putIfAbsent(normalise(id), bytes) != null) {
            LOG.debug("Duplicate schema id {} (keeping first, ignoring {})", id, resource);
        }
    }

    static String normalise(final String uri) {
        String result = uri.trim();
        final int hash = result.indexOf('#');
        if (hash >= 0) {
            result = result.substring(0, hash);
        }
        return result;
    }

    public boolean contains(final String uri) {
        return schemasById.containsKey(normalise(uri));
    }

    public int size() {
        return schemasById.size();
    }

    @Override
    public InputStream get(final String url) {
        final byte[] bytes = schemasById.get(normalise(url));
        if (bytes == null) {
            throw new UncheckedIOException(new IOException(
                    "JSON schema " + url + " is not in the classpath schema catalog (" + SCHEMA_ROOT + ")"));
        }
        return new ByteArrayInputStream(bytes);
    }
}

package uk.gov.moj.cpp.listing.contract;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Public message contract (MBD refactor P2.5, §4.4, §12): the payload of every {@code public.*}
 * message the Spring app published during the legacy IT run is valid against the legacy public JSON
 * schema of that message name. One dynamic test per name of the legacy
 * {@code public-publications-descriptor.yaml} (the 34 names listing publishes).
 *
 * <p>Inputs (system properties set by the {@code publicMessageContractTest} Gradle task, which runs
 * after {@code legacyIntegrationTest}):
 * <ul>
 * <li>{@code public.contract.dir}: {@code listing-messaging/src/contract} — the descriptor, the
 * processor's {@code json/schema/public*.json} (moved unchanged from listing-event-processor) and
 * the three core-domain schemas their {@code $ref}s reach that listing-api does not vendor;</li>
 * <li>{@code schema.catalog.dirs}: further directories indexed by schema {@code id} (listing-json,
 * listing-api's vendored core-domain schemas);</li>
 * <li>{@code contract.publicMessages.dir}: the messages {@code PublicMessageRecorder} captured (a different
 * property from the recorder's own switch, {@code legacy.publicMessages.dir}).</li>
 * </ul>
 *
 * <p>Schema per name, as the framework resolved it: the descriptor's {@code schema_uri} looked up by
 * schema {@code id}, else {@code json/schema/<name>.json}. The payload is the envelope without
 * {@code _metadata}; draft-04 validation with the framework's everit library. A name with no legacy
 * schema, or that the legacy suite never made the app publish, is reported as skipped (aborted) with
 * the reason, not passed.
 */
class PublicMessageContractTest {

    /**
     * Schema violations the JEE app's payloads already had (same payload built the same way at commit
     * b932de14b; kept for parity, as the RAML response-schema defects of D20). Only these exact messages
     * are tolerated for these names; any other violation fails. Listed in docs/breaking.md.
     * <ul>
     * <li>offences-removed-from-allocated-hearing: the JEE processor copied every key of the private
     * event {@code listing.events.offences-removed-from-existing-allocated-hearing} except
     * {@code sourceContext}, so {@code isResultFlow} goes out although the public schema has
     * {@code additionalProperties: false} (NextHearingProcessor#handleOffencesRemovedFromExistingAllocatedHearingEvent).</li>
     * </ul>
     */
    private static final Map<String, List<String>> KNOWN_LEGACY_DEVIATIONS = Map.of(
            "public.events.listing.offences-removed-from-allocated-hearing",
            List.of("#: extraneous key [isResultFlow] is not permitted"));

    private static final Path CONTRACT_DIR = Path.of(required("public.contract.dir"));
    private static final Path MESSAGES_DIR = Path.of(required("contract.publicMessages.dir"));

    @TestFactory
    Stream<DynamicTest> everyPublishedPublicMessageIsValidAgainstItsLegacySchema() throws IOException {
        final SchemaCatalog catalog = new SchemaCatalog();
        catalog.index(CONTRACT_DIR.resolve("json/schema"));
        for (final String dir : System.getProperty("schema.catalog.dirs", "").split(java.io.File.pathSeparator)) {
            if (!dir.isBlank()) {
                catalog.index(Path.of(dir));
            }
        }
        catalog.index(CONTRACT_DIR.resolve("vendored-core-domain"));

        return publishedNames().entrySet().stream().map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
            final String name = entry.getKey();
            final byte[] schemaJson = schemaFor(catalog, name, entry.getValue());
            assumeTrue(schemaJson != null, "no legacy schema for " + name
                    + " (neither schema_uri " + entry.getValue() + " nor json/schema/" + name + ".json): the JEE app did not validate it either");
            final List<Path> samples = samples(name);
            assumeTrue(!samples.isEmpty(), "the legacy IT suite did not make the app publish " + name);

            final Schema schema = SchemaLoader.builder()
                    .schemaJson(new JSONObject(new JSONTokener(new ByteArrayInputStream(schemaJson))))
                    .httpClient(catalog)
                    .build().load().build();
            final List<String> failures = new ArrayList<>();
            for (final Path sample : samples) {
                final JSONObject payload = new JSONObject(Files.readString(sample));
                payload.remove("_metadata");
                try {
                    schema.validate(payload);
                } catch (final ValidationException e) {
                    final List<String> unexpected = e.getAllMessages().stream()
                            .filter(message -> !KNOWN_LEGACY_DEVIATIONS.getOrDefault(name, List.of()).contains(message))
                            .toList();
                    if (!unexpected.isEmpty()) {
                        failures.add(sample.getFileName() + ": " + String.join("; ", unexpected));
                    }
                }
            }
            if (!failures.isEmpty()) {
                fail(failures.size() + " of " + samples.size() + " recorded " + name + " payloads are not valid against the legacy schema:\n  "
                        + String.join("\n  ", failures.subList(0, Math.min(10, failures.size()))));
            }
            assertTrue(samples.size() > 0);
        }));
    }

    /** name → schema_uri, in descriptor order. */
    @SuppressWarnings("unchecked")
    private static Map<String, String> publishedNames() throws IOException {
        final Map<String, Object> yaml;
        try (InputStream in = Files.newInputStream(CONTRACT_DIR.resolve("public-publications-descriptor.yaml"))) {
            yaml = new Yaml().load(in);
        }
        final Map<String, String> names = new LinkedHashMap<>();
        final List<Map<String, Object>> subscriptions = (List<Map<String, Object>>)
                ((Map<String, Object>) yaml.get("subscriptions_descriptor")).get("subscriptions");
        for (final Map<String, Object> subscription : subscriptions) {
            for (final Map<String, Object> event : (List<Map<String, Object>>) subscription.get("events")) {
                names.put(((String) event.get("name")).trim(), (String) event.get("schema_uri"));
            }
        }
        return names;
    }

    private static byte[] schemaFor(final SchemaCatalog catalog, final String name, final String schemaUri) throws IOException {
        if (schemaUri != null && catalog.contains(schemaUri)) {
            return catalog.bytes(schemaUri);
        }
        final Path byName = CONTRACT_DIR.resolve("json/schema/" + name + ".json");
        return Files.exists(byName) ? Files.readAllBytes(byName) : null;
    }

    private static List<Path> samples(final String name) throws IOException {
        final Path dir = MESSAGES_DIR.resolve(name);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String required(final String property) {
        final String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("System property " + property + " is not set: run ./gradlew :listing-integration-test-legacy:publicMessageContractTest");
        }
        return value;
    }

    /** Schemas indexed by their draft-04 {@code id} (first one wins, as the framework catalog). */
    private static final class SchemaCatalog implements SchemaClient {

        private final Map<String, byte[]> byId = new LinkedHashMap<>();

        void index(final Path root) throws IOException {
            if (!Files.isDirectory(root)) {
                return;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (final Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                    final byte[] bytes = Files.readAllBytes(file);
                    final Object parsed;
                    try {
                        parsed = new JSONTokener(new ByteArrayInputStream(bytes)).nextValue();
                    } catch (final RuntimeException notJson) {
                        continue;
                    }
                    if (parsed instanceof JSONObject json) {
                        final String id = json.optString("id", json.optString("$id", ""));
                        if (!id.isBlank()) {
                            byId.putIfAbsent(normalise(id), bytes);
                        }
                    }
                }
            }
        }

        boolean contains(final String uri) {
            return byId.containsKey(normalise(uri));
        }

        byte[] bytes(final String uri) {
            return byId.get(normalise(uri));
        }

        @Override
        public InputStream get(final String url) {
            final byte[] bytes = byId.get(normalise(url));
            if (bytes == null) {
                throw new UncheckedIOException(new IOException("JSON schema " + url + " is not in the contract schema catalog"));
            }
            return new ByteArrayInputStream(bytes);
        }

        private static String normalise(final String uri) {
            final String trimmed = uri.trim();
            final int hash = trimmed.indexOf('#');
            return hash >= 0 ? trimmed.substring(0, hash) : trimmed;
        }
    }
}

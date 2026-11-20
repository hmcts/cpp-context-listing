package uk.gov.moj.cpp.listing.messaging.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * The subscription must select exactly what the legacy processor MDB selected: same names, same
 * selector string (Artemis keeps the filter on the durable subscription queue).
 */
public class PublicEventSubscriptionTest {

    private static final Path CONTRACT_DIR = Path.of(System.getProperty("listing.contractDir", "src/contract"));

    /**
     * Copied verbatim from the framework-generated
     * {@code listing-event-processor/target/generated-sources/uk/gov/justice/api/ListingEventProcessorPublicEventJmsListener.java}
     * (activation config {@code messageSelector}). That module was deleted at P2.6 (the generated
     * class was compared with this literal while it existed); this literal is now the record.
     */
    public static final String GENERATED_MDB_SELECTOR = "CPPNAME in('public.progression.defendant-offences-changed','public.progression.case-defendant-changed','public.progression.court-application-changed','public.progression.events.hearing-extended','public.progression.defendants-added-to-court-proceedings','public.progression.events.case-or-application-ejected','public.progression.case-markers-updated','public.progression.hearing-resulted-case-updated','public.progression.defendant-legalaid-status-updated','public.hearing.trial-vacated','public.progression.case-linked','public.progression.application-offences-updated','public.progression.application-laa-reference-updated-for-application','public.hearing.hearing-days-cancelled','public.hearing.defence-counsel-added','public.hearing.defence-counsel-updated','public.hearing.defence-counsel-removed','public.hearing.prosecution-counsel-added','public.hearing.prosecution-counsel-updated','public.hearing.prosecution-counsel-removed','public.events.hearing.marked-as-duplicate','public.progression.events.cps-prosecutor-updated','public.hearing.selected-offences-removed-from-existing-hearing','public.progression.offences-removed-from-existing-allocated-hearing','public.events.hearing.hearing-resulted','public.progression.case-removed-from-group-cases','public.progression.events.court-application-deleted','public.progression.related-hearing-updated-for-adhoc-hearing')";

    private final PublicEventSubscription subscription = new PublicEventSubscription();

    @Test
    void selectorIsTheGeneratedMdbSelector() {
        assertThat(subscription.selector()).isEqualTo(GENERATED_MDB_SELECTOR);
    }

    @Test
    @SuppressWarnings("unchecked")
    void namesAreThePublicEventSubscriptionOfTheLegacyDescriptorInOrder() throws IOException {
        final Path descriptor = CONTRACT_DIR.resolve("subscriptions-descriptor.yaml");
        final Map<String, Object> yaml;
        try (InputStream in = Files.newInputStream(descriptor)) {
            yaml = new Yaml().load(in);
        }
        final List<Map<String, Object>> subscriptions = (List<Map<String, Object>>)
                ((Map<String, Object>) yaml.get("subscriptions_descriptor")).get("subscriptions");
        final List<String> legacyNames = subscriptions.stream()
                .filter(s -> "public event subscription".equals(s.get("name")))
                .flatMap(s -> ((List<Map<String, Object>>) s.get("events")).stream())
                .map(event -> ((String) event.getOrDefault("name", event.get("name "))).trim())
                .toList();

        assertThat(subscription.eventNames()).hasSize(28).containsExactlyElementsOf(legacyNames);
        assertThat(subscription.subscribesTo("public.progression.case-linked")).isTrue();
        assertThat(subscription.subscribesTo("public.listing.hearing-confirmed")).isFalse();
    }
}

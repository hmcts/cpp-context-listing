package uk.gov.moj.cpp.listing.messaging.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * The subscription must select exactly what the legacy processor MDB selected: same names, same
 * selector string (Artemis keeps the filter on the durable subscription queue).
 */
public class PublicEventSubscriptionTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("listing.repoRoot", "../"));

    /**
     * Copied verbatim from the framework-generated
     * {@code listing-event-processor/target/generated-sources/uk/gov/justice/api/ListingEventProcessorPublicEventJmsListener.java}
     * (activation config {@code messageSelector}).
     */
    public static final String GENERATED_MDB_SELECTOR = "CPPNAME in('public.progression.defendant-offences-changed','public.progression.case-defendant-changed','public.progression.court-application-changed','public.progression.events.hearing-extended','public.progression.defendants-added-to-court-proceedings','public.progression.events.case-or-application-ejected','public.progression.case-markers-updated','public.progression.hearing-resulted-case-updated','public.progression.defendant-legalaid-status-updated','public.hearing.trial-vacated','public.progression.case-linked','public.progression.application-offences-updated','public.progression.application-laa-reference-updated-for-application','public.hearing.hearing-days-cancelled','public.hearing.defence-counsel-added','public.hearing.defence-counsel-updated','public.hearing.defence-counsel-removed','public.hearing.prosecution-counsel-added','public.hearing.prosecution-counsel-updated','public.hearing.prosecution-counsel-removed','public.events.hearing.marked-as-duplicate','public.progression.events.cps-prosecutor-updated','public.hearing.selected-offences-removed-from-existing-hearing','public.progression.offences-removed-from-existing-allocated-hearing','public.events.hearing.hearing-resulted','public.progression.case-removed-from-group-cases','public.progression.events.court-application-deleted','public.progression.related-hearing-updated-for-adhoc-hearing')";

    private final PublicEventSubscription subscription = new PublicEventSubscription();

    @Test
    void selectorIsTheGeneratedMdbSelector() {
        assertThat(subscription.selector()).isEqualTo(GENERATED_MDB_SELECTOR);
    }

    @Test
    void selectorStillMatchesTheGeneratedMdbWhenItHasBeenBuilt() throws IOException {
        final Path generated = REPO_ROOT.resolve("listing-event/listing-event-processor/target/generated-sources/uk/gov/justice/api/"
                + "ListingEventProcessorPublicEventJmsListener.java");
        if (!Files.exists(generated)) {
            return; // the Maven build has not generated it in this checkout; the literal above is the record
        }
        final Matcher selector = Pattern.compile("propertyName = \"messageSelector\", propertyValue = \"(.*?)\"\\)")
                .matcher(Files.readString(generated));
        assertThat(selector.find()).isTrue();
        assertThat(selector.group(1)).isEqualTo(subscription.selector());
        assertThat(Files.readString(generated)).contains("propertyValue = \"listing.event.processor.public.event\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void namesAreThePublicEventSubscriptionOfTheLegacyDescriptorInOrder() throws IOException {
        final Path descriptor = REPO_ROOT.resolve("listing-event/listing-event-processor/src/yaml/subscriptions-descriptor.yaml");
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

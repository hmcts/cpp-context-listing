package uk.gov.moj.cpp.listing.messaging.inbound;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * The listing processor's subscription to the framework's {@code public.event} topic (MBD refactor
 * P2.2, ADR 013): the 28 {@code public.*} events of the "public event subscription" in
 * {@code listing-event/listing-event-processor/src/yaml/subscriptions-descriptor.yaml}, in the same
 * order, and the JMS message selector the framework generated from them for
 * {@code ListingEventProcessorPublicEventJmsListener} ({@code CPPNAME in('…','…')}).
 *
 * <p>The selector string is kept byte-identical to the generated MDB's: Artemis stores the filter
 * on the durable subscription queue, and a consumer that joins an existing shared durable
 * subscription with a different filter would fail (or, with no consumers attached, recreate the
 * queue and lose its messages). A unit test compares both against the legacy sources.
 *
 * <p>(§4.3 of the refactor prompt says 26; the descriptor and the generated selector have 28.)
 */
@Component
public class PublicEventSubscription {

    /** Event names in descriptor order. */
    public static final List<String> EVENT_NAMES = List.of(
            "public.progression.defendant-offences-changed",
            "public.progression.case-defendant-changed",
            "public.progression.court-application-changed",
            "public.progression.events.hearing-extended",
            "public.progression.defendants-added-to-court-proceedings",
            "public.progression.events.case-or-application-ejected",
            "public.progression.case-markers-updated",
            "public.progression.hearing-resulted-case-updated",
            "public.progression.defendant-legalaid-status-updated",
            "public.hearing.trial-vacated",
            "public.progression.case-linked",
            "public.progression.application-offences-updated",
            "public.progression.application-laa-reference-updated-for-application",
            "public.hearing.hearing-days-cancelled",
            "public.hearing.defence-counsel-added",
            "public.hearing.defence-counsel-updated",
            "public.hearing.defence-counsel-removed",
            "public.hearing.prosecution-counsel-added",
            "public.hearing.prosecution-counsel-updated",
            "public.hearing.prosecution-counsel-removed",
            "public.events.hearing.marked-as-duplicate",
            "public.progression.events.cps-prosecutor-updated",
            "public.hearing.selected-offences-removed-from-existing-hearing",
            "public.progression.offences-removed-from-existing-allocated-hearing",
            "public.events.hearing.hearing-resulted",
            "public.progression.case-removed-from-group-cases",
            "public.progression.events.court-application-deleted",
            "public.progression.related-hearing-updated-for-adhoc-hearing");

    private static final String SELECTOR = EVENT_NAMES.stream()
            .map(name -> "'" + name + "'")
            .collect(Collectors.joining(",", "CPPNAME in(", ")"));

    public List<String> eventNames() {
        return EVENT_NAMES;
    }

    public boolean subscribesTo(final String eventName) {
        return EVENT_NAMES.contains(eventName);
    }

    /** {@code CPPNAME in('public.progression.defendant-offences-changed',…)}, as the framework generated it. */
    public String selector() {
        return SELECTOR;
    }
}

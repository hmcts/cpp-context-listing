package uk.gov.moj.cpp.listing.messaging.outbox;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

/**
 * Where the relay delivers an outbox entry (MBD refactor P2.2, ADR 013). Since ADR 015 the only
 * route is {@link PublicEventPublisher} ({@code public.*} names → the {@code public.event} topic):
 * the outbox carries public messages only, and there is no in-process (private event) route any
 * more. The relay picks the first route that {@link #handles} the name; an entry no route handles
 * is a failed attempt.
 */
public interface OutboxRoute {

    boolean handles(String eventName);

    /** Delivers the event; throws to make the relay count a failed attempt and retry. */
    void deliver(Envelope envelope);
}

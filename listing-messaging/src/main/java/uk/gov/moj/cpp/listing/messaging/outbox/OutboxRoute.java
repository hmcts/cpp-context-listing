package uk.gov.moj.cpp.listing.messaging.outbox;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

/**
 * Where the relay delivers an outbox entry (MBD refactor P2.2, ADR 013). Two routes exist:
 * {@link PublicEventPublisher} ({@code public.*} names → the {@code public.event} topic) and
 * {@link InProcessEventRoute} (names that have a {@link PrivateEventHandler} → that handler, in its
 * own transaction; for the P2.3 event-processor logic). The relay picks the first route that
 * {@link #handles} the name.
 */
public interface OutboxRoute {

    boolean handles(String eventName);

    /** Delivers the event; throws to make the relay count a failed attempt and retry. */
    void deliver(Envelope envelope);
}

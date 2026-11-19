package uk.gov.moj.cpp.listing.messaging.inbound;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.Set;

/**
 * Handles inbound {@code public.*} events from the {@code public.event} topic (MBD refactor P2.2,
 * ADR 013). P2.4 adds one per legacy processor handler (progression ×16, hearing ×9,
 * {@code public.events.hearing.*} ×2, plus the court-application-deleted handler).
 *
 * <p>{@link #handle} runs inside the listener's database transaction, after the event id was
 * recorded in {@code inbound_event_dedup}; it may write the viewstore and the outbox
 * ({@code EventOutbox}). Throwing rolls everything back, including the dedup row, and the broker
 * redelivers the message (to the DLQ after its max delivery attempts).
 *
 * <p>Every handled name must be one the subscription selects ({@link PublicEventSubscription});
 * each name has at most one handler.
 */
public interface InboundEventHandler {

    Set<String> handledEvents();

    void handle(Envelope event);
}

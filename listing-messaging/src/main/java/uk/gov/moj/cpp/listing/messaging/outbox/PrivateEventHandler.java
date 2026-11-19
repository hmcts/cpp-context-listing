package uk.gov.moj.cpp.listing.messaging.outbox;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.Set;

/**
 * An in-process consumer of outbox events (MBD refactor P2.2, ADR 013): the extension point for
 * P2.3, where the event-processor logic that today runs after a private {@code listing.events.*}
 * event becomes Spring components. A service writes the private event to the outbox in its own
 * transaction; the relay calls the handler after commit, in a new transaction, retrying up to
 * {@code listing.messaging.outbox.relay.max-attempts} times (at-least-once: handlers must tolerate
 * a redelivered event).
 *
 * <p>Each event name may have at most one handler. No handler exists yet (P2.2 ports no processor
 * logic).
 */
public interface PrivateEventHandler {

    Set<String> handledEvents();

    void handle(Envelope event);
}

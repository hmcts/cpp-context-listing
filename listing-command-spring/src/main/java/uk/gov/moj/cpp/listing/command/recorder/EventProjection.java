package uk.gov.moj.cpp.listing.command.recorder;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.Set;

/**
 * A ported event-listener handler (MBD refactor P2.3, ADR 014): the viewstore projection that the
 * framework's {@code EVENT_LISTENER} ran for a private {@code listing.events.*} event. Each slice
 * ports its {@code listing-event-listener} handlers as framework-free classes implementing this
 * interface (payload parsed from the envelope's {@code JsonObject}, viewstore writes through the
 * Spring Data repositories).
 *
 * <p>{@link EventRecorder} calls {@link #project} synchronously, in the command's transaction, for
 * every recorded event whose name is in {@link #handledEvents()} (as ADR 011 did in-VM in Phase 1):
 * a projection failure rolls the command back. Each event name has at most one projection, as the
 * listener had at most one {@code @Handles} method per name ({@link EventProjections}).
 */
public interface EventProjection {

    /** The private event names ({@code listing.events.*}) this projection handles. */
    Set<String> handledEvents();

    void project(Envelope event);
}

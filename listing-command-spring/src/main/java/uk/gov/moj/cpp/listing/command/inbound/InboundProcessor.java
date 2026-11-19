package uk.gov.moj.cpp.listing.command.inbound;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.inbound.InboundEventHandler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base of the ported JEE event-processor classes that handle inbound {@code public.*} events
 * (MBD refactor P2.4, ADR 013 and ADR 015): each subclass registers its JEE {@code @Handles}
 * methods by event name, and {@code InboundEventDispatcher} calls {@link #handle} in the
 * listener's transaction, after the dedup row was written.
 *
 * <p>The internal commands a handler sends ({@code ProcessorSender} → the P2.3 command services)
 * run directly, in that transaction. The whole handler runs as one {@link ProcessorQueue} command:
 * the decisions of every command it sends run first, then their processor steps in raised order,
 * as the Phase 1 relay delivered them (JEE P1g: the inbound handler's in-process commands appended
 * their events to the outbox, the relay processed them afterwards).
 */
public abstract class InboundProcessor implements InboundEventHandler {

    private final ProcessorQueue processorQueue;
    private final Map<String, Consumer<Envelope>> handlers = new LinkedHashMap<>();

    protected InboundProcessor(final ProcessorQueue processorQueue) {
        this.processorQueue = processorQueue;
    }

    /** Registers the handler of one subscribed event ({@code @Handles(name)}). */
    protected final void on(final String eventName, final Consumer<Envelope> handler) {
        if (handlers.putIfAbsent(eventName, handler) != null) {
            throw new IllegalStateException(getClass().getName() + " registers " + eventName + " twice");
        }
    }

    @Override
    public final Set<String> handledEvents() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    @Override
    public final void handle(final Envelope event) {
        final Consumer<Envelope> handler = handlers.get(event.name());
        if (handler == null) {
            throw new IllegalArgumentException(getClass().getName() + " does not handle " + event.name());
        }
        processorQueue.command(() -> handler.accept(event));
    }
}

package uk.gov.moj.cpp.listing.command.recorder;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;
import uk.gov.moj.cpp.listing.messaging.outbox.InProcessEventRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the private events a command raised (MBD refactor ADR 011, Spring port in P2.3.0,
 * ADR 014). Replaces {@code EventStream.append(...)} and the Phase 1 JEE {@code EventRecorder}. For
 * each event, in the command's transaction ({@code MANDATORY}):
 * <ol>
 *     <li>builds its envelope with the framework metadata convention ({@link EventOutbox#envelopeFor}:
 *     the command's metadata, a new id, the event name, {@code createdAt}, the command id appended
 *     to {@code causation}, {@code stream.id});</li>
 *     <li>runs the ported listener projection registered for the name ({@link EventProjections}),
 *     so the viewstore changes commit with the command;</li>
 *     <li>appends the event to the outbox if a ported processor handler subscribes to it (a
 *     {@code PrivateEventHandler}, routed by {@link InProcessEventRoute}); the relay hands it to
 *     the handler after commit, in the handler's own transaction (ADR 013).</li>
 * </ol>
 * An event with neither is recorded nowhere, as an event outside both subscriptions was in Phase 1
 * (only the aggregate cared about it). Nothing is published on JMS from here: public events are
 * written by the processor handlers.
 */
@Component
@Transactional(propagation = MANDATORY)
public class EventRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRecorder.class);

    private final EventOutbox eventOutbox;
    private final EventProjections projections;
    private final InProcessEventRoute processors;

    public EventRecorder(final EventOutbox eventOutbox, final EventProjections projections, final InProcessEventRoute processors) {
        this.eventOutbox = eventOutbox;
        this.projections = projections;
        this.processors = processors;
    }

    /**
     * Records events raised by an aggregate (payload POJOs annotated with {@link Event}).
     *
     * @param cause    the command's metadata
     * @param streamId the aggregate id; becomes {@code _metadata.stream.id}
     */
    public List<Envelope> recordEvents(final Metadata cause, final UUID streamId, final List<Object> events) {
        return record(cause, events.stream().map(event -> new DomainEvent(nameOf(event), streamId, event)).toList());
    }

    /** Records events returned by a framework-free domain service. */
    public List<Envelope> record(final Metadata cause, final List<DomainEvent> events) {
        final List<Envelope> recorded = new ArrayList<>(events.size());
        for (final DomainEvent event : events) {
            final Envelope envelope = eventOutbox.envelopeFor(cause, event);
            final String name = envelope.name();
            final boolean projected = projections.forEvent(name).map(projection -> {
                projection.project(envelope);
                return true;
            }).orElse(false);
            final boolean processed = processors.handles(name);
            if (processed) {
                eventOutbox.append(envelope);
            }
            if (!projected && !processed) {
                LOGGER.debug("Event {} ({}) has no listener projection and no processor handler", name, envelope.metadata().id());
            }
            recorded.add(envelope);
        }
        return recorded;
    }

    /** The private event name of a generated event POJO (its {@link Event} annotation). */
    public static String nameOf(final Object event) {
        final Event annotation = event.getClass().getAnnotation(Event.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Event POJO " + event.getClass().getName() + " has no @Event annotation");
        }
        return annotation.value();
    }
}

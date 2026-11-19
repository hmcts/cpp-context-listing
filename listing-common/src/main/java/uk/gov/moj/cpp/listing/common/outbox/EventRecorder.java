package uk.gov.moj.cpp.listing.common.outbox;

import static java.util.stream.Collectors.toSet;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.core.annotation.ServiceComponentLocation;
import uk.gov.justice.services.core.dispatcher.Dispatcher;
import uk.gov.justice.services.core.dispatcher.DispatcherCache;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.subscription.domain.subscriptiondescriptor.Subscription;
import uk.gov.justice.subscription.registry.SubscriptionsDescriptorsRegistry;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Replaces {@code EventStream.append(...)} for migrated aggregates (MBD refactor Phase 1,
 * ADR 011). For each event, inside the caller's transaction:
 * <ol>
 *     <li>runs the event listener's handlers in-VM through the framework's {@code EVENT_LISTENER}
 *     dispatcher, so the viewstore is updated in the same transaction as the command. The
 *     dispatcher is called directly, not through the {@code EVENT_LISTENER} interceptor chain: that
 *     chain contains {@code SubscriptionEventInterceptor}, which tracks event-store event numbers
 *     ({@code processed_event}) and rejects events that have none ("Missing previous event
 *     number"). The other interceptors skipped are metrics, debug logging and feature control;
 *     the subscription filter is applied here instead;</li>
 *     <li>appends the event to {@code event_outbox} if the event processor subscribes to it, so
 *     {@link EventOutboxRelay} hands it to the processor after commit.</li>
 * </ol>
 * Nothing is written to the event store and nothing is published on {@code listing.event}.
 */
@ApplicationScoped
public class EventRecorder {

    static final String EVENT_LISTENER = "EVENT_LISTENER";
    static final String EVENT_LISTENER_SUBSCRIPTION = "event listener subscription";
    static final String EVENT_PROCESSOR_SUBSCRIPTION = EventProcessorInvoker.EVENT_PROCESSOR_SUBSCRIPTION;

    @Inject
    private DispatcherCache dispatcherCache;

    @Inject
    private SubscriptionsDescriptorsRegistry subscriptionsDescriptorsRegistry;

    @Inject
    private EventOutboxWriter eventOutboxWriter;

    private Dispatcher eventListener;
    private Set<String> listenerEvents;
    private Set<String> processorEvents;

    @PostConstruct
    void initialise() {
        eventListener = dispatcherCache.dispatcherFor(EVENT_LISTENER, ServiceComponentLocation.LOCAL);
        listenerEvents = eventNames(EVENT_LISTENER_SUBSCRIPTION);
        processorEvents = eventNames(EVENT_PROCESSOR_SUBSCRIPTION);
    }

    /**
     * Records events raised by a command on one stream (aggregate id).
     *
     * @param command  the command that caused the events (metadata source)
     * @param streamId the aggregate / stream id; becomes {@code _metadata.stream.id}
     * @param events   event payload POJOs annotated with {@link Event}
     */
    public void record(final Envelope<?> command, final UUID streamId, final List<Object> events) {
        final Metadata causedBy = command.metadata();
        for (final Object event : events) {
            final JsonEnvelope envelope = eventOutboxWriter.envelopeFor(causedBy, new DomainEvent(nameOf(event), streamId, event));
            final String name = envelope.metadata().name();

            if (listenerEvents.contains(name)) {
                eventListener.dispatch(envelope);
            }
            if (processorEvents.contains(name)) {
                eventOutboxWriter.append(envelope);
            }
        }
    }

    static String nameOf(final Object event) {
        final Event annotation = event.getClass().getAnnotation(Event.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Event POJO " + event.getClass().getName() + " has no @Event annotation");
        }
        return annotation.value();
    }

    private Set<String> eventNames(final String subscriptionName) {
        final Subscription subscription = subscriptionsDescriptorsRegistry.getSubscriptionFor(subscriptionName);
        return subscription.getEvents().stream()
                .map(uk.gov.justice.subscription.domain.subscriptiondescriptor.Event::getName)
                .collect(toSet());
    }
}

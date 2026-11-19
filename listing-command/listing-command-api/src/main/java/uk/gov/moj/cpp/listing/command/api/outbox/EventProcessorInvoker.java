package uk.gov.moj.cpp.listing.command.api.outbox;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.subscription.SubscriptionManager;
import uk.gov.justice.services.subscription.annotation.SubscriptionName;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;

/**
 * Hands an outbox event to the event processor in-VM (MBD refactor Phase 1, ADR 2).
 *
 * <p>Uses the same {@link SubscriptionManager} bean that the processor's generated
 * {@code listing.event} MDB calls, so the processor's interceptor chain and handlers run exactly
 * as for a JMS delivery. The event is not put on {@code listing.event}: the event listener's MDB
 * has no message selector and would reject it ("No position found in event") into the DLQ.
 *
 * <p>Runs in its own transaction, as the MDB's container-managed delivery did; a failure rolls
 * back only the processor's work for this event.
 */
@Stateless
public class EventProcessorInvoker {

    static final String EVENT_PROCESSOR_SUBSCRIPTION = "event processor subscription";

    @Inject
    @SubscriptionName(EVENT_PROCESSOR_SUBSCRIPTION)
    private SubscriptionManager eventProcessorSubscription;

    @TransactionAttribute(REQUIRES_NEW)
    public void process(final JsonEnvelope event) {
        eventProcessorSubscription.process(event);
    }
}

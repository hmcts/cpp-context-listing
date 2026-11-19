package uk.gov.moj.cpp.listing.messaging.outbox;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Routes outbox events to their {@link PrivateEventHandler} in-process (MBD refactor P2.2,
 * ADR 013). The handler runs in a {@code REQUIRES_NEW} transaction, as the Phase 1
 * {@code EventProcessorInvoker} did: a handler failure rolls back only the handler's work, never
 * the relay's transaction, which must still record the failed attempt (§11 pitfall 2).
 */
@Component
@Order(2)
public class InProcessEventRoute implements OutboxRoute {

    private final Map<String, PrivateEventHandler> handlers = new HashMap<>();
    private final TransactionTemplate requiresNew;

    public InProcessEventRoute(final List<PrivateEventHandler> handlers, final PlatformTransactionManager transactionManager) {
        for (final PrivateEventHandler handler : handlers) {
            for (final String name : handler.handledEvents()) {
                final PrivateEventHandler previous = this.handlers.putIfAbsent(name, handler);
                if (previous != null) {
                    throw new IllegalStateException("Two private event handlers for " + name + ": "
                            + previous.getClass().getName() + " and " + handler.getClass().getName());
                }
            }
        }
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public boolean handles(final String eventName) {
        return handlers.containsKey(eventName);
    }

    @Override
    public void deliver(final Envelope envelope) {
        final PrivateEventHandler handler = handlers.get(envelope.name());
        requiresNew.executeWithoutResult(status -> handler.handle(envelope));
    }
}

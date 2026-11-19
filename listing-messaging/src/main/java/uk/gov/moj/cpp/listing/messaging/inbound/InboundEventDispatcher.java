package uk.gov.moj.cpp.listing.messaging.inbound;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Dispatches an inbound envelope to the {@link InboundEventHandler} registered for its
 * {@code _metadata.name} (MBD refactor P2.2, ADR 013), replacing the framework's
 * {@code SubscriptionManager} → interceptor chain → {@code @Handles} dispatcher.
 *
 * <p>The registry is checked at start-up: a handler for a name the subscription does not select,
 * or two handlers for one name, fail the application context.
 */
@Component
public class InboundEventDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundEventDispatcher.class);

    private final Map<String, InboundEventHandler> handlers = new HashMap<>();

    public InboundEventDispatcher(final List<InboundEventHandler> handlers, final PublicEventSubscription subscription) {
        for (final InboundEventHandler handler : handlers) {
            for (final String name : handler.handledEvents()) {
                if (!subscription.subscribesTo(name)) {
                    throw new IllegalStateException(handler.getClass().getName() + " handles " + name
                            + ", which the public.event subscription does not select");
                }
                final InboundEventHandler previous = this.handlers.putIfAbsent(name, handler);
                if (previous != null) {
                    throw new IllegalStateException("Two inbound event handlers for " + name + ": "
                            + previous.getClass().getName() + " and " + handler.getClass().getName());
                }
            }
        }
    }

    /**
     * @return {@code true} if a handler ran; {@code false} if none is registered for the name
     *         (logged; the message is consumed, as there is nothing that could handle it later)
     */
    public boolean dispatch(final Envelope envelope) {
        final InboundEventHandler handler = handlers.get(envelope.name());
        if (handler == null) {
            LOGGER.warn("No handler for inbound event {} {}; consumed without action", envelope.name(), envelope.metadata().id());
            return false;
        }
        handler.handle(envelope);
        return true;
    }

    public boolean hasHandlerFor(final String eventName) {
        return handlers.containsKey(eventName);
    }
}

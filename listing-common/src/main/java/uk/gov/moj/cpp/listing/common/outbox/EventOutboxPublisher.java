package uk.gov.moj.cpp.listing.common.outbox;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository.OutboxEntry;

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers the oldest {@code event_outbox} entry to the event processor
 * (MBD refactor Phase 1, ADR 2).
 *
 * <p>One event per call, in {@code id} order, under the relay's advisory lock (one deliverer
 * across all nodes). The processor runs in its own transaction ({@link EventProcessorInvoker}).
 * On success the entry is deleted. On failure the attempt is counted and the entry retried on the
 * next call, like JMS redelivery; after {@link #MAX_ATTEMPTS} it is logged at ERROR and dropped,
 * which is where JMS would have sent it to the DLQ. Delivery is at-least-once: if this transaction
 * fails after the processor's transaction committed, the event is delivered again.
 */
@Stateless
public class EventOutboxPublisher {

    static final int MAX_ATTEMPTS = Integer.getInteger("listing.outbox.relay.maxAttempts", 10);

    private static final Logger LOGGER = LoggerFactory.getLogger(EventOutboxPublisher.class);

    @Inject
    private EventOutboxJdbcRepository eventOutboxJdbcRepository;

    @Inject
    private EventProcessorInvoker eventProcessorInvoker;

    @Inject
    private JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter;

    /**
     * @return {@code true} if an entry was handled (delivered, retried or dropped), so the caller
     *         should call again; {@code false} if the outbox is empty or another relay holds the lock
     */
    @TransactionAttribute(REQUIRES_NEW)
    public boolean deliverNext() {
        if (!eventOutboxJdbcRepository.tryLockForRelay()) {
            return false;
        }

        final List<OutboxEntry> next = eventOutboxJdbcRepository.nextBatch(1);
        if (next.isEmpty()) {
            return false;
        }

        final OutboxEntry entry = next.get(0);
        final JsonEnvelope event = jsonObjectEnvelopeConverter.asEnvelope(entry.envelopeJson());
        try {
            eventProcessorInvoker.process(event);
            eventOutboxJdbcRepository.delete(List.of(entry.id()));
        } catch (final RuntimeException e) {
            handleFailure(entry, event, e);
        }
        return true;
    }

    private void handleFailure(final OutboxEntry entry, final JsonEnvelope event, final RuntimeException e) {
        final int attempt = entry.attempts() + 1;
        if (attempt >= MAX_ATTEMPTS) {
            LOGGER.error("Event processor failed on outbox event {} '{}' {} times; dropping it",
                    event.metadata().id(), event.metadata().name(), attempt, e);
            eventOutboxJdbcRepository.delete(List.of(entry.id()));
        } else {
            LOGGER.warn("Event processor failed on outbox event {} '{}' (attempt {} of {}); will retry",
                    event.metadata().id(), event.metadata().name(), attempt, MAX_ATTEMPTS, e);
            eventOutboxJdbcRepository.incrementAttempts(entry.id());
        }
    }
}

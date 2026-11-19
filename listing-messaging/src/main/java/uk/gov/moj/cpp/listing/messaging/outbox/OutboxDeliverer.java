package uk.gov.moj.cpp.listing.messaging.outbox;

import uk.gov.moj.cpp.listing.messaging.config.MessagingProperties;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository.OutboxEntry;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Delivers the oldest {@code event_outbox} entry (MBD refactor P2.2; ADR 002, ADR 013). The Spring
 * port of the Phase 1 {@code EventOutboxPublisher}, with the same semantics:
 * <ul>
 *     <li>one entry per transaction, in {@code id} order, under a transaction-scoped Postgres
 *     advisory lock (one deliverer across all nodes; the same lock key as Phase 1);</li>
 *     <li>on success the entry is deleted by its exact id (a lower id committing late is kept);</li>
 *     <li>on failure the attempt is counted and the entry retried by a later run; after
 *     {@code max-attempts} it is logged at ERROR and dropped (where JMS would use the DLQ);</li>
 *     <li>at-least-once: if this transaction fails after delivery, the entry is delivered again.</li>
 * </ul>
 * The delivery's exception is caught here, inside this class's own transaction, and never passes
 * through a transactional proxy: the transaction stays committable, so the attempt count is
 * recorded (§11 pitfall 2). In-process handlers run in their own transaction
 * ({@link InProcessEventRoute}).
 */
@Component
public class OutboxDeliverer {

    /** What one call did. */
    public enum Result {
        /** An entry was delivered and deleted: call again. */
        DELIVERED,
        /** An entry failed (attempt counted, or dropped after the last attempt): retry on the next run. */
        FAILED,
        /** Nothing to do: the outbox is empty or another node holds the relay lock. */
        IDLE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDeliverer.class);

    private final EventOutboxJdbcRepository repository;
    private final EnvelopeCodec codec;
    private final List<OutboxRoute> routes;
    private final TransactionTemplate transaction;
    private final int maxAttempts;

    public OutboxDeliverer(final EventOutboxJdbcRepository repository, final EnvelopeCodec codec, final List<OutboxRoute> routes,
                           final PlatformTransactionManager transactionManager, final MessagingProperties properties) {
        this.repository = repository;
        this.codec = codec;
        this.routes = List.copyOf(routes);
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.maxAttempts = properties.getOutbox().getRelay().getMaxAttempts();
    }

    public Result deliverNext() {
        return transaction.execute(status -> deliverNextInTransaction());
    }

    private Result deliverNextInTransaction() {
        if (!repository.tryLockForRelay()) {
            return Result.IDLE;
        }
        final List<OutboxEntry> next = repository.nextBatch(1);
        if (next.isEmpty()) {
            return Result.IDLE;
        }
        final OutboxEntry entry = next.get(0);
        try {
            final Envelope envelope = codec.parse(entry.envelopeJson());
            routeFor(envelope.name()).deliver(envelope);
        } catch (final RuntimeException e) {
            handleFailure(entry, e);
            return Result.FAILED;
        }
        repository.delete(List.of(entry.id()));
        return Result.DELIVERED;
    }

    private OutboxRoute routeFor(final String eventName) {
        return routes.stream()
                .filter(route -> route.handles(eventName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No outbox route (JMS topic or in-process handler) for event " + eventName));
    }

    private void handleFailure(final OutboxEntry entry, final RuntimeException e) {
        final int attempt = entry.attempts() + 1;
        if (attempt >= maxAttempts) {
            LOGGER.error("Outbox delivery of entry {} '{}' failed {} times; dropping it", entry.id(), entry.eventName(), attempt, e);
            repository.delete(List.of(entry.id()));
        } else {
            LOGGER.warn("Outbox delivery of entry {} '{}' failed (attempt {} of {}); will retry",
                    entry.id(), entry.eventName(), attempt, maxAttempts, e);
            repository.incrementAttempts(entry.id());
        }
    }
}

package uk.gov.moj.cpp.listing.messaging.inbound;

import uk.gov.moj.cpp.listing.messaging.config.MessagingProperties;
import uk.gov.moj.cpp.listing.persistence.repository.inbound.InboundEventDedupJdbcRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Purges {@code inbound_event_dedup} rows older than {@code listing.messaging.inbound.dedup.retention}
 * (default 30 days; D26, §15.3, ADR 013).
 *
 * <p>Runs on {@code listing.messaging.inbound.dedup.purge.cron} (default daily). Each batch is its
 * own transaction: it takes a transaction-scoped Postgres advisory lock (as the outbox relay does)
 * and deletes at most {@code batch-size} of the oldest expired rows. If another node holds the lock
 * the run stops, so only one node purges at a time; a run ends when a batch deletes fewer rows than
 * the batch size. The cutoff is fixed at the start of the run.
 *
 * <p>Active only when {@code listing.messaging.enabled} and
 * {@code listing.messaging.inbound.dedup.purge.enabled} are both true.
 */
@Component
@ConditionalOnExpression("${listing.messaging.enabled:true} and ${listing.messaging.inbound.dedup.purge.enabled:true}")
public class InboundDedupPurge {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundDedupPurge.class);

    private final InboundEventDedupJdbcRepository repository;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final Duration retention;
    private final int batchSize;

    public InboundDedupPurge(final InboundEventDedupJdbcRepository repository, final PlatformTransactionManager transactionManager,
                             final Clock clock, final MessagingProperties properties) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
        final MessagingProperties.Dedup dedup = properties.getInbound().getDedup();
        this.retention = dedup.getRetention();
        this.batchSize = dedup.getPurge().getBatchSize();
        if (retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("listing.messaging.inbound.dedup.retention must be positive, was " + retention);
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("listing.messaging.inbound.dedup.purge.batch-size must be at least 1, was " + batchSize);
        }
    }

    @Scheduled(cron = "${listing.messaging.inbound.dedup.purge.cron:0 30 2 * * *}")
    public void purge() {
        final Instant cutoff = clock.instant().minus(retention);
        long total = 0;
        try {
            int deleted;
            do {
                final Integer batch = transaction.execute(status -> repository.tryLockForPurge()
                        ? repository.deleteReceivedBefore(cutoff, batchSize)
                        : null);
                if (batch == null) {
                    LOGGER.info("Inbound dedup purge skipped: another node holds the purge lock");
                    return;
                }
                deleted = batch;
                total += deleted;
            } while (deleted >= batchSize);
            LOGGER.info("Inbound dedup purge deleted {} rows received before {}", total, cutoff);
        } catch (final RuntimeException e) {
            // Never let an exception escape the scheduled method; the next run carries on.
            LOGGER.error("Inbound dedup purge failed after deleting {} rows; will retry on the next run", total, e);
        }
    }
}

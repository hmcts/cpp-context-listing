package uk.gov.moj.cpp.listing.messaging.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the transactional outbox relay (MBD refactor P2.2; ADR 002, ADR 013), replacing the
 * Phase 1 EJB {@code EventOutboxRelay}.
 *
 * <p>A fixed-delay schedule ({@code listing.messaging.outbox.relay.delay-millis}, default 25 ms),
 * so runs never overlap. Each run delivers entries one by one until the outbox is empty, another
 * node holds the lock, or an entry fails; a failed entry is retried on the next run (Phase 1
 * retried it immediately in the same run; waiting one delay is the only behavioural difference).
 * Ordering and the single deliverer come from the advisory lock in {@link OutboxDeliverer}.
 *
 * <p>Active only when {@code listing.messaging.enabled} and
 * {@code listing.messaging.outbox.relay.enabled} are both true.
 */
@Component
@ConditionalOnExpression("${listing.messaging.enabled:true} and ${listing.messaging.outbox.relay.enabled:true}")
public class OutboxRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxDeliverer deliverer;

    public OutboxRelay(final OutboxDeliverer deliverer) {
        this.deliverer = deliverer;
    }

    @Scheduled(fixedDelayString = "${listing.messaging.outbox.relay.delay-millis:25}",
            initialDelayString = "${listing.messaging.outbox.relay.delay-millis:25}")
    public void drain() {
        try {
            OutboxDeliverer.Result result;
            do {
                result = deliverer.deliverNext();
            } while (result == OutboxDeliverer.Result.DELIVERED);
        } catch (final RuntimeException e) {
            // Never let an exception escape the scheduled method: log, and the entry is retried
            // on the next run (its transaction rolled back).
            LOGGER.error("Outbox relay run failed; the entry was rolled back and will be retried", e);
        }
    }
}

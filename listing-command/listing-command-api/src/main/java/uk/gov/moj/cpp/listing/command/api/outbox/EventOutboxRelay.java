package uk.gov.moj.cpp.listing.command.api.outbox;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the transactional outbox relay (MBD refactor Phase 1, ADR 2).
 *
 * <p>Polls {@code event_outbox} with a fixed delay, so runs never overlap (unlike an EJB interval
 * timer). Each run delivers events one by one until the outbox is empty. Ordering and
 * single-deliverer behaviour across nodes come from the advisory lock in
 * {@link EventOutboxPublisher}.
 *
 * <p>System properties: {@code listing.outbox.relay.delayMillis} (default 25) and
 * {@code listing.outbox.relay.maxAttempts} (default 10).
 */
@Singleton
@Startup
public class EventOutboxRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventOutboxRelay.class);

    private static final long DELAY_MILLIS = Long.getLong("listing.outbox.relay.delayMillis", 25L);

    @Resource
    private ManagedScheduledExecutorService scheduler;

    @Inject
    private EventOutboxPublisher eventOutboxPublisher;

    private ScheduledFuture<?> relay;

    @PostConstruct
    void start() {
        relay = scheduler.scheduleWithFixedDelay(this::drain, DELAY_MILLIS, DELAY_MILLIS, MILLISECONDS);
        LOGGER.info("Event outbox relay started (delay {}ms, max attempts {})", DELAY_MILLIS, EventOutboxPublisher.MAX_ATTEMPTS);
    }

    @PreDestroy
    void stop() {
        if (relay != null) {
            relay.cancel(false);
        }
    }

    void drain() {
        try {
            boolean delivered;
            do {
                delivered = eventOutboxPublisher.deliverNext();
            } while (delivered);
        } catch (final RuntimeException e) {
            // Never let an exception escape: it would cancel the scheduled task for good.
            LOGGER.error("Event outbox relay run failed; the entry was rolled back and will be retried", e);
        }
    }
}

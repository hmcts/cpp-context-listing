package uk.gov.moj.cpp.listing.command.support;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs a follow-up step after the current transaction commits, on an executor (MBD refactor
 * P2.3.1, ADR 015; §5 "use @Async where the original was asynchronous"). It replaces the JEE hop
 * "command → private event → (after commit, from the event topic / outbox relay) event processor"
 * for processor steps that were asynchronous, e.g. the XHIBIT court-list export: the follow-up
 * never sees uncommitted data, a rolled-back command never triggers it, and the HTTP request
 * returns (202) without waiting for it.
 *
 * <p>A failing step is retried, up to {@value #DEFAULT_MAX_ATTEMPTS} attempts in all, at once, as
 * the Phase 1 JEE outbox relay retried a processor step (P2.3.2, decided follow-up of P2.3.1); each
 * attempt must run in its own transaction (the court-list steps use {@code REQUIRES_NEW}), so a
 * failed attempt leaves nothing behind. After the last attempt it is logged at ERROR and dropped,
 * as the relay dropped it. Unlike the JEE path the hand-off is not durable: if the application stops
 * between the commit and the follow-up, the step is lost (the next publish is the retry, as for a
 * failed XHIBIT export). See ADR 015, implementation notes.
 */
public class AfterCommit {

    private static final Logger LOGGER = LoggerFactory.getLogger(AfterCommit.class);

    /** The JEE relay's attempts per event (P1.0, D27). */
    public static final int DEFAULT_MAX_ATTEMPTS = 10;

    private final Executor executor;
    private final int maxAttempts;

    public AfterCommit(final Executor executor) {
        this(executor, DEFAULT_MAX_ATTEMPTS);
    }

    public AfterCommit(final Executor executor, final int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.executor = executor;
        this.maxAttempts = maxAttempts;
    }

    /** Schedules {@code step} for after the commit of the current transaction (now, if there is none). */
    public void run(final String description, final Runnable step) {
        final Runnable guarded = () -> runWithRetries(description, step);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(guarded);
                }
            });
        } else {
            executor.execute(guarded);
        }
    }

    private void runWithRetries(final String description, final Runnable step) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                step.run();
                return;
            } catch (final RuntimeException e) {
                if (attempt < maxAttempts) {
                    LOGGER.warn("Asynchronous step '{}' failed (attempt {} of {}); retrying", description, attempt, maxAttempts, e);
                } else {
                    LOGGER.error("Asynchronous step '{}' failed (attempt {} of {}); dropped", description, attempt, maxAttempts, e);
                }
            }
        }
    }
}

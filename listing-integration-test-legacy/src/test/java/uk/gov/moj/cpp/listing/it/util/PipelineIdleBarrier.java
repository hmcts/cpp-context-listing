package uk.gov.moj.cpp.listing.it.util;

import static java.lang.System.currentTimeMillis;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

/**
 * Happens-after barrier for assertions that something did NOT happen.
 *
 * <p>Returns once listing's whole async pipeline has been observed empty {@value #CONFIRMATIONS}
 * times in a row: both listing command queues, the service's own subscriptions on the listing and
 * public event topics ({@code MessageCount}, so routed-but-undispatched messages count too) and the
 * event-store publish relay tables. Work still in flight always sits in one of those, and each hop
 * hands it to the next inside the same transaction, so "all empty, repeatedly" means nothing more
 * can be emitted. The repeat closes the few-millisecond gap between a JMS ack and its paired DB
 * commit.</p>
 *
 * <p>Fails instead of returning when idleness cannot be established (pipeline still busy after
 * {@value #MAX_WAIT_MILLIS}ms, or the broker counts are unreadable): an absence check made without a
 * barrier proves nothing.</p>
 */
public final class PipelineIdleBarrier {

    private static final long MAX_WAIT_MILLIS = 30_000;
    private static final long POLL_INTERVAL_MILLIS = 50;
    private static final int CONFIRMATIONS = 3;

    private PipelineIdleBarrier() {
    }

    public static void awaitPipelineIdle(final DatabaseCleaner databaseCleaner, final String contextName) {
        final long deadline = currentTimeMillis() + MAX_WAIT_MILLIS;
        int consecutiveIdle = 0;
        int consecutiveUnreadable = 0;
        long lastInFlight = 0;
        while (currentTimeMillis() < deadline) {
            final long brokerCount = ArtemisQueuePurger.pipelineMessageCount();
            if (brokerCount < 0) {
                if (++consecutiveUnreadable >= CONFIRMATIONS) {
                    throw new IllegalStateException("Pipeline idle barrier: Artemis queue counts unreadable — cannot prove absence");
                }
                consecutiveIdle = 0;
            } else {
                consecutiveUnreadable = 0;
                lastInFlight = brokerCount + databaseCleaner.publishQueueDepth(contextName);
                consecutiveIdle = lastInFlight == 0 ? consecutiveIdle + 1 : 0;
                if (consecutiveIdle >= CONFIRMATIONS) {
                    return;
                }
            }
            sleep();
        }
        throw new IllegalStateException("Pipeline idle barrier: " + lastInFlight
                + " message(s)/relay row(s) still in flight after " + MAX_WAIT_MILLIS + "ms — cannot prove absence");
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the pipeline to go idle", e);
        }
    }
}

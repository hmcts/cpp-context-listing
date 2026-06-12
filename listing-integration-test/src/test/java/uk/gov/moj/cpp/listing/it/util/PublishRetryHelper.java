package uk.gov.moj.cpp.listing.it.util;

import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.RETRY_ATTEMPT_TIMEOUT_IN_MILLIS;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.TIMEOUT_IN_MILLIS;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.withRetryAttemptPollBudget;

import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;

/**
 * Shared re-publish loop for public events that race the asynchronous case&lt;-&gt;hearing link:
 * the {@code Case} aggregate silently drops the update ({@code hearingIds.isEmpty() →
 * Stream.empty()}) until the async {@code add-hearing-to-case} command has run, and a dropped
 * publish emits nothing — no JMS redelivery, no observable — so the only recovery is publishing
 * again with a fresh metadata id (the framework dedupes by metadata id).
 *
 * <p><b>Adaptive poll budget.</b> A dropped publish can never satisfy the poll, so burning the
 * full {@link RestPollerHelper#TIMEOUT_IN_MILLIS} budget before re-publishing is pure waste —
 * on vld this class of loop cost ~375s per run in full-budget first attempts. Non-final attempts
 * therefore poll with the short {@link RestPollerHelper#RETRY_ATTEMPT_TIMEOUT_IN_MILLIS} budget;
 * only the final attempt gets the full budget (preserving the original tail behaviour for a
 * genuinely slow, but not dropped, projection).
 *
 * <p>The attempt count is derived so the short-probe phase spans the same wall clock as the
 * previous 3 &times; full-budget loops' first two attempts — the last re-publish therefore
 * happens no earlier than it used to, preserving coverage of slow link formation (measured at
 * ~90s on vld), and total worst-case wall clock stays at 3 &times; the full budget. What changes
 * is granularity: a re-publish lands within ~5s of the link forming instead of up to a full
 * budget later.
 */
public final class PublishRetryHelper {

    private static final int MAX_PUBLISH_ATTEMPTS =
            (int) (2 * RestPollerHelper.TIMEOUT_IN_MILLIS / RETRY_ATTEMPT_TIMEOUT_IN_MILLIS) + 1;

    private PublishRetryHelper() {
    }

    /**
     * Publishes via {@code publish} and verifies via {@code verify}, re-publishing until the
     * read model reflects the update or the attempt budget is exhausted. {@code verify} must
     * observe through a {@link RestPollerHelper}-built poll so the adaptive budget applies.
     */
    public static void publishUntilReflected(final Logger logger, final String tag, final String description,
                                             final Runnable publish, final Runnable verify) {
        retryLoop(logger, tag, description, publish, verify, true);
    }

    /**
     * Variant for callers that have already done the initial publish: attempt 1 only verifies;
     * {@code republish} runs before each subsequent attempt.
     */
    public static void verifyOrRepublishUntilReflected(final Logger logger, final String tag, final String description,
                                                       final Runnable republish, final Runnable verify) {
        retryLoop(logger, tag, description, republish, verify, false);
    }

    private static void retryLoop(final Logger logger, final String tag, final String description,
                                  final Runnable publish, final Runnable verify,
                                  final boolean publishOnFirstAttempt) {
        for (int attempt = 1; attempt <= MAX_PUBLISH_ATTEMPTS; attempt++) {
            final boolean finalAttempt = attempt == MAX_PUBLISH_ATTEMPTS;
            if (attempt > 1 || publishOnFirstAttempt) {
                logger.info("[{}] publishing {} (attempt {}/{})", tag, description, attempt, MAX_PUBLISH_ATTEMPTS);
                publish.run();
            }
            try {
                if (finalAttempt) {
                    verify.run();
                } else {
                    withRetryAttemptPollBudget(verify);
                }
                logger.info("[{}] read model reflected {} after {} attempt(s)", tag, description, attempt);
                return;
            } catch (final ConditionTimeoutException notYetReflected) {
                if (finalAttempt) {
                    logger.error("[{}] {} still not reflected after {} attempts ({}ms short / {}ms final poll budgets) — failing",
                            tag, description, MAX_PUBLISH_ATTEMPTS, RETRY_ATTEMPT_TIMEOUT_IN_MILLIS, TIMEOUT_IN_MILLIS);
                    throw notYetReflected;
                }
                // The case<->hearing link was not established when this publish was processed, so the
                // update was dropped. Re-publish with a fresh metadata id and poll again.
                logger.warn("[{}] attempt {} did not land within {}ms (case<->hearing link likely not yet established); re-publishing",
                        tag, attempt, RETRY_ATTEMPT_TIMEOUT_IN_MILLIS);
            }
        }
    }
}

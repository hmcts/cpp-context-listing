package uk.gov.moj.cpp.listing.it.util;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;

import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.RestPoller;

import java.time.Duration;

public class RestPollerHelper {

    public static final long DELAY_IN_MILLIS = 300L;
    public static final long INTERVAL_IN_MILLIS = 20L;
    public static final long TIMEOUT_IN_MILLIS = 90000L;
    public static final long RETRY_ATTEMPT_TIMEOUT_IN_MILLIS = 5000L;
    public static final FibonacciPollWithStartAndMax POLL_INTERVAL = new FibonacciPollWithStartAndMax(Duration.ofMillis(INTERVAL_IN_MILLIS), Duration.ofMillis(DELAY_IN_MILLIS));

    private static final ThreadLocal<Long> POLL_BUDGET_OVERRIDE = new ThreadLocal<>();

    public static RestPoller pollWithDefaults(final RequestParams requestParams) {
        return poll(requestParams, POLL_INTERVAL, Duration.ofMillis(currentPollBudgetInMillis()));
    }
    public static RestPoller pollWithDefaults(final RequestParamsBuilder requestParams) {
        return poll(requestParams.build(), POLL_INTERVAL, Duration.ofMillis(currentPollBudgetInMillis()));
    }

    public static RestPoller pollWithDelayForJms(final RequestParams requestParams) {
        return poll(requestParams, POLL_INTERVAL, Duration.ofMillis(currentPollBudgetInMillis()))
                .pollDelay(DELAY_IN_MILLIS, MILLISECONDS); // Add delay for JMS processing;
    }

    /**
     * Runs {@code verification} with the poll budget reduced to
     * {@link #RETRY_ATTEMPT_TIMEOUT_IN_MILLIS} for every poll constructed on this thread inside it.
     *
     * <p>For re-publish retry loops (see {@code PublishRetryHelper}): when a publish was silently
     * dropped by the aggregate, the polled observable can never appear, so non-final attempts
     * should fail fast and re-publish instead of burning the full {@link #TIMEOUT_IN_MILLIS}
     * budget. The override is read when the {@code RestPoller} is built (on the calling thread),
     * so it applies to all polls the verification starts, and is always restored afterwards.
     */
    public static void withRetryAttemptPollBudget(final Runnable verification) {
        POLL_BUDGET_OVERRIDE.set(RETRY_ATTEMPT_TIMEOUT_IN_MILLIS);
        try {
            verification.run();
        } finally {
            POLL_BUDGET_OVERRIDE.remove();
        }
    }

    private static long currentPollBudgetInMillis() {
        final Long override = POLL_BUDGET_OVERRIDE.get();
        return override != null ? override : TIMEOUT_IN_MILLIS;
    }
}

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
    public static final FibonacciPollWithStartAndMax POLL_INTERVAL = new FibonacciPollWithStartAndMax(Duration.ofMillis(INTERVAL_IN_MILLIS), Duration.ofMillis(DELAY_IN_MILLIS));

    public static RestPoller pollWithDefaults(final RequestParams requestParams) {
        return poll(requestParams, POLL_INTERVAL, Duration.ofMillis(TIMEOUT_IN_MILLIS));
    }
    public static RestPoller pollWithDefaults(final RequestParamsBuilder requestParams) {
        return poll(requestParams.build(), POLL_INTERVAL, Duration.ofMillis(TIMEOUT_IN_MILLIS));
    }

    public static RestPoller pollWithDelayForJms(final RequestParams requestParams) {
        return poll(requestParams, POLL_INTERVAL, Duration.ofMillis(TIMEOUT_IN_MILLIS))
                .pollDelay(DELAY_IN_MILLIS, MILLISECONDS); // Add delay for JMS processing;
    }
}

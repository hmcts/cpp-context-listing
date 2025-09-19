package uk.gov.moj.cpp.listing.it.util;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.RestPoller;

public class RestPollerHelper {

    public static final long DELAY_IN_MILLIS = 300L;
    public static final long INTERVAL_IN_MILLIS = 100L;
    public static final long TIMEOUT_IN_MILLIS = 50000L;

    public static RestPoller pollWithDefaults(final RequestParams requestParams) {
        return poll(requestParams)
                .timeout(TIMEOUT_IN_MILLIS, MILLISECONDS)
                .pollDelay(DELAY_IN_MILLIS, MILLISECONDS)
                .pollInterval(INTERVAL_IN_MILLIS, MILLISECONDS);
    }

    public static RestPoller pollWithDelayForJms(final RequestParams requestParams) {
        return poll(requestParams)
                .timeout(TIMEOUT_IN_MILLIS, MILLISECONDS)
                .pollDelay(DELAY_IN_MILLIS, MILLISECONDS)  // Add delay for JMS processing
                .pollInterval(INTERVAL_IN_MILLIS, MILLISECONDS);
    }
}

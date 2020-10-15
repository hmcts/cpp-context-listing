package uk.gov.moj.cpp.listing.it.util;

import static java.text.MessageFormat.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.http.RestPoller;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class HearingHelper {
    public static final long DELAY_IN_MILLIS = 0L;
    public static final long INTERVAL_IN_MILLIS = 200L;
    public static final long TIMEOUT_IN_MILLIS = 20000L;

    public static JsonObject getHearingById(final UUID userId, final UUID hearingId, final Matcher<? super ReadContext> jsonPayloadMatcher) {

        ResponseData responseData = pollWithDefaults(getParams(userId, hearingId))
                .until(
                        status().is(OK),
                        payload().isJson(jsonPayloadMatcher));

        return getJsonObject(responseData.getPayload());
    }

    private static RestPoller pollWithDefaults(final RequestParams requestParams) {
        return poll(requestParams)
                .timeout(TIMEOUT_IN_MILLIS, MILLISECONDS)
                .pollDelay(DELAY_IN_MILLIS, MILLISECONDS)
                .pollInterval(INTERVAL_IN_MILLIS, MILLISECONDS);
    }


    private static JsonObject getJsonObject(final String json) {
        try (final JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }


    private static RequestParams getParams(final UUID userId, final UUID hearingId) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"), hearingId));

        return requestParams(searchHearingUrl, "application/vnd.listing.search.hearing+json")
                .withHeader(HeaderConstants.USER_ID, userId).build();
    }
}

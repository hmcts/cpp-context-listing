package uk.gov.moj.cpp.listing.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher;
import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;
import uk.gov.moj.cpp.listing.it.util.RestPollerHelper;
import uk.gov.moj.cpp.listing.utils.PropertyUtil;

import java.text.MessageFormat;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

public class SearchHearingHelper {

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String HEARING_FILTER = "$.hearings[?(@.id == '%s')]";

    public static String pollForHearing(final String url, final String userId, final Matcher[] matchers) {

        return pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, userId).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))
                ).getPayload();

    }

    public static String pollForHearing(final String url, final String userId, final Matcher[] matchers, final String mediaType) {

        return pollWithDefaults(requestParams(url, mediaType).withHeader(USER_ID, userId).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))
                ).getPayload();

    }

    public static String pollUntilHearingIsPresent(final String courtCentreId, final boolean allocated, final String userId, final String hearingId) {

        return pollForHearing(courtCentreId, allocated, userId, new Matcher[]{
                withJsonPath(getHearingFilter(hearingId), hasSize(1))
        });
    }

    public static void pollUntilHearingIsPresent(final String url, final String userId, final String hearingId) {

        pollForHearing(url, userId, new Matcher[]{
                withJsonPath(getHearingFilter(hearingId), hasSize(1))
        });
    }

    public static String pollUntilHearingIsPresent(final String url, final String userId, final String hearingId, final String mediaType, final int size) {

        final String payload = pollForHearing(url, userId, new Matcher[]{
                withJsonPath(getHearingFilter(hearingId), hasSize(size))
        }, mediaType);

     return payload;
    }

    public static String pollUntilSizeMatch(final String url, final String userId, final String mediaType, final int count) {
        final String payload = pollForHearing(url, userId, new Matcher[]{withJsonPath("$.hearings",hasSize(count))}, mediaType);

        return payload;
    }
    public static String pollForHearing(final String courtCentreId, final boolean allocated, final String userId, final Matcher[] matchers) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), courtCentreId, allocated));

        return pollForHearing(searchHearingUrl, userId, matchers);
    }

    public static String getHearingFilter(final String hearingId) {
        return String.format(HEARING_FILTER, hearingId);
    }

    /**
     * Poll for hearing with JMS delay to handle asynchronous message processing.
     * Use this method when the test involves JMS commands that need time to process.
     */
    public static String pollForHearingWithJmsDelay(final String courtCentreId, final boolean allocated, final String userId, final Matcher[] matchers) {
        final String searchHearingUrl = String.format("%s/%s", PropertyUtil.getBaseUri(),
                MessageFormat.format(PropertyUtil.readConfig().getProperty("listing.range.search.hearings"), courtCentreId, allocated));

        return RestPollerHelper.pollWithDelayForJms(RequestParamsBuilder.requestParams(searchHearingUrl, SearchHearingHelper.MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(HeaderConstants.USER_ID, userId).build())
                .until(
                        ResponseStatusMatcher.status().is(OK),
                        ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(matchers))
                ).getPayload();
    }

    /**
     * Poll for hearing by week commencing using the new week commencing range search endpoint.
     * Uses the listing.range.search.hearings.by.week.commencing property to construct the URL
     * with specific parameters: courtCentreId, allocated, weekCommencingStartDate, weekCommencingEndDate, pageNumber, pageSize.
     */
    public static String pollForHearingByWeekCommencing(final String courtCentreId, final boolean allocated, final String weekCommencingStartDate, final String weekCommencingEndDate, final String userId, final Matcher[] matchers) {
        final String searchHearingUrl = String.format("%s/%s", PropertyUtil.getBaseUri(),
                MessageFormat.format(PropertyUtil.readConfig().getProperty("listing.range.search.hearings.by.week.commencing"), 
                       weekCommencingStartDate, weekCommencingEndDate, courtCentreId, allocated))
                + "&pageNumber=1&pageSize=50";

        return SearchHearingHelper.pollForHearing(searchHearingUrl, userId, matchers);
    }
}

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

    public static void pollUntilHearingIsPresent(final String courtCentreId, final boolean allocated, final String userId, final String hearingId) {

        pollForHearing(courtCentreId, allocated, userId, new Matcher[]{
                withJsonPath(getHearingFilter(hearingId), hasSize(1))
        });
    }

    public static void pollUntilHearingIsPresent(final String url, final String userId, final String hearingId) {

        pollForHearing(url, userId, new Matcher[]{
                withJsonPath(getHearingFilter(hearingId), hasSize(1))
        });
    }

    public static String pollForHearing(final String courtCentreId, final boolean allocated, final String userId, final Matcher[] matchers) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), courtCentreId, allocated));

        return pollForHearing(searchHearingUrl, userId, matchers);
    }

    public static String getHearingFilter(final String hearingId) {
        return String.format(HEARING_FILTER, hearingId);
    }
}

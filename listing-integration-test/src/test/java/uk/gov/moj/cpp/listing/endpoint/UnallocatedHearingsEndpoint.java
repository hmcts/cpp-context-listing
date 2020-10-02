package uk.gov.moj.cpp.listing.endpoint;

import static java.text.MessageFormat.format;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.moj.cpp.listing.steps.data.HearingData;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class UnallocatedHearingsEndpoint {

    public static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";

    public static void pollForUnallocatedHearings(UUID userId, HearingData hearingsData, Matcher matcher) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.unallocated-hearings"),
                        hearingsData.getListedCases().get(0).getCaseId()));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, userId))
                .until(
                        status().is(Response.Status.OK),
                        payload().isJson(matcher)
                );
    }
}

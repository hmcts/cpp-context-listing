package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

public class CommonHearingSteps extends AbstractIT {

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";

    protected HearingsData hearingsData;

    public CommonHearingSteps(final HearingsData hearingsData) {
        this.hearingsData = hearingsData;
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingData.getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].id",
                                        equalTo(hearingData.getCourtApplications().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].id",
                                        equalTo(hearingData.getListedCases().get(0).getCaseId().toString())),
                                withJsonPath("$.hearings[0].listedCases[1].id",
                                        equalTo(hearingData.getListedCases().get(1).getCaseId().toString()))
                        )));
    }
}

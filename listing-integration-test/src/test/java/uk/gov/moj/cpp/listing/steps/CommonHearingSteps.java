package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.hasItem;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.getHearingFilter;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.hamcrest.Matcher;

public class CommonHearingSteps extends AbstractIT {

    protected HearingsData hearingsData;

    public CommonHearingSteps(final HearingsData hearingsData) {
        this.hearingsData = hearingsData;
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String hearingFilter = getHearingFilter(hearingData.getId().toString());
        pollForHearing(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingFilter + ".courtApplications[0].id",
                        hasItem(hearingData.getCourtApplications().get(0).getId().toString())),
                withJsonPath(hearingFilter + ".listedCases[0].id",
                        hasItem(hearingData.getListedCases().get(0).getCaseId().toString())),
                withJsonPath(hearingFilter + ".listedCases[1].id",
                        hasItem(hearingData.getListedCases().get(1).getCaseId().toString()))
        });
    }

}

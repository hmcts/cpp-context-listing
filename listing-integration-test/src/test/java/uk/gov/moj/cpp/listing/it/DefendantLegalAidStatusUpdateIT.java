package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;

import uk.gov.moj.cpp.listing.steps.DefendantLegalAidStatusUpdateSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class DefendantLegalAidStatusUpdateIT extends AbstractIT {

    @Test
    void shouldUpdateDefendantLegalAidStatusFollowingPublicDefendantLegalAidStatusUpdatedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final DefendantLegalAidStatusUpdateSteps defendantLegalAidStatusUpdateSteps = new DefendantLegalAidStatusUpdateSteps(caseId, hearingData);
        defendantLegalAidStatusUpdateSteps.whenCaseDefendantLegalAidStatusUpdatedPublicEventIsPublished();
        // Use JMS-aware polling to handle asynchronous message processing
        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].legalAidStatus", equalTo("Granted"))
        });

    }
}

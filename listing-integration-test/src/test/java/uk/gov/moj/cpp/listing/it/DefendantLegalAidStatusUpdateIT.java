package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.DefendantLegalAidStatusUpdateSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

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
        defendantLegalAidStatusUpdateSteps.publishUntilLegalAidStatusReflected();
    }
}

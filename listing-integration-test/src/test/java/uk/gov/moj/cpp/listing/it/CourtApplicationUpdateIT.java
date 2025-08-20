package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;

import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
class CourtApplicationUpdateIT extends AbstractIT {

    @Test
    
    void shouldUpdateCourtApplicationForFutureHearings() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationUpdatedPublicEventIsPublished();
        courtApplicationSteps.verifyCourtApplicationUpdatedFromAPI();
    }
}

package uk.gov.moj.cpp.listing.it;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
class CourtApplicationUpdateIT extends AbstractIT {

    @Test
    
    void shouldUpdateCourtApplicationForFutureHearings() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.publishUntilCourtApplicationReflected();
    }
}

package uk.gov.moj.cpp.listing.it;

import org.junit.Test;
import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

public class CourtApplicationUpdateIT extends AbstractIT {

    @Test
    public void shouldUpdateCourtApplicationForFutureHearings() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData)) {
            courtApplicationSteps.whenCaseCourtApplicationUpdatedPublicEventIsPublished();
            courtApplicationSteps.verifyCourtApplicationUpdatedInActiveMQ();
            courtApplicationSteps.verifyCourtApplicationUpdatedInPrivateMessage();
            courtApplicationSteps.verifyCourtApplicationUpdatedFromAPI();
        }
    }
}

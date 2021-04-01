package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.Test;


public class AddCourtApplicationIT extends AbstractIT {

    @Test
    public void shouldAddCourtApplicationForHearingId() {

        final HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(false);
        }

        try (final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData)) {
            courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
            courtApplicationSteps.verifyCourtApplicationAddedInActiveMQ();
            courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
            courtApplicationSteps.verifyCourtApplicationAddedFromAPI();
        }
    }
}

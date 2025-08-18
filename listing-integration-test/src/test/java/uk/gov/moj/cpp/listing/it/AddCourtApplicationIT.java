package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;

import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.Test;

public class AddCourtApplicationIT extends AbstractIT {

    @Test
    public void shouldAddCourtApplicationForHearingId() {

        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(false);
        listCourtHearingSteps.verifyPublicEventCourtApplicationAdded();

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyPublicEventCourtApplicationAdded();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(false);
    }
}

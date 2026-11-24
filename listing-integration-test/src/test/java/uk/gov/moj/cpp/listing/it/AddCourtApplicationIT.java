package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubCrownFallbackDraftSessionForHearing;

import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.Test;
class AddCourtApplicationIT extends AbstractIT {

    @Test
    void shouldAddCourtApplicationForHearingId() {

        final HearingsData hearingsData = hearingsData();
        // No courtroom in the payload: real courtscheduler auto-creates a DRAFT session for the
        // hearing's date. Stub that answer so listing books it instead of failing open.
        hearingsData.getHearingData().forEach(hearing ->
                stubCrownFallbackDraftSessionForHearing(hearing.getId().toString(), hearing.getHearingStartDate(),
                        hearing.getCourtRoomId()));
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

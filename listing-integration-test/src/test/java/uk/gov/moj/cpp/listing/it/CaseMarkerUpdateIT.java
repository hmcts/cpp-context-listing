package uk.gov.moj.cpp.listing.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.with;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateCaseMarkersSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseMarkerData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S2925")
public class CaseMarkerUpdateIT extends AbstractIT {

    @Test
    public void shouldUpdateCaseMarkersForListedCase() throws Exception {
        final HearingsData hearingsData = listCourtHearing();
        final CaseMarkerData caseMarkerData = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseMarkers().get(0);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final UpdateCaseMarkersSteps steps = new UpdateCaseMarkersSteps(caseId, hearingData, caseMarkerData);
        steps.whenCaseMarkerUpdatedPublicEventIsPublished();
        with().pollDelay(10000, MILLISECONDS);
        steps.verifyPublicEventCaseMarkersUpdatedInActiveMQ();
        steps.verifyEventCaseMarkersToBeUpdateInActiveMQ();
    }


    private HearingsData listCourtHearing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        return hearingsData;
    }
}

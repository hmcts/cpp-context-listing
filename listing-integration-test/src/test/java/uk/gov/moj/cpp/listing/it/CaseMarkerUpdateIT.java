package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateCaseMarkersSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseMarkerData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;

import java.util.UUID;

import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S2925")
public class CaseMarkerUpdateIT extends AbstractIT {

    @Test
    public void shouldUpdateCaseMarkersForListedCase() {
        final HearingsData hearingsData = listCourtHearing();
        final ListedCaseData caseToUpdateMarkers = hearingsData.getHearingData().get(0).getListedCases().get(0);
        final CaseMarkerData caseMarkerData = caseToUpdateMarkers.getCaseMarkers().get(0);
        final UUID caseIdToUpdateMarkers = caseToUpdateMarkers.getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final UpdateCaseMarkersSteps steps = new UpdateCaseMarkersSteps(caseIdToUpdateMarkers, hearingData, caseMarkerData);
        steps.whenCaseMarkerUpdatedPublicEventIsPublished();
        steps.verifyCaseMarkersUpdatedThroughAPI(caseIdToUpdateMarkers);
    }


    private HearingsData listCourtHearing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        return hearingsData;
    }
}

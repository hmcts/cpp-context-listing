package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import org.junit.Before;
import org.junit.Test;

public class CourtListIT extends AbstractIT {

    private static final String ALPHABETICAL = "Alphabetical";
    private static final String PUBLIC = "Public";
    public static final String STANDARD = "Standard";
    private CourtListSteps courtListSteps;

    @Before
    public void setupStepsForCourtList() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
        }
        courtListSteps = new CourtListSteps(updatedHearingDataForAllocation);
    }

    @Test
    public void generateAlphabeticalCourtListForHearing() {
        courtListSteps.verifyCourtListRequestedAndIsCorrect(ALPHABETICAL);
    }


    @Test
    public void generatePublicCourtList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrect(PUBLIC);
    }

    @Test
    public void generateStandardCourtList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrect(STANDARD);
    }

}

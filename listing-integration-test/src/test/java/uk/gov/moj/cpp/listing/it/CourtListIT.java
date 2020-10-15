package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CourtListIT extends AbstractIT {

    private static final String ALPHABETICAL = "Alphabetical";
    private static final String PUBLIC = "Public";
    public static final String STANDARD = "Standard";
    public static final String JUDGE = "Judge";
    public static final String BENCH = "Bench";
    final UUID COURT_CENTRE_ID = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
    final UUID HEARING_TYPE_ID = fromString("52edf232-3c09-4c74-a6ad-737985c2e662");

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

    @Test
    public void generateJudgeList() {
        stubOrganisationUnit(COURT_CENTRE_ID);
        stubGetReferenceDataHearingTypes(HEARING_TYPE_ID);
        courtListSteps.verifyCourtListRequestedAndIsCorrect(JUDGE);
    }

    @Test
    public void generateBenchList() {
        courtListSteps.verifyCourtListRequestedAndIsCorrect(BENCH);
    }

}

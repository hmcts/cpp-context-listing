package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.RestrictCourtListData;

import java.util.UUID;

import org.junit.Test;

public class ListCourtHearingIT extends AbstractIT {

    static final boolean UNALLOCATED = false;
    static final boolean ALLOCATED = true;

    @Test
    public void listHearingWithUnallocatedData() {
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsData())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
    }

    @Test
    public void listHearingWithAllocatedData() {
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
    }

    @Test
    public void listHearingWithUnallocatedDataForStandaloneApplication() {
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataStandaloneApplication())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedInActiveMQForStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);
        }
    }

    @Test
    public void listHearingWithLegalEntity() {
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithLegalEntity())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingWithLegalEntity();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithLegalEntity(UNALLOCATED);
        }
    }
}
                                                                                                                                                                                                                                     
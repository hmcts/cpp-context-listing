package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.SendCaseForListingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.Test;

public class SendCaseForListingIT extends AbstractIT {

    static final boolean UNALLOCATED = false;
    static final boolean ALLOCATED = true;

    @Test
    public void listHearingWithUnallocatedData() {
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(HearingsData.hearingsData())) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedInActiveMQ();
            sendCaseForListingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
    }

    @Test
    public void listHearingWithAllocatedData() {
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedInActiveMQ();
            sendCaseForListingSteps.verifyHearingAllocatedForListingInActiveMQ();
            sendCaseForListingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
    }
}
                                                                                                                                                                                                                                     
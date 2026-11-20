package uk.gov.moj.cpp.listing.it;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.listing.steps.PayloadBasedListCourtHearingSteps;

/**
 * Example integration test demonstrating the new payload-based approach
 * using JSON files from test-data folder instead of nested classes
 */
class PayloadBasedListCourtHearingIT extends AbstractIT {

    @Test
    void shouldListCourtHearingSubmittedWithSpiAllocated() {
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiAllocated();
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);
    }

    @Test
    void shouldListCourtHearingSubmittedWithSpiUnallocated() {
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiUnallocated();
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);
    }

    @Test
    void shouldListCourtHearingSubmittedWithSpiTwoDefendantsUnallocated() {
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiTwoDefendantsUnallocated();
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);
    }

    @Test
    void shouldListCourtHearingSubmittedWithAdhocHearingCreation() {
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        listCourtHearingSteps.whenListCourtHearingSubmittedWithAdhocHearingCreation();
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);
    }

    @Test
    void shouldListCourtHearingSubmittedWithMccWithoutCourtScheduleAllocated() {
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        listCourtHearingSteps.whenListCourtHearingSubmittedWithMccWithoutCourtScheduleAllocated();
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);
    }
} 
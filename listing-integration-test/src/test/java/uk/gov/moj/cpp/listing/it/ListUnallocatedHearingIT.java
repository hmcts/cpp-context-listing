package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListUnAllocatedCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")

public class ListUnallocatedHearingIT extends AbstractIT {

    @Test
    public void shouldListHearingWithUnallocatedData() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListUnAllocatedCourtHearingSteps listCourtHearingSteps = new ListUnAllocatedCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForUnallocatedListing();
        listCourtHearingSteps.verifyHearingUnallocatededFromAPI();
    }
}

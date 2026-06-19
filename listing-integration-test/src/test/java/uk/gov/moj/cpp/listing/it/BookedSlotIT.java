package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


public class BookedSlotIT extends AbstractIT {




    @Test
    public void shouldListHearingWithBookedSlots() {

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataForBookedSlot());
        listCourtHearingSteps.whenCaseIsSubmittedForListingWithBookedSlot();
        listCourtHearingSteps.verifyHearingListedFromAPIAllocatedForBookSlots();
        listCourtHearingSteps.verifyPublicEventHearingConfirmed();
    }
}

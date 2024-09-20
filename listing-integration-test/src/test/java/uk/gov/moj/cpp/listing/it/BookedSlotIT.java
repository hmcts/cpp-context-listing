package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class BookedSlotIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
    }


    @Test
    public void shouldListHearingWithBookedSlots() {

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataForBookedSlot());
        listCourtHearingSteps.whenCaseIsSubmittedForListingWithBookedSlot();
        listCourtHearingSteps.verifyHearingListedWithBookedSlotsInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPIAllocatedForBookSlots();
        listCourtHearingSteps.verifyHearingConfirmedInPublicMQ();
    }
}

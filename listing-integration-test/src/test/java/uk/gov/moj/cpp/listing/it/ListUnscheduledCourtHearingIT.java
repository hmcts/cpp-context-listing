package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListUnscheduledCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateUnscheduledHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class ListUnscheduledCourtHearingIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @Before
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
    }


    @Test
    public void shouldListHearingWithUnallocatedData() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        try (final UpdateUnscheduledHearingSteps updateHearingSteps = new UpdateUnscheduledHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsInAllocationInMQ();
            updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
            updateHearingSteps.verifyHearingIsNotUnscheduledListedFromAPI();

        }
    }

}

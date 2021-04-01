package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListUnAllocatedCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class ListUnallocatedHearingIT extends AbstractIT {

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
        try (final ListUnAllocatedCourtHearingSteps listCourtHearingSteps = new ListUnAllocatedCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnallocatedListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingUnallocatededFromAPI();
        }
    }

}

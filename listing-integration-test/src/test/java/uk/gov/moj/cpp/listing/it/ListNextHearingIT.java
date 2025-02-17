package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.notHmiEnabledHearingsData;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListNextHearingIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "listing_notes");
    }

    @Test
    public void shouldListNextHearings() {
        final HearingsData firstHearings = hearingsData();
        final HearingsData nextHearings = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps.verifyHearingListedFromAPI(nextHearings);
    }


    @Test
    public void shouldDeleteOldNextHearingsAndListNextHearings() {

        final HearingsData oldNextHearings = hearingsData();
        final HearingsData nextHearings = hearingsData();

        final HearingsData firstHearings = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenNextHearingSubmittedForListing(oldNextHearings);
        listNextHearingSteps1.verifyHearingListedFromAPI(oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyPublicUnallocatedOldHearingDeletedInPublicMQ(oldNextHearings);

        listNextHearingSteps2.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps2.verifyHearingListedFromAPI(nextHearings);
        listNextHearingSteps2.verifyPublicOffencesMovedToHearingInActiveMQ(nextHearings, oldNextHearings, firstHearings.getHearingData().get(0).getId());
    }


    @Test
    public void shouldDeleteOldScheduledNextHearingsAndScheduledNextHearings() {

        final HearingsData oldNextHearings = notHmiEnabledHearingsData();
        final HearingsData nextHearings = notHmiEnabledHearingsData();
        final HearingsData firstHearings = notHmiEnabledHearingsData();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenUnscheduledNextHearingSubmittedForListing(oldNextHearings);
        listNextHearingSteps1.verifyUnscheduledHearingListedFromApi(oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyOldHearingDeleted(oldNextHearings);
        listNextHearingSteps2.verifyPublicUnallocatedOldHearingDeletedInPublicMQ(oldNextHearings);

        listNextHearingSteps2.whenUnscheduledNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps2.verifyUnscheduledHearingListedFromApi(nextHearings);
        listNextHearingSteps2.verifyPublicOffencesMovedToHearingInActiveMQ(nextHearings, oldNextHearings, firstHearings.getHearingData().get(0).getId());
    }


    @Test
    public void shouldDeleteOldRelatedtHearingsAndUpdateRelatedHearings() {

        final HearingsData oldNextHearings = hearingsData();
        final HearingsData nextHearings = hearingsData();

        final HearingsData firstHearings = hearingsData();
        final HearingsData existedHearings = hearingsData();

        final ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps1.verifyHearingListedFromAPI(UNALLOCATED);

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(existedHearings);
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyHearingListedFromAPI(UNALLOCATED);


        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps1.verifyCasesAddedToHearingFromApi(existedHearings, oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyPublicOffencesRemovedFromExistingHearingInActiveMQ(existedHearingId, oldNextHearings);

        listNextHearingSteps2.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, nextHearings);
        listNextHearingSteps2.verifyCasesAddedToHearingFromApi(existedHearings, nextHearings);
    }

    @Test
    public void shouldAddCasetoExistingHearingforAdHocHearing() {
        final HearingsData existedHearings = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(existedHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();

        final HearingsData adhocHearings = hearingsData();
        adhocHearings.getHearingData().get(0).getListedCases().addAll(existedHearings.getHearingData().get(0).getListedCases());
        final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(adhocHearings.getHearingData().get(0));

        listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListingForAdhocHearing(existedHearingId, adhocHearings);
        listNextHearingSteps.verifyPublicHearingAddedToCaseInActiveMQ(existedHearingId);
        listNextHearingSteps.verifyCasesAddedToHearingFromApi(existedHearings, adhocHearings);

    }

    @Test
    public void shouldDeleteOldAllocatedRelatedHearingsAndUpdateRelatedHearings() {

        final HearingsData oldNextHearings = hearingsDataWithAllocationDataAndJudiciary();

        final HearingsData firstHearings = hearingsDataWithAllocationDataAndJudiciary();
        final HearingsData existedHearings = hearingsDataWithAllocationDataAndJudiciary();

        final ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps1.verifyHearingListedFromAPI(ALLOCATED);

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(existedHearings);
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyHearingListedFromAPI(ALLOCATED);


        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps1.verifyCasesAddedToAllocatedHearingFromApi(existedHearings, oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps3 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps3.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps3.verifyCasesAddedToAllocatedHearingFromApi(existedHearings, oldNextHearings);

        ListNextHearingSteps listNextHearingSteps4 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps4.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps4.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);

    }

}

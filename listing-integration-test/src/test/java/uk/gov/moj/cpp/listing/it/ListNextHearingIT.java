package uk.gov.moj.cpp.listing.it;

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
        final HearingsData firstHearings = HearingsData.hearingsData();
        final HearingsData nextHearings = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps.verifyNextHearingRequestedInActiveMQ(nextHearings);
        listNextHearingSteps.verifyHearingListedInActiveMQ(nextHearings);
        listNextHearingSteps.verifyHearingListedFromAPI(nextHearings);
    }


    @Test
    public void shouldDeleteOldNextHearingsAndListNextHearings() {

        final HearingsData oldNextHearings = HearingsData.hearingsData();
        final HearingsData nextHearings = HearingsData.hearingsData();

        final HearingsData firstHearings = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenNextHearingSubmittedForListing(oldNextHearings);
        listNextHearingSteps1.verifyNextHearingRequestedInActiveMQ(oldNextHearings);
        listNextHearingSteps1.verifyHearingListedInActiveMQ(oldNextHearings);
        listNextHearingSteps1.verifyHearingListedFromAPI(oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyUnAllocatedOldHearingDeletedInActiveMQ(oldNextHearings);
        listNextHearingSteps2.verifyOldHearingDeleted(oldNextHearings);
        listNextHearingSteps2.verifyHearingMarkedAsDuplicateForCaseInActiveMQ(oldNextHearings);
        listNextHearingSteps2.verifyPublicUnallocatedOldHearingDeletedInPublicMQ(oldNextHearings);

        listNextHearingSteps2.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps2.verifyNextHearingRequestedInActiveMQ(nextHearings);
        listNextHearingSteps2.verifyHearingListedInActiveMQ(nextHearings);
        listNextHearingSteps2.verifyHearingListedFromAPI(nextHearings);
    }


    @Test
    public void shouldDeleteOldScheduledNextHearingsAndScheduledNextHearings() {

        final HearingsData oldNextHearings = HearingsData.notHmiEnabledHearingsData();
        final HearingsData nextHearings = HearingsData.notHmiEnabledHearingsData();
        final HearingsData firstHearings = HearingsData.notHmiEnabledHearingsData();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenUnscheduledNextHearingSubmittedForListing(oldNextHearings);
        listNextHearingSteps1.verifyUnscheduledNextHearingRequestedInActiveMQ(oldNextHearings);
        listNextHearingSteps1.verifyHearingListedInActiveMQ(oldNextHearings);
        listNextHearingSteps1.verifyUnscheduledHearingListedFromApi(oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyUnAllocatedOldHearingDeletedInActiveMQ(oldNextHearings);
        listNextHearingSteps2.verifyOldHearingDeleted(oldNextHearings);
        listNextHearingSteps2.verifyHearingMarkedAsDuplicateForCaseInActiveMQ(oldNextHearings);
        listNextHearingSteps2.verifyPublicUnallocatedOldHearingDeletedInPublicMQ(oldNextHearings);

        listNextHearingSteps2.whenUnscheduledNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps2.verifyUnscheduledNextHearingRequestedInActiveMQ(nextHearings);
        listNextHearingSteps2.verifyHearingListedInActiveMQ(nextHearings);
        listNextHearingSteps2.verifyUnscheduledHearingListedFromApi(nextHearings);
    }


    @Test
    public void shouldDeleteOldRelatedtHearingsAndUpdateRelatedHearings() {

        final HearingsData oldNextHearings = HearingsData.hearingsData();
        final HearingsData nextHearings = HearingsData.hearingsData();

        final HearingsData firstHearings = HearingsData.hearingsData();
        final HearingsData existedHearings = HearingsData.hearingsData();

        final ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps1.verifyHearingListedInActiveMQ();
        listCourtHearingSteps1.verifyHearingListedFromAPI(UNALLOCATED);

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(existedHearings);
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyHearingListedInActiveMQ();
        listCourtHearingSteps2.verifyHearingListedFromAPI(UNALLOCATED);


        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps1.verifyUpdateRelatedHearingRequestedInActiveMQ(existedHearingId);
        listNextHearingSteps1.verifyCasesAddedToHearingInActiveMQ(existedHearingId, oldNextHearings);
        listNextHearingSteps1.verifyCasesAddedToHearingFromApi(existedHearings, oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyRemoveOffencesFromExistingHearingRequestedInActiveMQ(existedHearingId);
        listNextHearingSteps2.verifyOffencesRemovedFromExistingHearingInActiveMQ(existedHearingId, oldNextHearings);
        listNextHearingSteps2.verifyPublicOffencesRemovedFromExistingHearingInActiveMQ(existedHearingId, oldNextHearings);

        listNextHearingSteps2.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, nextHearings);
        listNextHearingSteps2.verifyUpdateRelatedHearingRequestedInActiveMQ(existedHearingId);
        listNextHearingSteps2.verifyCasesAddedToHearingInActiveMQ(existedHearingId, nextHearings);
        listNextHearingSteps2.verifyCasesAddedToHearingFromApi(existedHearings, nextHearings);
    }


    @Test
    public void shouldDeleteOldAllocatedRelatedHearingsAndUpdateRelatedHearings() {

        final HearingsData oldNextHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final HearingsData nextHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();

        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final HearingsData existedHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();

        final ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps1.verifyHearingListedInActiveMQ();
        listCourtHearingSteps1.verifyHearingListedFromAPI(ALLOCATED);

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(existedHearings);
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyHearingListedInActiveMQ();
        listCourtHearingSteps2.verifyHearingListedFromAPI(ALLOCATED);


        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps1.verifyUpdateRelatedHearingRequestedInActiveMQ(existedHearingId);
        listNextHearingSteps1.verifyCasesAddedToHearingInActiveMQ(existedHearingId, oldNextHearings);
        listNextHearingSteps1.verifyCasesAddedToAllocatedHearingFromApi(existedHearings, oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyRemoveOffencesFromExistingHearingRequestedInActiveMQ(existedHearingId);
        listNextHearingSteps2.verifyOffencesRemovedFromAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);
        listNextHearingSteps2.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);
    }

}

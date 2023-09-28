package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;


public class ListNextHearingIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @Before
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
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0))) {
            listNextHearingSteps.whenNextHearingSubmittedForListing(nextHearings);
            listNextHearingSteps.verifyNextHearingRequestedInActiveMQ(nextHearings);
            listNextHearingSteps.verifyHearingListedInActiveMQ(nextHearings);
            listNextHearingSteps.verifyHearingListedFromAPI(nextHearings);

        }
    }


    @Test
    public void shouldDeleteOldNextHearingsAndListNextHearings() {

        final HearingsData oldNextHearings = HearingsData.hearingsData();
        final HearingsData nextHearings = HearingsData.hearingsData();

        final HearingsData firstHearings = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0))) {
            listNextHearingSteps.whenNextHearingSubmittedForListing(oldNextHearings);
            listNextHearingSteps.verifyNextHearingRequestedInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyHearingListedInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyHearingListedFromAPI(oldNextHearings);

        }

        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0))) {
            listNextHearingSteps.whenDeleteNextHearingSubmittedForListing();
            listNextHearingSteps.verifyUnAllocatedOldHearingDeletedInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyOldHearingDeleted(oldNextHearings);
            listNextHearingSteps.verifyHearingMarkedAsDuplicateForCaseInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyPublicUnallocatedOldHearingDeletedInPublicMQ(oldNextHearings);

            listNextHearingSteps.whenNextHearingSubmittedForListing(nextHearings);
            listNextHearingSteps.verifyNextHearingRequestedInActiveMQ(nextHearings);
            listNextHearingSteps.verifyHearingListedInActiveMQ(nextHearings);
            listNextHearingSteps.verifyHearingListedFromAPI(nextHearings);
        }
    }


    @Test
    public void shouldDeleteOldScheduledNextHearingsAndScheduledNextHearings() {

        final HearingsData oldNextHearings = HearingsData.notHmiEnabledHearingsData();
        final HearingsData nextHearings = HearingsData.notHmiEnabledHearingsData();
        final HearingsData firstHearings = HearingsData.notHmiEnabledHearingsData();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0))) {
            listNextHearingSteps.whenUnscheduledNextHearingSubmittedForListing(oldNextHearings);
            listNextHearingSteps.verifyUnscheduledNextHearingRequestedInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyHearingListedInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyUnscheduledHearingListedFromApi(oldNextHearings);

        }

        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0))) {
            listNextHearingSteps.whenDeleteNextHearingSubmittedForListing();
            listNextHearingSteps.verifyUnAllocatedOldHearingDeletedInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyOldHearingDeleted(oldNextHearings);
            listNextHearingSteps.verifyHearingMarkedAsDuplicateForCaseInActiveMQ(oldNextHearings);
            listNextHearingSteps.verifyPublicUnallocatedOldHearingDeletedInPublicMQ(oldNextHearings);

            listNextHearingSteps.whenUnscheduledNextHearingSubmittedForListing(nextHearings);
            listNextHearingSteps.verifyUnscheduledNextHearingRequestedInActiveMQ(nextHearings);
            listNextHearingSteps.verifyHearingListedInActiveMQ(nextHearings);
            listNextHearingSteps.verifyUnscheduledHearingListedFromApi(nextHearings);
        }
    }


    @Test
    public void shouldDeleteOldRelatedtHearingsAndUpdateRelatedHearings() {

        final HearingsData oldNextHearings = HearingsData.hearingsData();
        final HearingsData nextHearings = HearingsData.hearingsData();

        final HearingsData firstHearings = HearingsData.hearingsData();
        final HearingsData existedHearings = HearingsData.hearingsData();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(existedHearings)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }


        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();
        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0))) {
            listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
            listNextHearingSteps.verifyUpdateRelatedHearingRequestedInActiveMQ(existedHearingId);
            listNextHearingSteps.verifyCasesAddedToHearingInActiveMQ(existedHearingId, oldNextHearings);
            listNextHearingSteps.verifyCasesAddedToHearingFromApi(existedHearings, oldNextHearings);

        }

        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0))) {
            listNextHearingSteps.whenDeleteNextHearingSubmittedForListing();
            listNextHearingSteps.verifyRemoveOffencesFromExistingHearingRequestedInActiveMQ(existedHearingId);
            listNextHearingSteps.verifyOffencesRemovedFromExistingHearingInActiveMQ(existedHearingId, oldNextHearings);
            listNextHearingSteps.verifyPublicOffencesRemovedFromExistingHearingInActiveMQ(existedHearingId, oldNextHearings);

            listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, nextHearings);
            listNextHearingSteps.verifyUpdateRelatedHearingRequestedInActiveMQ(existedHearingId);
            listNextHearingSteps.verifyCasesAddedToHearingInActiveMQ(existedHearingId, nextHearings);
            listNextHearingSteps.verifyCasesAddedToHearingFromApi(existedHearings, nextHearings);
        }
    }

}

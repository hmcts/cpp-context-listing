package uk.gov.moj.cpp.listing.it;

import com.google.common.collect.ImmutableMap;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.ImmutableMap.of;
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;


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
    public void shouldAddCasetoExistingHearingforAdHocHearing() {
        final HearingsData existedHearings = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(existedHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();

        final HearingsData adhocHearings = HearingsData.hearingsData();
        adhocHearings.getHearingData().get(0).getListedCases().addAll(existedHearings.getHearingData().get(0).getListedCases());
        final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(adhocHearings.getHearingData().get(0));
        listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListingForAdhocHearing(existedHearingId, adhocHearings);
        listNextHearingSteps.verifyCasesAddedToHearingInActiveMQ(existedHearingId, adhocHearings);
        listNextHearingSteps.verifyPublicHearingAddedToCaseInActiveMQ(existedHearingId);
        listNextHearingSteps.verifyCasesAddedToHearingFromApi(existedHearings, adhocHearings);

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

        final ListNextHearingSteps listNextHearingSteps3 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps3.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps3.verifyUpdateRelatedHearingRequestedInActiveMQ(existedHearingId);
        listNextHearingSteps3.verifyCasesAddedToHearingInActiveMQ(existedHearingId, oldNextHearings);
        listNextHearingSteps3.verifyCasesAddedToAllocatedHearingFromApi(existedHearings, oldNextHearings);

        ListNextHearingSteps listNextHearingSteps4 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps4.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps4.verifyRemoveOffencesFromExistingHearingRequestedInActiveMQ(existedHearingId);
        listNextHearingSteps4.verifyOffencesRemovedFromAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);
        listNextHearingSteps4.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);

    }

}

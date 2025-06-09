package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.notHmiEnabledHearingsData;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.OffencesForDefendantUpdated;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;

import java.time.LocalDate;
import java.util.UUID;

import org.hamcrest.Matcher;
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
    public void shouldDeletePreviousHearingsAndCreateNextHearingRequested() {

        final HearingsData oldNextHearings = HearingsData.hearingsData();
        final HearingsData nextHearings = HearingsData.hearingsData();

        final HearingsData firstHearings = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenNextHearingSubmittedForListing(oldNextHearings);
        listNextHearingSteps1.verifyHearingListedFromAPI(oldNextHearings);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeletePreviousHearingAndCreateNextHearingForListing(nextHearings);
        listNextHearingSteps2.verifyOldHearingDeleted(oldNextHearings);
        listNextHearingSteps2.verifyPublicUnallocatedOldHearingDeletedInPublicMQ(oldNextHearings);

        listNextHearingSteps2.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps2.verifyHearingListedFromAPI(nextHearings);
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
        listNextHearingSteps1.verifyCasesAddedToHearingFromApi(existedHearings, oldNextHearings, false);

        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps2.verifyPublicOffencesRemovedFromExistingHearingInActiveMQ(existedHearingId, oldNextHearings);

        listNextHearingSteps2.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, nextHearings);
        listNextHearingSteps2.verifyCasesAddedToHearingFromApi(existedHearings, nextHearings, false);
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
        listNextHearingSteps.verifyCasesAddedToHearingFromApi(existedHearings, adhocHearings, false);

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

    @Test
    public void shouldRemoveOffencesFromNextHearingWhenFirstSeededHearingAmended(){

        final HearingsData nextHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final HearingsData secondHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();

        // First hearing created
        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        // First hearing resulted and next hearing created
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.stubReferenceDataForNextHearings(nextHearings);
        listNextHearingSteps1.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps1.verifyAllocatedHearingListedFromAPI(nextHearings.getHearingData().get(0));

        // Second hearing share and extend to next hearing
        final UUID nextHearingId = nextHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(nextHearings.getHearingData().get(0));
        listNextHearingSteps2.whenUpdateRelatedHearingSubmittedForListing(nextHearingId, secondHearings);
        listNextHearingSteps1.verifyUpdateRelatedHearingRequestedInActiveMQ(nextHearingId);
        listNextHearingSteps1.verifyCasesAddedToAllocatedHearingFromApi(nextHearings, secondHearings);

        // Amend Reshare First Hearing
        listNextHearingSteps1.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps1.verifyCasesAreInAllocatedHearingFromApi(nextHearings, secondHearings);
        listNextHearingSteps1.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(nextHearings.getHearingData().get(0).getId(), nextHearings);
    }

    @Test
    public void shouldRemoveOffencesFromNextHearingWhenNextHearingIsExistingHearing(){

        HearingsData secondHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(secondHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        // First hearing created
        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        // Second hearing share and extend to first hearing
        final UUID nextHearingId = firstHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenUpdateRelatedHearingSubmittedForListing(nextHearingId, secondHearings);
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.verifyCasesAddedToAllocatedHearingFromApi(firstHearings, secondHearings);

        // New Offence Added to case of next hearing
        DefendantData defendantData = secondHearings.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = secondHearings.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = secondHearings.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);
        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null);
        final OffencesForDefendantUpdated newOffences = steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly();
        final String newOffenceId = newOffences.getAddedOffences().stream().flatMap(a -> a.getOffences().stream()).map(Offence::getId).map(UUID::toString).toList().get(0);
        listNextHearingSteps1.verifyOffenceAddedToAllocatedHearingFromApi(firstHearings, newOffenceId);

        // Amend Reshare second Hearing
        listNextHearingSteps1.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps1.verifyCasesAreInAllocatedHearingFromApi(firstHearings, secondHearings);
        listNextHearingSteps1.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(nextHearingId, secondHearings, newOffenceId);

        HearingsData nextHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListNextHearingSteps listNextHearingStepsNew = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingStepsNew.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingStepsNew.verifyPublicOffencesMovedToHearingInActiveMQ();
    }

    @Test
    public void ShouldRemoveOffencesIfNextHearingWasExtended(){
       // GIVEN first hearing
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(randomUUID(), null, STRING.next(), randomUUID(), null, JurisdictionType.MAGISTRATES.name(), JurisdictionType.MAGISTRATES.name(),
                null, null);

        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData);
        final ListCourtHearingSteps firstHearingsSteps = new ListCourtHearingSteps(firstHearings);
        firstHearingsSteps.whenCaseIsSubmittedForListing();

        // GIVEN unallocated Hearing
        final HearingsData unallocatedHearing = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(unallocatedHearing);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        // When first hearing resulted
        HearingsData nextHearing = HearingsData.nextAllocatedHearingsData(firstHearings.getHearingData());
        final ListNextHearingSteps listNextHearingSteps1 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps1.whenNextHearingSubmittedForListing(nextHearing);
        listNextHearingSteps1.verifySingleHearingListedFromAPI(nextHearing.getHearingData().get(0), true);

        final UUID nextHearingId = nextHearing.getHearingData().get(0).getId();
        final UUID unAllocatedHearingId = unallocatedHearing.getHearingData().get(0).getId();

        // When unallocated Hearing extend to next hearing
        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(HearingsData.hearingsData(unAllocatedHearingId));
        listCourtHearingSteps2.extendHearing(unAllocatedHearingId,nextHearingId);
        listCourtHearingSteps2.verifyPublicEventHearingConfirmedAndExtendHearingFromProgression(nextHearingId, unAllocatedHearingId);
        listNextHearingSteps1.verifyCasesAddedToHearingFromApi(nextHearing, unallocatedHearing, true);

        // When first hearing is amended and reshared
        final ListNextHearingSteps listNextHearingSteps2 = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        listNextHearingSteps2.whenDeleteNextHearingSubmittedForListing();
        // Then next hearing has only Cases of unallocated hearing
        pollForHearing(nextHearing.getHearingData().get(0).getCourtCentreId().toString(), true, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[?(@.id == '" + nextHearingId + "')].id",
                        contains(nextHearingId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + nextHearingId + "')].listedCases.length()",
                        contains(2)),
                withJsonPath("$.hearings[?(@.id == '" + nextHearingId + "')].listedCases[?(@.id == '" +unallocatedHearing.getHearingData().get(0).getListedCases().get(0).getCaseId().toString() + "')].id",
                        contains(unallocatedHearing.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                withJsonPath("$.hearings[?(@.id == '" + nextHearingId + "')].listedCases[?(@.id == '" +unallocatedHearing.getHearingData().get(0).getListedCases().get(1).getCaseId().toString() + "')].id",
                        contains(unallocatedHearing.getHearingData().get(0).getListedCases().get(1).getCaseId().toString()))
        });
        listNextHearingSteps2.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(nextHearingId,firstHearings );
    }


}

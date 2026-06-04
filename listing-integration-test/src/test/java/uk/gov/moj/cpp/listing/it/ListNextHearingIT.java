package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.notHmiEnabledHearingsData;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;

import com.google.common.collect.ImmutableMap;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.courts.OffencesForDefendantUpdated;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class ListNextHearingIT extends AbstractIT {

    @Test
    void shouldListNextHearings() {
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
    void shouldDeleteOldNextHearingsAndListNextHearings() {

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
    void shouldDeletePreviousHearingsAndCreateNextHearingRequested() {

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
    void shouldDeleteOldScheduledNextHearingsAndScheduledNextHearings() {

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
    void shouldDeleteOldRelatedtHearingsAndUpdateRelatedHearings() {

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
    void shouldAddCasetoExistingHearingforAdHocHearing() {
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
    void shouldDeleteOldAllocatedRelatedHearingsAndUpdateRelatedHearings() {

        final HearingsData oldNextHearings = hearingsDataWithAllocationDataAndJudiciary();

        final HearingsData firstHearings = hearingsDataWithAllocationDataAndJudiciary();
        final HearingsData existedHearings = hearingsDataWithAllocationDataAndJudiciary();

        final ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(firstHearings);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();
        listCourtHearingSteps1.verifyHearingListedFromAPI(ALLOCATED);

        final ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(existedHearings);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps2.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps2.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();
        listCourtHearingSteps2.verifyHearingListedFromAPI(ALLOCATED);


        final UUID existedHearingId = existedHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0));

        // First iteration: add cases to existing hearing then delete
        listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps.verifyCasesAddedToAllocatedHearingFromApi(existedHearings, oldNextHearings);

        listNextHearingSteps.clearStaleAllocatedHearingMessages();
        listNextHearingSteps.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);

        // Second iteration: re-add cases to existing hearing then delete again
        listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(existedHearingId, oldNextHearings);
        listNextHearingSteps.verifyCasesAddedToAllocatedHearingFromApi(existedHearings, oldNextHearings);

        listNextHearingSteps.clearStaleAllocatedHearingMessages();
        listNextHearingSteps.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(existedHearingId, oldNextHearings);

    }

    @Test
    void shouldRemoveOffencesFromNextHearingWhenFirstSeededHearingAmended(){

        final HearingsData nextHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final HearingsData secondHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();

        // First hearing created
        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
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
        // Drain the offences-removed events produced by the earlier update-related-hearing step so the
        // verify below reads the DELETE's event, not the UPDATE's (same hearingId, so id-filter alone can't disambiguate).
        listNextHearingSteps1.clearStaleAllocatedHearingMessages();
        listNextHearingSteps1.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps1.verifyCasesAreInAllocatedHearingFromApi(nextHearings, secondHearings);
        listNextHearingSteps1.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(nextHearings.getHearingData().get(0).getId(), nextHearings);
    }

    @Test
    void shouldRemoveOffencesFromNextHearingWhenNextHearingIsExistingHearing(){

        HearingsData secondHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(secondHearings);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        // First hearing created
        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        // Second hearing share and extend to first hearing
        final UUID nextHearingId = firstHearings.getHearingData().get(0).getId();
        final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
        // Clear stale hearing-added-to-case messages from initial listings before the update
        listNextHearingSteps.clearHearingAddedToCaseMessages();
        listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(nextHearingId, secondHearings);
        listNextHearingSteps.verifyCasesAddedToAllocatedHearingFromApi(firstHearings, secondHearings);
        // Wait for case event streams to be updated with the new hearing ID before publishing offence event
        listNextHearingSteps.waitForCaseEventStreamUpdate(nextHearingId);

        // New Offence Added to case of next hearing
        DefendantData defendantData = secondHearings.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = secondHearings.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = secondHearings.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);
        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null, false);
        final OffencesForDefendantUpdated newOffences = steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly();
        final String newOffenceId = newOffences.getAddedOffences().stream().flatMap(a -> a.getOffences().stream()).map(Offence::getId).map(UUID::toString).toList().get(0);
        listNextHearingSteps.verifyOffenceAddedToAllocatedHearingFromApi(firstHearings, newOffenceId);

        // Amend Reshare second Hearing
        listNextHearingSteps.clearStaleAllocatedHearingMessages();
        listNextHearingSteps.whenDeleteNextHearingSubmittedForListing();
        listNextHearingSteps.verifyCasesAreInAllocatedHearingFromApi(firstHearings, secondHearings);
        listNextHearingSteps.verifyPublicOffencesRemovedFromExistingAllocatedHearingInActiveMQ(nextHearingId, secondHearings, newOffenceId);

        HearingsData nextHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        listNextHearingSteps.whenNextHearingSubmittedForListing(nextHearings);
        listNextHearingSteps.verifyPublicOffencesMovedToHearingInActiveMQ();
    }

    @Test
    void ShouldRemoveOffencesIfNextHearingWasExtended(){
       // GIVEN first hearing
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(randomUUID(), null, STRING.next(), randomUUID(), null, JurisdictionType.MAGISTRATES.name(), JurisdictionType.MAGISTRATES.name(),
                null, null);

        final HearingsData firstHearings = HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData);
        final ListCourtHearingSteps firstHearingsSteps = new ListCourtHearingSteps(firstHearings);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", firstHearingsSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(firstHearingsSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
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

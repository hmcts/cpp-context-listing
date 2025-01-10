package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubUpdateAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtRoom;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.DeleteCourtApplicationHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.SequenceHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleTypeData;
import uk.gov.moj.cpp.listing.steps.data.SequenceHearingData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")

public class HearingIT extends AbstractIT {
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");

    }

    @Test
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEvent() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        listCourtHearingSteps.verifyPrivateEventRequestedHearingFromStagingHmiNotInActiveMQ();

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedResultsInAllocationInMQ();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        updateHearingSteps.verifyPrivateEventUpdatedHearingInStagingHmiNotInActiveMQ();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void assignPublicListNoteInAllocatedListing() {
        stubGetAvailableHearingSlots();
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
        updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
        updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingConfirmedInPublicMQ();
    }


    @Test
    public void changePublicListNoteInAllocatedListing() {
        stubGetAvailableHearingSlots();
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
        updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
        updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingConfirmedInPublicMQ();


        final UpdatedHearingData updatedHearingDataForChangeingVideoLinkDetails = UpdatedHearingData.updatedHearingDataWithVideoLink(updatedHearingDataForAllocation);

        final UpdateHearingSteps updateHearingSteps1 = new UpdateHearingSteps(hearingsData, updatedHearingDataForChangeingVideoLinkDetails);
        updateHearingSteps1.whenHearingIsUpdatedForListingWithPublicListNote();
        updateHearingSteps1.verifyHearingUpdatedResultsForPublicListNoteInMQ();
        updateHearingSteps1.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
    }

    @Test
    public void removePublicListNoteInAllocatedListing() {
        stubGetAvailableHearingSlots();
        final HearingsData hearingsData = HearingsData.hearingsData();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
        updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
        updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingConfirmedInPublicMQ();


        final UpdatedHearingData updatedHearingDataForChangingVideoLinkDetails = UpdatedHearingData.updatedHearingDataWithoutVideoLink(updatedHearingDataForAllocation);

        final UpdateHearingSteps updateHearingSteps1 = new UpdateHearingSteps(hearingsData, updatedHearingDataForChangingVideoLinkDetails);
        updateHearingSteps1.whenHearingIsUpdatedForListing();
        updateHearingSteps1.verifyHearingUpdatedResultsForRemovingPublicListNoteInMQ();
    }


    @Test
    public void shouldUpdateHearingResultsInPartialAllocatedListing() {
        stubGetAvailableHearingSlots();
        HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        final UUID removedOffenceId = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(1).getOffenceId();
        hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().remove(1);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListingWithProsecutionCases();
        updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        updateHearingSteps.verifyHearingAllocatedEventNotExistsRemovedOffence(removedOffenceId);
        updateHearingSteps.verifyHearingPartiallyEventNotExistsRemovedOffence(removedOffenceId);
        updateHearingSteps.verifyPublicHearingChangesSaved();

    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndUpdateSlotDetails() {
        final UUID courtCentreId = randomUUID();
        stubGetReferenceDataCourtCentreById(courtCentreId);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndNotUpdateSlotDetails() {

        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedResultsInAllocationInMQ();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        updateHearingSteps.verifyPublicHearingChangesSaved();

    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndReturnSlotDetailsForAdjournmentHearing() {
        stubUpdateAvailableHearingSlotsService();
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndReturnSlotDetailsForAdjournmentHearingWithoutJudiciary() throws IOException {
        stubUpdateAvailableHearingSlotsService();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndAdjournmentFromDateWithoutJudiciary(1);

        final JsonObject getHearingSlotsJsonObject = stubGetAvailableHearingSlotsWithQueryParams(false,
                hearingsData.getHearingData().get(0).getCourtRoomId().toString(),
                "C55BN00",
                hearingsData.getHearingData().get(0).getHearingStartDate().toString(),
                hearingsData.getHearingData().get(0).getHearingStartDate().toString());

        final List<JudicialRoleData> judicialRoleDataList = getJudicialRoleDataFromRotaSLHearingSlots(getHearingSlotsJsonObject);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingWithJudicialId(judicialRoleDataList.get(0).getJudicialId());
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        listCourtHearingSteps.verifyJudiciaryAssignedEventWithRotaSLJudiciaries(judicialRoleDataList);
    }

    @Test
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEventWithNoJudiciary() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithoutJudiciary(hearingsData.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingConfirmedInPublicMQHasNoJudiciary();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndRaisesPublicHearingUpdatedEvent() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);


        final UpdatedHearingData updatedHearingDataForUnallocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedResultsInMQ();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyVacatedTrialUpdatedInPublicMQ(ALLOCATED, false);
        updateHearingSteps.verifyHearingUpdatedInPublicMQ();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndUpdateSlotDetails() {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithNonDefaultDaysWithAdditionalFields(hearingId);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabled();
        updateHearingSteps.verifyHearingUpdatedResultsForSlotUpdateInMQ();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingDaysWhenQueryFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void updateHearingResultsWhenMultipleOffencesSplitToMultipleHearings() {
        final HearingsData hearingsData = HearingsData.singleHearingDataSingleCaseMultipleOffences();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForSplit = UpdatedHearingData.updatedHearingDataForAllocationWithJurisdictionType(hearingId, CROWN_JURISDICTION);
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForSplit);
        updateHearingSteps.whenHearingIsUpdatedFromHmiWithoutCourtRoomSelection();
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabledWithoutCourtRoomSelection();
        updateHearingSteps.verifyHearingDaysChangedEventForOneDayOnlyWithoutCourtRoomSelection();
        updateHearingSteps.verifyHearingDaysChangedForHearingEvent();
        updateHearingSteps.verifyPublicHearingChangesSaved();

    }

    @Test
    public void updateHearingResultsWhenUnallocatedDefendentsSplitToMultipleHearings() {
        final HearingsData hearingsData = HearingsData.singleHearingDataSingleCaseMultipleDefendents();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData, true);
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID hearingId =  hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForSplit = UpdatedHearingData.updatedHearingDataForAllocationWithDefendant(hearingId, hearingsData);
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForSplit, true);
        updateHearingSteps.whenHearingIsUpdatedForListingWithProsecutionCasesDefendantsSplit();
        updateHearingSteps.verifyHearingListedFromAPI(true, allOf(
                withJsonPath("$.hearings[0].id", equalTo(hearingId.toString())),
                withJsonPath("$.hearings[0].allocated", equalTo(true)),
                withJsonPath("$.hearings[0].hearingDays[0].courtRoomId", Objects::nonNull),
                withJsonPath("$.hearings[0].listedCases[0].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants.length()", equalTo(1)),
                 withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences.length()", equalTo(2)),
                 withJsonPath("$.hearings[0].listedCases[0].defendants[0].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())),
                 withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())),
                 withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[1].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(1).getOffenceId().toString()))
        ), null, null);
    }

    @Test
    public void updateHearingResultsWhenCourtRoomNotSelected() {
        final HearingsData hearingsData = HearingsData.singleHearingDataForHMI();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UUID courtCenterId = hearingsData.getHearingData().get(0).getCourtCentreId();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithNonDefaultDaysWithoutCourtRoomSelection(hearingId, courtCenterId);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedFromHmiWithoutCourtRoomSelection();
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabledWithoutCourtRoomSelection();
        updateHearingSteps.verifyHearingDaysChangedEventForOneDayOnlyWithoutCourtRoomSelection();
        updateHearingSteps.verifyHearingDaysChangedForHearingEvent();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void updateHearingResultsWhenCourtRoomNotSelectedForHearingListed() {
        final HearingsData hearingsData = HearingsData.singleHearingDataForHMI();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        listCourtHearingSteps.verifyHearingListedPublicEvent();
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndUpdateSlotDetailsFromHmi() {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithNonDefaultDays(hearingId);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedFromHmi();
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedResultsForSlotUpdateInMQ();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingDaysWhenQueryFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }
    @Disabled("will be handled with DD-34779")
    @Test
    public void ShouldUnAllocateHearingWhenHmiRemoveCourtRoom() {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedFromHmi(Arrays.asList("courtRoomId"));
        updateHearingSteps.verifyHearingListedFromAPI(UNALLOCATED, allOf(
                withJsonPath("$.hearings[0].id", equalTo(updatedHearingDataForAllocation.getHearingId().toString())),
                withJsonPath("$.hearings[0].allocated", equalTo(false)),
                withoutJsonPath("$.hearings[0].hearingDays[0].courtRoomId")), null, null);
    }

    @Disabled("will be handled with DD-34779")
    @Test
    public void ShouldUnScheduledHearingWhenHmiRemoveHearingDate() {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedFromHmi(Arrays.asList("courtRoomId", "startDate", "endDate"));
        updateHearingSteps.verifyStartDateRemovedEvent();
        updateHearingSteps.verifyEndDateRemovedEvent();
        updateHearingSteps.verifyCourtRoomRemovedEvent();
        updateHearingSteps.verifyEmptyHearingDaysChangedEventInActiveMQ();
        updateHearingSteps.verifyUnallocatedHearingEvent();
    }
    @Disabled("will be handled with DD-34779")
    @Test
    public void shouldUnAllocatedAndWeekCommercingHearingWhenHmiConvertFixedDateToWeekCommercing() {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataWithWeekCommencingDate(hearingsData.getHearingData().get(0), LocalDate.now(), LocalDate.now().plusWeeks(1), 1);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedFromHmi(Arrays.asList("startDate", "endDate", "courtRoomId"));
        updateHearingSteps.verifyStartDateRemovedEvent();
        updateHearingSteps.verifyEndDateRemovedEvent();
        updateHearingSteps.verifyCourtRoomRemovedEvent();
        updateHearingSteps.verifyEmptyHearingDaysChangedEventInActiveMQ();
        updateHearingSteps.verifyUnallocatedHearingEvent();
        updateHearingSteps.verifyWeekCommercingDateChangedEvent();
        updateHearingSteps.verifyHearingListedFromAPI(UNALLOCATED, allOf(
                withJsonPath("$.hearings[0].id", equalTo(updatedHearingDataForAllocation.getHearingId().toString())),
                withJsonPath("$.hearings[0].allocated", equalTo(false)),
                withoutJsonPath("$.hearings[0].hearingDays[0]"),
                withJsonPath("$.hearings[0].weekCommencingEndDate", is(LocalDate.now().plusWeeks(1).toString())),
                withJsonPath("$.hearings[0].weekCommencingStartDate", is(LocalDate.now().toString())),
                withJsonPath("$.hearings[0].weekCommencingDurationInWeeks", is("1")),
                withoutJsonPath("$.hearings[0].startDate"),
                withoutJsonPath("$.hearings[0].endDate")
        ), LocalDate.now().toString(), LocalDate.now().plusWeeks(1).toString());
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndUpdateSlotDetailsWithNoJudiciaryAndGetJudiciaryInfoFromRotaSL() throws IOException {
        final UUID courtCentreId = randomUUID();
        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithoutJudiciary(hearingId);

        final String sessionStartDate = updatedHearingDataForAllocation.getStartDate();

        final JsonObject getHearingSlotsJsonObject = stubGetAvailableHearingSlotsWithQueryParams(false,
                updatedHearingDataForAllocation.getCourtRoomId().toString(),
                "C55BN00",
                sessionStartDate,
                sessionStartDate);

        final List<JudicialRoleData> judicialRoleDataList = getJudicialRoleDataFromRotaSLHearingSlots(getHearingSlotsJsonObject);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyJudiciaryChangedEventWithRotaSLJudiciaries(judicialRoleDataList);
        updateHearingSteps.verifyHearingDaysWhenQueryFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    private List<JudicialRoleData> getJudicialRoleDataFromRotaSLHearingSlots(final JsonObject getHearingSlotsJsonObject) {
        final List<JudicialRoleData> judicialRoleDataList = new ArrayList<>();
        getHearingSlotsJsonObject.getJsonArray("hearingSlots")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(hearingSlotJsonObject -> {
                    final JsonArray judiciariesJsonArray = hearingSlotJsonObject.getJsonArray("judiciaries");
                    judiciariesJsonArray.stream()
                            .map(JsonObject.class::cast)
                            .forEach(rotaSlJudiciaryJsonObject ->
                                    judicialRoleDataList.add(
                                            new JudicialRoleData(Optional.of(rotaSlJudiciaryJsonObject.getBoolean("benchChairman")),
                                                    Optional.of(rotaSlJudiciaryJsonObject.getBoolean("deputy")),
                                                    UUID.fromString(rotaSlJudiciaryJsonObject.getString("judiciaryId")),
                                                    null,
                                                    new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE"))
                                    )
                            );
                });
        return judicialRoleDataList;
    }

    @Test
    public void updateAllocatedHearingWithNoCourtRoomResultsInUnallocatedListing() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataWithNoCourtRoom = UpdatedHearingData.updatedHearingDataWithNoCourtRoom(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithNoCourtRoom);
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabled();
        updateHearingSteps.verifyHearingUpdatedWithNoCourtRoomResultsInUnallocationInMQ();
        updateHearingSteps.verifyHearingUpdatedWithNoCourtRoomAndUnallocatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void updateAllocatedHearingWithNoEndDateResultsInUnallocatedListing() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataWithNoEndDate = UpdatedHearingData.updatedHearingDataWithNoEndDate(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithNoEndDate);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedWithNoEndDateResultsInUnallocationInMQ();
        updateHearingSteps.verifyHearingUpdatedWithNoEndDateAndUnallocatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void hearingCanBeSearchedForUsingDifferentCombinationsOfParameters() {

        stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataForUnallocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingFoundByAllocatedFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDate();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndSearchDate();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndCourtRoomFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdAndHearingTypFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndIdHearingTypAndJurisdictionTypeFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();
    }

    @Test
    public void updateJudicialRolesForHearings() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary);
        updateHearingSteps.whenJudiciaryIsChangedForHearings();
        updateHearingSteps.verifyHearingWithUpdatedJudiciaryInMQ();
        updateHearingSteps.verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI();
    }

    @Test
    public void sequenceHearingDays() {
        final HearingsData hearingsData = HearingsData.singleHearingData();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        final SequenceHearingData sequenceHearingData = new SequenceHearingData(updatedHearingDataForAllocation);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingFoundByAllocatedFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();

        final SequenceHearingSteps sequenceHearingSteps = new SequenceHearingSteps(sequenceHearingData);
        sequenceHearingSteps.whenHearingDaysAreSequenced();
        sequenceHearingSteps.verifyHearingWithSequencedDaysInMQ();
        sequenceHearingSteps.verifyHearingWithSequencedDaysInPublicMQ();
        sequenceHearingSteps.verifyHearingDaysAreSequencedFromAPI();
        sequenceHearingSteps.verifyHearingUpdatedInPublicMQ();
    }

    @Test
    public void shouldUpdateWeekCommencing() {
        final HearingsData hearingsData = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 1);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        listCourtHearingSteps.verifyHearingListedWithWeekCommencingFromAPI(UNALLOCATED, LocalDate.now(), 1);
    }

    @Test
    public void shouldHideUnallocatedHearingOnceVacatedFromUnallocatedLists() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacated();

        vacatingTrialSteps.verifyListingTrialVacatedEvent(false);
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        listCourtHearingSteps.verifyHearingIsNotListed(false);
    }

    @Test
    public void shouldFindUnallocatedHearingWithCaseUrn() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedWithAnyAllocationFromAPI(UNALLOCATED);
    }

    @Test
    public void shouldFindHearingForCotr() {
        final HearingsData hearingsData = HearingsData.trialHearingsData();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final String startDate = nonNull(hearingsData.getHearingData().get(0).getHearingStartDate()) ? hearingsData.getHearingData().get(0).getHearingStartDate().toString() : null;
        final String endDate = nonNull(hearingsData.getHearingData().get(0).getHearingStartDate()) ? hearingsData.getHearingData().get(0).getHearingStartDate().toString() : null;

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedForCotr(courtCentreId, startDate, endDate);
    }

    @Test
    public void shouldDeleteCourtApplicationHearing() {
        final HearingsData hearingsData = HearingsData.hearingsData();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final DeleteCourtApplicationHearingSteps deleteCourtApplicationHearingSteps = new DeleteCourtApplicationHearingSteps();
        final String hearingId = hearingsData.getHearingData().get(0).getId().toString();
        final String courtCenterId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final String applicationId = hearingsData.getHearingData().get(0).getCourtApplications().get(0).getId().toString();
        deleteCourtApplicationHearingSteps.whenRaisedCourtApplicationHearingPublicEvent(hearingId, applicationId);
        deleteCourtApplicationHearingSteps.verifyCourtApplicationHearingDeletedPrivateEvent(hearingId);
        deleteCourtApplicationHearingSteps.verifyOldHearingDeleted(hearingId, courtCenterId );
    }

}

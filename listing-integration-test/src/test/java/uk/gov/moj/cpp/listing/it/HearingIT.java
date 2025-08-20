package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.listing.it.util.HearingHelper.pollForHearingById;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndAdjournmentFromDateWithoutJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.singleHearingData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.singleHearingDataForHMI;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.singleHearingDataSingleCaseMultipleDefendents;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.singleHearingDataSingleCaseMultipleOffences;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.trialHearingsData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocation;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocationWithDefendant;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocationWithJurisdictionType;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocationWithNonDefaultDays;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocationWithNonDefaultDaysWithAdditionalFields;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocationWithNonDefaultDaysWithoutCourtRoomSelection;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocationWithoutJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithNoCourtRoom;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithVideoLink;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithoutVideoLink;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithJudiciary;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedulesWithJudiciaries;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubUpdateAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtRoom;

import uk.gov.moj.cpp.listing.steps.DeleteCourtApplicationHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.SequenceHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleTypeData;
import uk.gov.moj.cpp.listing.steps.data.SequenceHearingData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")

class HearingIT extends AbstractIT {
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";


    @Test
    void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEvent() throws IOException {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventHearingConfirmed();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
    }

    @Test
    void assignPublicListNoteInAllocatedListing() throws IOException {
        stubGetAvailableHearingSlots();
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
        updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();

        final UpdatedHearingData updatedHearingDataForChangingVideoLinkDetails = updatedHearingDataWithVideoLink(updatedHearingDataForAllocation);

        final UpdateHearingSteps updateHearingSteps1 = new UpdateHearingSteps(hearingsData, updatedHearingDataForChangingVideoLinkDetails);
        updateHearingSteps1.whenHearingIsUpdatedForListingWithPublicListNote();
        updateHearingSteps1.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();

        final UpdatedHearingData updatedHearingDataForRemovingVideoLinkDetails = updatedHearingDataWithoutVideoLink(updatedHearingDataForAllocation);

        final UpdateHearingSteps updateHearingSteps2 = new UpdateHearingSteps(hearingsData, updatedHearingDataForRemovingVideoLinkDetails);
        updateHearingSteps2.whenHearingIsUpdatedForListing();
        updateHearingSteps2.verifyPrivateEventPublicListNoteRemoved();
    }

    @Test
    void shouldUpdateHearingResultsInPartialAllocatedListing() throws IOException {
        stubGetAvailableHearingSlots();
        HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().remove(1);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListingWithProsecutionCases();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();

    }


    @Test

    void shouldRaisePublicHearingConfirmedPublicEventAndReturnSlotDetailsForAdjournmentHearing() {
        stubUpdateAvailableHearingSlotsService();
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    void shouldRaisePublicHearingConfirmedPublicEventAndReturnSlotDetailsForAdjournmentHearingWithoutJudiciary() throws IOException {
        stubUpdateAvailableHearingSlotsService();

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndAdjournmentFromDateWithoutJudiciary(1);
        final JsonObject getHearingSlotsJsonObject = stubGetAvailableHearingSlotsWithQueryParams(false,
                hearingsData.getHearingData().get(0).getCourtRoomId().toString(),
                "C55BN00",
                hearingsData.getHearingData().get(0).getHearingStartDate().toString(),
                hearingsData.getHearingData().get(0).getHearingStartDate().toString());

        final List<JudicialRoleData> judicialRoleDataList = getJudicialRoleDataFromRotaSLHearingSlots(getHearingSlotsJsonObject);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessionsWithJudiciary(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime(),20,judicialRoleDataList);

        listCourtHearingSteps.whenCaseIsSubmittedForListingWithJudicialId(judicialRoleDataList.get(0).getJudicialId());
        verifyJudiciaryAssignedToAllocatedHearingFromAPI(hearingsData.getHearingData().get(0).getId(), judicialRoleDataList);
    }

    @Test
    void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEventWithNoJudiciary() throws IOException {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocationWithoutJudiciary(hearingsData.getHearingData().get(0).getId());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyPublicEventHearingConfirmed_hasNoJudiciary();
    }

    @Test
    void updateHearingResultsInUpdatedListingAndRaisesPublicHearingUpdatedEvent() throws IOException {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataForUnAllocation = updatedHearingData(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventVacatedTrialUpdated(ALLOCATED, false);
        updateHearingSteps.verifyPublicEventHearingUpdated();
    }

    @Test
    void shouldUpdateMultipleHearingsWithAllocationAndRaisesPublicEventWithFailures() throws IOException {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataForUnallocation = updatedHearingData(hearingsData.getHearingData().get(0));
        final UpdatedHearingData updatedHearingDataForUnallocation1 = updatedHearingData(hearingsData.getHearingData().get(1));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenMultiHearingsUpdatedForListing(updatedHearingDataForUnallocation1);
        updateHearingSteps.verifyPublicEventHearingsUpdateCompleted();
    }

    @Test
    void updateHearingResultsInUpdatedListingAndUpdateSlotDetails() throws IOException {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocationWithNonDefaultDaysWithAdditionalFields(hearingId);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabled();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingDaysWhenQueryingFromAPI();
    }

    @Test
    void updateHearingResultsWhenMultipleOffencesSplitToMultipleHearings() throws IOException {
        final HearingsData hearingsData = singleHearingDataSingleCaseMultipleOffences();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForSplit = updatedHearingDataForAllocationWithJurisdictionType(hearingId, CROWN_JURISDICTION);
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForSplit);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabledWithoutCourtRoomSelection();
        updateHearingSteps.verifyPublicEventHearingDaysChangedForHearing();

    }

    @Test
    void updateHearingResultsWhenUnallocatedDefendantsSplitToMultipleHearings() throws IOException {
        final HearingsData hearingsData = singleHearingDataSingleCaseMultipleDefendents();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData, true);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForSplit = updatedHearingDataForAllocationWithDefendant(hearingId, hearingsData);
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForSplit, true);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListingWithProsecutionCasesDefendantsSplit();

        updateHearingSteps.verifyHearingListedFromAPI(true, new Matcher[]{
                withJsonPath("$.hearings[0].id", equalTo(hearingId.toString())),
                withJsonPath("$.hearings[0].allocated", equalTo(true)),
                withJsonPath("$.hearings[0].hearingDays[0].courtRoomId", Objects::nonNull),
                withJsonPath("$.hearings[0].listedCases[0].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants.length()", equalTo(1)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences.length()", equalTo(2)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[1].id", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(1).getOffenceId().toString()))
        }, null, null);
    }

    @Test
    void updateHearingResultsWhenCourtRoomNotSelected() {
        final HearingsData hearingsData = singleHearingDataForHMI();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        listCourtHearingSteps.verifyPublicEventHearingListed();

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UUID courtCenterId = hearingsData.getHearingData().get(0).getCourtCentreId();
        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocationWithNonDefaultDaysWithoutCourtRoomSelection(hearingId, courtCenterId);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabledWithoutCourtRoomSelection();
        updateHearingSteps.verifyPublicEventHearingDaysChangedForHearing();
    }

    @Test
    void updateHearingResultsInUpdatedListingAndUpdateSlotDetailsFromHmi() throws IOException {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocationWithNonDefaultDays(hearingId);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyHearingDaysWhenQueryingFromAPI();
    }

    @Test
    void updateHearingResultsInUpdatedListingAndUpdateSlotDetailsWithNoJudiciaryAndGetJudiciaryInfoFromRotaSL() throws IOException {
        final UUID courtCentreId = randomUUID();
        stubGetReferenceDataCourtCentreById(courtCentreId);


        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);


        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocationWithoutJudiciary(hearingId);

        final String sessionStartDate = updatedHearingDataForAllocation.getStartDate();

        final JsonObject getHearingSlotsJsonObject = stubGetAvailableHearingSlotsWithQueryParams(false,
                updatedHearingDataForAllocation.getCourtRoomId().toString(),
                "C55BN00",
                sessionStartDate,
                sessionStartDate);

        final List<JudicialRoleData> judicialRoleDataList = getJudicialRoleDataFromRotaSLHearingSlots(getHearingSlotsJsonObject);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubListHearingInCourtSessionsWithMultipleSchedulesWithJudiciaries(updateHearingSteps.getUpdatedHearingData(),judicialRoleDataList);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        verifyJudiciaryAssignedToAllocatedHearingFromAPI(updatedHearingDataForAllocation.getHearingId(), judicialRoleDataList);
        updateHearingSteps.verifyHearingDaysWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
    }

    @Test
    @Disabled("Will be fixed with SPRDT-179")
    void shouldAllocatingHearingForMagsWithoutCourtScheduleIdAndAutomaticallyUpdateMissingCourtScheduleId() {
        final UUID courtCentreId = randomUUID();
        final UUID courtScheduleId1 = UUID.fromString("d4b9299c-c6c6-3747-8dac-01ca82239c27");

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final UUID hearingId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId();
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        stubListHearingInCourtSessions(hearingId.toString(), courtScheduleId1.toString(), hearingStartTime);
        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId1.toString());
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubGetReferenceDataCourtCentreById(courtCentreId);
        stubProvisionalBookingWithCustomParams(stubParams);

        listCourtHearingSteps.whenCaseIsSubmittedForListing();

        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<>() {{
            put(hearingDate.toString(), courtScheduleId1.toString());
        }};

        listCourtHearingSteps.verifyHearingListedWithHearingDaysCourtSchedule(ALLOCATED, courtRoomSchedules.keySet().toArray(String[]::new), courtRoomSchedules.values().toArray(String[]::new));
        listCourtHearingSteps.verifyHearingDayCourtScheduleCarriedOverToCommand(hearingDate, courtScheduleId1);

        UUID updatedCourtScheduleId = randomUUID();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedHearingData updatedHearingData = updatedHearingData(hearingData, hearingDate);
        UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        listCourtHearingSteps.verifyHearingUpdatedWithHearingDaysCourtSchedule(updatedHearingData);
        listCourtHearingSteps.verifyHearingDayCourtScheduledUpdated(updatedCourtScheduleId);
        updateHearingSteps.verifyPublicEventHearingDaysChangedForHearing();
        listCourtHearingSteps.verifyHearingDayCourtScheduleCarriedOverToCommand(hearingDate, updatedCourtScheduleId);
    }

    @Test
    void updateAllocatedHearingWithNoCourtRoomResultsInUnallocatedListing() throws IOException {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataWithNoCourtRoom = updatedHearingDataWithNoCourtRoom(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithNoCourtRoom);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListingHmiEnabled();
        updateHearingSteps.verifyHearingUpdatedWithNoCourtRoomAndUnallocatedWhenQueryingFromAPI();
    }

    @Test
    void hearingCanBeSearchedForUsingDifferentCombinationsOfParameters() throws IOException {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", hearingsData.getHearingData().get(0).getCourtCentreId().toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataForUnAllocation = updatedHearingData(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnAllocation);
        stubGetAvailableHearingSlotsWithQueryParams(updateHearingSteps.getUpdatedHearingData());
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingFoundByAllocatedFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDate();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndSearchDate();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndCourtRoomFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdAndHearingTypFromAPI();
        updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreAndIdHearingTypAndJurisdictionTypeFromAPI();
    }

    @Test
    void updateJudicialRolesForHearings() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "d4b9299c-c6c6-3747-8dac-01ca82239c27",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        final UUID courtCentreId = randomUUID();
        final UUID courtScheduleId1 = UUID.fromString("d4b9299c-c6c6-3747-8dac-01ca82239c27");
        final UUID hearingId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId();
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId1.toString());
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary);
        updateHearingSteps.whenJudiciaryIsChangedForHearings();
        updateHearingSteps.verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI();
    }

    @Test
    void sequenceHearingDays() {
        final HearingsData hearingsData = singleHearingData();
        final UpdatedHearingData updatedHearingDataForAllocation = updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        final SequenceHearingData sequenceHearingData = new SequenceHearingData(updatedHearingDataForAllocation);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation);
        stubListHearingInCourtSessionsWithMultipleSchedules(updateHearingSteps.getUpdatedHearingData());
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingFoundByAllocatedFromAPI();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();

        final SequenceHearingSteps sequenceHearingSteps = new SequenceHearingSteps(sequenceHearingData);
        sequenceHearingSteps.whenHearingDaysAreSequenced();
        sequenceHearingSteps.verifyHearingDaySequencedPublicEvent();
        sequenceHearingSteps.verifyHearingDaysAreSequencedFromAPI();
        sequenceHearingSteps.verifyPublicEventHearingUpdated();
    }

    @Test

    void shouldUpdateWeekCommencing() {
        final HearingsData hearingsData = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 1);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        listCourtHearingSteps.verifyHearingListedWithWeekCommencingFromAPI(UNALLOCATED, LocalDate.now(), 1);
    }

    @Test
    void shouldHideUnallocatedHearingOnceVacatedFromUnallocatedLists() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();

        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        listCourtHearingSteps.verifyHearingIsNotListed(false);
    }

    @Test
    void shouldFindUnallocatedHearingWithCaseUrn() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedWithAnyAllocationFromAPI(UNALLOCATED);
    }

    @Test
    void shouldFindHearingForCotr() {
        final HearingsData hearingsData = trialHearingsData();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final String startDate = nonNull(hearingsData.getHearingData().get(0).getHearingStartDate()) ? hearingsData.getHearingData().get(0).getHearingStartDate().toString() : null;
        final String endDate = nonNull(hearingsData.getHearingData().get(0).getHearingStartDate()) ? hearingsData.getHearingData().get(0).getHearingStartDate().toString() : null;

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedForCotr(courtCentreId, startDate, endDate);
    }

    @Test
    void shouldDeleteCourtApplicationHearing() {
        final HearingsData hearingsData = hearingsData();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final DeleteCourtApplicationHearingSteps deleteCourtApplicationHearingSteps = new DeleteCourtApplicationHearingSteps();
        final String hearingId = hearingsData.getHearingData().get(0).getId().toString();
        final String courtCenterId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final String applicationId = hearingsData.getHearingData().get(0).getCourtApplications().get(0).getId().toString();
        deleteCourtApplicationHearingSteps.whenRaisedCourtApplicationHearingPublicEvent(hearingId, applicationId);
        deleteCourtApplicationHearingSteps.verifyOldHearingDeleted(hearingId, courtCenterId);
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

    private void verifyJudiciaryAssignedToAllocatedHearingFromAPI(final UUID hearingId, final List<JudicialRoleData> judicialRoleDataList) {

        List<Matcher> matchers = Lists.newArrayList();
        IntStream.range(0, judicialRoleDataList.size())
                .forEach(judiciaryIndex -> {
                    final String baseJudiciaryPath = String.format("$.judiciary[%d]", judiciaryIndex);
                    matchers.add(withJsonPath(baseJudiciaryPath + ".judicialId", is(judicialRoleDataList.get(judiciaryIndex).getJudicialId().toString())));
                    matchers.add(withJsonPath(baseJudiciaryPath + ".judicialRoleType.judiciaryType", is(judicialRoleDataList.get(judiciaryIndex).getJudicialRoleType().getJudiciaryType())));
                    matchers.add(withJsonPath(baseJudiciaryPath + ".isBenchChairman", is(judicialRoleDataList.get(judiciaryIndex).getIsBenchChairman().get())));
                    matchers.add(withJsonPath(baseJudiciaryPath + ".isDeputy", is(judicialRoleDataList.get(judiciaryIndex).getIsDeputy().get())));
                });

        pollForHearingById(getLoggedInUser(), hearingId, allOf(matchers.toArray(new Matcher[0])));
    }


    @Test
    void shouldRaisePublicEventJudiciaryChangedForHearingStatus() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId().toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(hearingsData.getHearingData().get(0));
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary);
        updateHearingSteps.whenJudiciaryIsChangedForHearings();
        updateHearingSteps.verifyJudiciaryChangedForHearingStatusPublicEvent();
    }

}

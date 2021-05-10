package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubUpdateAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtRoom;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;

@SuppressWarnings("squid:S1607")

public class HearingIT extends AbstractIT {
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";

    @Test
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEvent() {
        final HearingsData hearingsData = hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsInAllocationInMQ();
            updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        }
    }

    @Test
    public void assignPublicListNoteInAllocatedListing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
            updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        }
    }


    @Test
    public void changePublicListNoteInAllocatedListing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
            updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        }


        final UpdatedHearingData updatedHearingDataForChangeingVideoLinkDetails = UpdatedHearingData.updatedHearingDataWithVideoLink(updatedHearingDataForAllocation);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForChangeingVideoLinkDetails)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingUpdatedResultsForPublicListNoteInMQ();
            updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
        }
    }

    @Test
    public void removePublicListNoteInAllocatedListing() {
        final HearingsData hearingsData = HearingsData.hearingsData();

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
            updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        }


        final UpdatedHearingData updatedHearingDataForChangingVideoLinkDetails = UpdatedHearingData.updatedHearingDataWithoutVideoLink(updatedHearingDataForAllocation);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForChangingVideoLinkDetails)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsForRemovingPublicListNoteInMQ();
        }
    }


    @Test
    public void shouldUpdateHearingResultsInPartialAllocatedListing() {
        HearingsData hearingsData = hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        final UUID removedOffenceId = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(1).getOffenceId();
        hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().remove(1);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithProsecutionCases();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
            updateHearingSteps.verifyHearingAllocatedEventNotExistsRemovedOffence(removedOffenceId);
            updateHearingSteps.verifyHearingPartiallyEvent(removedOffenceId);

        }

    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndUpdateSlotDetails() {
        final UUID courtCentreId = randomUUID();
        stubGetReferenceDataCourtCentreById(courtCentreId);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndNotUpdateSlotDetails() {

        final HearingsData hearingsData = hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsInAllocationInMQ();
            updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        }

    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndReturnSlotDetailsForAdjournmentHearing() {
        stubUpdateAvailableHearingSlotsService();
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
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

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingWithJudicialId(judicialRoleDataList.get(0).getJudicialId());
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
            listCourtHearingSteps.verifyJudiciaryAssignedEventWithRotaSLJudiciaries(judicialRoleDataList);
        }
    }

    @Test
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEventWithNoJudiciary() {
        final HearingsData hearingsData = hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithoutJudiciary(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingConfirmedInPublicMQHasNoJudiciary();
        }
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndRaisesPublicHearingUpdatedEvent() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForUnallocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsInMQ();
            updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
            updateHearingSteps.verifyVacatedTrialUpdatedInPublicMQ(ALLOCATED, false);
            updateHearingSteps.verifyHearingUpdatedInPublicMQ();
        }
    }


    @Test
    public void updateHearingResultsInUpdatedListingAndUpdateSlotDetails() {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithNonDefaultDays(hearingId);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsForSlotUpdateInMQ();
            updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingDaysWhenQueryFromAPI();
        }
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndUpdateSlotDetailsWithNoJudiciaryAndGetJudiciaryInfoFromRotaSL() throws IOException {
        final UUID courtCentreId = randomUUID();
        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, MAGISTRATES_JURISDICTION);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithoutJudiciary(hearingId);

        final String sessionStartDate = updatedHearingDataForAllocation.getStartDate();

        final JsonObject getHearingSlotsJsonObject = stubGetAvailableHearingSlotsWithQueryParams(false,
                updatedHearingDataForAllocation.getCourtRoomId().toString(),
                "C55BN00",
                sessionStartDate,
                sessionStartDate);

        final List<JudicialRoleData> judicialRoleDataList = getJudicialRoleDataFromRotaSLHearingSlots(getHearingSlotsJsonObject);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyJudiciaryChangedEventWithRotaSLJudiciaries(judicialRoleDataList);
            updateHearingSteps.verifyHearingDaysWhenQueryFromAPI();
        }
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
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataWithNoCourtRoom = UpdatedHearingData.updatedHearingDataWithNoCourtRoom(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithNoCourtRoom)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedWithNoCourtRoomResultsInUnallocationInMQ();
            updateHearingSteps.verifyHearingUpdatedWithNoCourtRoomAndUnallocatedWhenQueryingFromAPI();
        }
    }

    @Test
    public void updateAllocatedHearingWithNoEndDateResultsInUnallocatedListing() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataWithNoEndDate = UpdatedHearingData.updatedHearingDataWithNoEndDate(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithNoEndDate)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedWithNoEndDateResultsInUnallocationInMQ();
            updateHearingSteps.verifyHearingUpdatedWithNoEndDateAndUnallocatedWhenQueryingFromAPI();
        }
    }

    @Test
    public void hearingCanBeSearchedForUsingDifferentCombinationsOfParameters() {

        stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForUnallocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation)) {
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
    }

    @Test
    public void updateJudicialRolesForHearings() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary)) {
            updateHearingSteps.whenJudiciaryIsChangedForHearings();
            updateHearingSteps.verifyHearingWithUpdatedJudiciaryInMQ();
            updateHearingSteps.verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI();
        }
    }

    @Test
    public void sequenceHearingDays() {
        final HearingsData hearingsData = HearingsData.singleHearingData();
        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        final SequenceHearingData sequenceHearingData = new SequenceHearingData(updatedHearingDataForAllocation);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingFoundByAllocatedFromAPI();
        }

        try (final SequenceHearingSteps sequenceHearingSteps = new SequenceHearingSteps(sequenceHearingData)) {
            sequenceHearingSteps.whenHearingDaysAreSequenced();
            sequenceHearingSteps.verifyHearingWithSequencedDaysInMQ();
            sequenceHearingSteps.verifyHearingDaysAreSequencedFromAPI();
            sequenceHearingSteps.verifyHearingUpdatedInPublicMQ();
        }
    }

    @Test
    public void shouldUpdateWeekCommencing() {
        final HearingsData hearingsData = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 1);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
            listCourtHearingSteps.verifyHearingListedWithWeekCommencingFromAPI(UNALLOCATED, LocalDate.now(), 1);
        }
    }

    @Test
    public void shouldHideUnallocatedHearingOnceVacatedFromUnallocatedLists() {
        final HearingsData hearingsData = hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

            final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
            vacatingTrialSteps.whenHearingIsVacated();

            vacatingTrialSteps.verifyListingTrialVacatedEvent(false);
            vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
            listCourtHearingSteps.verifyHearingIsNotListed(false);
        }
    }

    @Test
    public void shouldFindUnallocatedHearingWithCaseUrn() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedWithAnyAllocationFromAPI(UNALLOCATED);
        }
    }


}

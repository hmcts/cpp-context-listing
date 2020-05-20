package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubUpdateAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtRoom;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.SequenceHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.SequenceHearingData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class HearingIT extends AbstractIT {
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";

    @Test
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEvent() {
        final HearingsData hearingsData = HearingsData.hearingsData();
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
    public void shouldRaisePublicHearingConfirmedPublicEventAndUpdateSlotDetails() {
        final UUID courtCentreId = randomUUID();
        stubGetReferenceDataCourtCentreById(courtCentreId);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, "MAGISTRATES"))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

    }

    @Test
    public void shouldRaisePublicHearingConfirmedPublicEventAndNotUpdateSlotDetails() {
        final HearingsData hearingsData = HearingsData.hearingsData();
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
        stubGetReferenceDataCourtRoom(updatedHearingDataForAllocation.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingDataForAllocation.getCourtRoomId());

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
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEventWithNoJudiciary() {
        final HearingsData hearingsData = HearingsData.hearingsData();
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
            updateHearingSteps.verifyHearingUpdatedInPublicMQ();
        }
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndUpdateSlotDetails() {
        final UUID courtCentreId = randomUUID();

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, "MAGISTRATES");
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
        }
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
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataForUnallocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation)) {
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
}

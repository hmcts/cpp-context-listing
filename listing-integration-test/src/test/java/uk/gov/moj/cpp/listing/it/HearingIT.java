package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.SendCaseForListingSteps;
import uk.gov.moj.cpp.listing.steps.SequenceHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.SequenceHearingData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import org.junit.Test;

public class HearingIT extends AbstractIT {

    @Test
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEvent() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedInActiveMQ();
            sendCaseForListingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsInAllocationInMQ();
            updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingConfirmedInPublicMQ();
        }
    }

    @Test
    public void updateHearingResultsInAllocatedListingAndRaisesPublicHearingConfirmedPublicEventWithNoJudiciary() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedInActiveMQ();
            sendCaseForListingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocationWithoutJudiciary(hearingsData.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingConfirmedInPublicMQHasNoJudiciary();
        }
    }

    @Test
    public void updateHearingResultsInUpdatedListingAndRaisesPublicHearingUpdatedEvent() {
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        UpdatedHearingData updatedHearingDataForUnallocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedResultsInMQ();
            updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
            updateHearingSteps.verifyHearingUpdatedInPublicMQ();
        }
    }


    @Test
    public void updateAllocatedHearingWithNoCourtRoomResultsInUnallocatedListing() {
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        UpdatedHearingData updatedHearingDataWithNoCourtRoom = UpdatedHearingData.updatedHearingDataWithNoCourtRoom(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithNoCourtRoom)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedWithNoCourtRoomResultsInUnallocationInMQ();
            updateHearingSteps.verifyHearingUpdatedWithNoCourtRoomAndUnallocatedWhenQueryingFromAPI();
        }
    }

    @Test
    public void updateAllocatedHearingWithNoEndDateResultsInUnallocatedListing() {
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        UpdatedHearingData updatedHearingDataWithNoEndDate = UpdatedHearingData.updatedHearingDataWithNoEndDate(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithNoEndDate)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedWithNoEndDateResultsInUnallocationInMQ();
            updateHearingSteps.verifyHearingUpdatedWithNoEndDateAndUnallocatedWhenQueryingFromAPI();
        }
    }

    @Test
    public void hearingCanBeSearchedForUsingDifferentCombinationsOfParameters() {
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        UpdatedHearingData updatedHearingDataForUnallocation = UpdatedHearingData.updatedHearingData(hearingsData.getHearingData().get(0));
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
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(hearingsData.getHearingData().get(0));
        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary)) {
            updateHearingSteps.whenJudiciaryIsChangedForHearings();
            updateHearingSteps.verifyHearingWithUpdatedJudiciaryInMQ();
            updateHearingSteps.verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI();
        }
    }

    @Test
    public void sequenceHearingDays() {
        HearingsData hearingsData = HearingsData.singleHearingData();
        UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());
        SequenceHearingData sequenceHearingData = new SequenceHearingData(updatedHearingDataForAllocation);
        
        try (SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedFromAPI(UNALLOCATED);
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
}

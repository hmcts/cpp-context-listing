package uk.gov.moj.cpp.listing.it;

import static java.time.LocalDate.now;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithWeekCommencingDate;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.WeekCommencingHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;

import org.junit.Test;

public class WeekCommencingHearingIT extends AbstractIT {

    @Test
    public void shouldUpdateHearingWithWeekCommencingDatesAndKeepItUnallocated() {
        final HearingsData hearingsData = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 1);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataWithWeekCommencingDate = updatedHearingDataWithWeekCommencingDate(hearingsData.getHearingData().get(0), now().toString(), now().plusDays(7l).toString(), 1);

        try (final WeekCommencingHearingSteps weekCommencingHearingSteps = new WeekCommencingHearingSteps(updatedHearingDataWithWeekCommencingDate)) {
            weekCommencingHearingSteps.whenHearingIsUpdatedForListingForWeekCommencingDate();

            weekCommencingHearingSteps.verifyHearingUpdatedResultsForWeekCommencingInMQ();
            weekCommencingHearingSteps.verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI();
        }
    }

    @Test
    public void shouldUpdateUpdateHearingWithWeekCommencingDatesToFixedDatesAndAllocateHearing() {
        final HearingsData hearingsData = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 1);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        final UpdatedHearingData updatedHearingDataWithWeekCommencingDate = updatedHearingDataWithWeekCommencingDate(hearingsData.getHearingData().get(0), now().toString(), now().plusDays(7l).toString(), 1);

        try (final WeekCommencingHearingSteps weekCommencingHearingSteps = new WeekCommencingHearingSteps(updatedHearingDataWithWeekCommencingDate)) {
            weekCommencingHearingSteps.whenHearingIsUpdatedForListingForWeekCommencingDate();

            weekCommencingHearingSteps.verifyHearingUpdatedResultsForWeekCommencingInMQ();
            weekCommencingHearingSteps.verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI();
        }

        final UpdatedHearingData updatedHearingDataForUnallocation = updatedHearingData(hearingsData.getHearingData().get(0));

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingUpdatedWhenWeekCommencingDateRemovedResultsInMQ();
            updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        }
    }
}

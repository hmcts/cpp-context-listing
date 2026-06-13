package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithWeekCommencingDate;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.WeekCommencingHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.LocalDate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class WeekCommencingHearingIT extends AbstractIT {

    @Test
    public void shouldUpdateHearingWithWeekCommencingDatesAndKeepItUnallocated() {
        final HearingsData hearingsData = HearingsData.hearingsDataForWeekCommencing(ItClock.today(), 1);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataWithWeekCommencingDate = updatedHearingDataWithWeekCommencingDate(hearingsData.getHearingData().get(0), ItClock.today().plusDays(1).toString(), ItClock.today().plusDays(7l).toString(), 1);

        final WeekCommencingHearingSteps weekCommencingHearingSteps = new WeekCommencingHearingSteps(updatedHearingDataWithWeekCommencingDate);
        weekCommencingHearingSteps.whenHearingIsUpdatedForListingForWeekCommencingDate();

        weekCommencingHearingSteps.verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI();
    }

    @Test
    public void shouldUpdateUpdateHearingWithWeekCommencingDatesToFixedDatesAndAllocateHearing() {
        final HearingsData hearingsData = HearingsData.hearingsDataForWeekCommencing(ItClock.today(), 1);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final UpdatedHearingData updatedHearingDataWithWeekCommencingDate = updatedHearingDataWithWeekCommencingDate(hearingsData.getHearingData().get(0), ItClock.today().plusDays(1).toString(), ItClock.today().plusDays(7l).toString(), 1);

        final WeekCommencingHearingSteps weekCommencingHearingSteps = new WeekCommencingHearingSteps(updatedHearingDataWithWeekCommencingDate);
        weekCommencingHearingSteps.whenHearingIsUpdatedForListingForWeekCommencingDate();

        weekCommencingHearingSteps.verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI();

        final UpdatedHearingData updatedHearingDataForUnallocation = updatedHearingData(hearingsData.getHearingData().get(0));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedWhenWeekCommencingDateRemovedResultsInMQ();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
    }
}

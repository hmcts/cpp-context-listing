package uk.gov.moj.cpp.listing.it;

import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.next;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithWeekCommencingDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetCourtSchedulesByIdWithDraftStatus;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyListHearingInCourtSessionsCalledOnce;

import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.WeekCommencingHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;

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
    public void shouldListCrownWeekCommencingHearingWithBookedSlotKeepItsSessionDayAndStayUnallocated() {
        final LocalDate weekCommencingStartDate = ItClock.today().with(next(MONDAY));
        final HearingsData hearingsData = HearingsData.hearingsDataForBookedSlotWithWeekCommencing(weekCommencingStartDate, 1);
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final RotaSlot bookedSlot = hearingData.getBookedSlots().get(0);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingWithProsecutionCasesAndBookedDraftSession();

        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        listCourtHearingSteps.verifyHearingListedWithWeekCommencingFromAPI(UNALLOCATED, weekCommencingStartDate, 1);
        listCourtHearingSteps.verifyCrownWeekCommencingHearingListedWithBookedSessionDayFromAPI(bookedSlot.getCourtScheduleId(),
                weekCommencingStartDate, bookedSlot.getStartTime(), bookedSlot.getDuration());
        verifyListHearingInCourtSessionsCalledOnce(hearingData.getId().toString(), bookedSlot.getCourtScheduleId(),
                bookedSlot.getStartTime(), bookedSlot.getDuration());
        listCourtHearingSteps.verifyPublicEventHearingListed();
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

        final String courtScheduleId = updatedHearingDataForUnallocation.getNonDefaultDays().get(0).getCourtScheduleId().orElseThrow();
        // Rich overload: the single-day UPDATE enrichment sanity-checks hearingDate against the
        // session's sessionDate, so the stub must agree with the update payload's nonDefaultDay.
        final LocalDate updatedStartDate = LocalDate.parse(updatedHearingDataForUnallocation.getStartDate());
        final java.time.ZonedDateTime updatedStartTime = updatedStartDate.atTime(10, 0).atZone(ZoneOffset.UTC);
        stubGetCourtSchedulesByIdWithDraftStatus(Collections.singletonList(courtScheduleId), false,
                updatedStartDate,
                updatedHearingDataForUnallocation.getCourtCentreId(),
                updatedHearingDataForUnallocation.getCourtRoomId(),
                updatedStartTime);
        // Sessions listing must also reflect the UPDATED start time — the enrichment rebuilds
        // the hearing day from this response (stale seed-time values would revert the date).
        stubListHearingInCourtSessions(hearingsData.getHearingData().get(0).getId().toString(),
                courtScheduleId,
                updatedStartTime);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForUnallocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedWhenWeekCommencingDateRemovedResultsInMQ();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
    }
}

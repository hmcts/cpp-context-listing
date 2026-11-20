package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForCrownAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetCourtSchedulesByIdWithSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;

import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.CourtScheduleStubSession;
import uk.gov.moj.cpp.listing.steps.DailyListPayloadSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WeekCommencingListPayloadIT extends AbstractIT {

    private DailyListPayloadSteps weekCommencingListPayloadSteps;
    private UpdatedHearingData updatedHearingData;

    @BeforeEach
    public void setUp() {
        super.setUp();

        // WARN and FIRM list types require CROWN jurisdiction — the DB query for week commencing
        // hearings always filters on jurisdictionType = CROWN via RangeSearchQueryRequestFactory.
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        updatedHearingData = updatedHearingDataForCrownAllocation(hearingsData.getHearingData().get(0).getId());

        // ccsph2n CROWN update enrichment: each nonDefaultDay's courtScheduleId is resolved via courtscheduler
        // search.court-schedules-by-id and the hearing day is re-derived from that session (date, centre, room),
        // so stub ONE session per nonDefaultDay on its own date. The sessions listing then mirrors the same
        // per-day start times (team overload) instead of one shared start time.
        stubGetCourtSchedulesByIdWithSessions(updatedHearingData.getNonDefaultDays().stream()
                .map(nonDefaultDay -> new CourtScheduleStubSession(
                        nonDefaultDay.getCourtScheduleId().orElseThrow(),
                        ZonedDateTime.parse(nonDefaultDay.getStartTime()).toLocalDate(),
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getCourtRoomId(),
                        ZonedDateTime.parse(nonDefaultDay.getStartTime()),
                        false))
                .toList());
        stubListHearingInCourtSessionsWithMultipleSchedules(updatedHearingData);

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();

        weekCommencingListPayloadSteps = new DailyListPayloadSteps(hearingsData, updatedHearingData);
    }

    @Test
    void shouldReturnWeekCommencingListPayloadForWarn() {
        weekCommencingListPayloadSteps.verifyWeekCommencingListPayloadContainsHearing("WARN", updatedHearingData.getEndDate());
    }

    @Test
    void shouldReturnWeekCommencingListPayloadForFirm() {
        weekCommencingListPayloadSteps.verifyWeekCommencingListPayloadContainsHearing("FIRM", updatedHearingData.getEndDate());
    }
}

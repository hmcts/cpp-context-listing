package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForCrownAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;

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

        stubListHearingInCourtSessionsWithMultipleSchedules(
                hearingsData.getHearingData().get(0).getId().toString(),
                updatedHearingData.getNonDefaultDays().get(0).getCourtScheduleId().map(java.util.UUID::fromString).orElse(null).toString(),
                updatedHearingData.getNonDefaultDays().get(1).getCourtScheduleId().map(java.util.UUID::fromString).orElse(null).toString(),
                ZonedDateTime.parse(updatedHearingData.getNonDefaultDays().get(0).getStartTime()),
                updatedHearingData.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();

        weekCommencingListPayloadSteps = new DailyListPayloadSteps(updatedHearingData);
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

package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;

import uk.gov.moj.cpp.listing.steps.DailyListPayloadSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DailyListPayloadIT extends AbstractIT {

    private DailyListPayloadSteps dailyListPayloadSteps;

    @BeforeEach
    public void setUp() {
        super.setUp();
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final UpdatedHearingData updatedHearingData = updatedHearingDataForAllocation(hearingsData.getHearingData().get(0).getId());

        stubListHearingInCourtSessionsWithMultipleSchedules(
                hearingsData.getHearingData().get(0).getId().toString(),
                updatedHearingData.getNonDefaultDays().get(0).getCourtScheduleId().map(java.util.UUID::fromString).orElse(null).toString(),
                updatedHearingData.getNonDefaultDays().get(1).getCourtScheduleId().map(java.util.UUID::fromString).orElse(null).toString(),
                ZonedDateTime.parse(updatedHearingData.getNonDefaultDays().get(0).getStartTime()),
                updatedHearingData.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();

        dailyListPayloadSteps = new DailyListPayloadSteps(updatedHearingData);
    }

    @Test
    void shouldReturnDailyListPayloadForDraft() {
        dailyListPayloadSteps.verifyDailyListPayloadContainsHearing("DRAFT");
    }

    @Test
    void shouldReturnDailyListPayloadForFinal() {
        dailyListPayloadSteps.verifyDailyListPayloadContainsHearing("FINAL");
    }
}

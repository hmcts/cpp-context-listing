package uk.gov.moj.cpp.listing.it;

import static java.time.LocalDate.parse;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForCrownAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.PddaListPayloadSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PddaListPayloadIT extends AbstractIT {

    private PddaListPayloadSteps pddaListPayloadSteps;
    private LocalDate endDate;

    @BeforeEach
    public void setUp() {
        super.setUp();
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        // WARN and FIRM (like the daily list tests) need a CROWN allocated hearing. The hearing keeps its own
        // (PTP) type: the Xhibit generation looks the type up in the hearing type reference data cache, which
        // is loaded once and only contains the types served by the stubs, always including PTP.
        final UpdatedHearingData updatedHearingData = updatedHearingDataForCrownAllocation(
                hearingsData.getHearingData().get(0).getId(),
                hearingsData.getHearingData().get(0).getHearingTypeData());
        endDate = parse(updatedHearingData.getStartDate()).plusDays(2);

        stubListHearingInCourtSessionsWithMultipleSchedules(
                hearingsData.getHearingData().get(0).getId().toString(),
                updatedHearingData.getNonDefaultDays().get(0).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                updatedHearingData.getNonDefaultDays().get(1).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                ZonedDateTime.parse(updatedHearingData.getNonDefaultDays().get(0).getStartTime()),
                updatedHearingData.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();

        pddaListPayloadSteps = new PddaListPayloadSteps(hearingsData, updatedHearingData);
    }

    @Test
    void shouldReturnPddaDailyListForDraft() {
        pddaListPayloadSteps.verifyPddaListPayloadIsReturned("DRAFT", "DailyList", endDate);
        pddaListPayloadSteps.verifyPddaDailyListContainsTheListedHearing("DRAFT", endDate);
    }

    @Test
    void shouldReturnPddaDailyListForFinal() {
        pddaListPayloadSteps.verifyPddaListPayloadIsReturned("FINAL", "DailyList", endDate);
    }

    @Test
    void shouldReturnPddaAdvanceListForWarn() {
        pddaListPayloadSteps.verifyPddaListPayloadIsReturned("WARN", "WarnedList", endDate);
    }

    @Test
    void shouldReturnPddaFirmListForFirm() {
        pddaListPayloadSteps.verifyPddaListPayloadIsReturned("FIRM", "FirmList", endDate);
    }
}

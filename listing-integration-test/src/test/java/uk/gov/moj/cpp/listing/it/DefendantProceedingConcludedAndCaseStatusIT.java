package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.singleHearingDataMultipleCasesWithSingleOffence;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.*;

import com.google.common.collect.ImmutableMap;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.listing.steps.CaseUpdatedAndDefendantProceedingsConcludedSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DefendantProceedingConcludedAndCaseStatusIT extends AbstractIT {

    @Test
    void shouldUpdateDefendantProceedingConcludedAndCaseStatusEventFromProgression() {
        HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);

        final CaseUpdatedAndDefendantProceedingsConcludedSteps caseUpdatedAndDefendantProceedingsConcludedSteps = new CaseUpdatedAndDefendantProceedingsConcludedSteps(caseId, hearingData);
        caseUpdatedAndDefendantProceedingsConcludedSteps.publishUntilCaseStatusReflected(UNALLOCATED);
    }

    @Test
    void shouldUpdateDefendantProceedingConcludedAndCaseStatusEventFromProgressionWhenAllocated() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(ItClock.today(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ItClock.nowUtc()
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);

        final CaseUpdatedAndDefendantProceedingsConcludedSteps caseUpdatedAndDefendantProceedingsConcludedSteps = new CaseUpdatedAndDefendantProceedingsConcludedSteps(caseId, hearingData);
        caseUpdatedAndDefendantProceedingsConcludedSteps.publishUntilCaseStatusReflected(ALLOCATED);
    }

    @Test
    void shouldNotUpdateDefendantProceedingConcludedAndCaseStatusEventFromProgressionWhenIncorrectDefendantIsSupplied() {
        HearingsData hearingsData = singleHearingDataMultipleCasesWithSingleOffence();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        final UUID caseId = listedCaseData.getCaseId();

        // Using a different defendant ID so that we land in the situation where defendant list is empty
        ReflectionUtil.setField(listedCaseData.getDefendants().get(0), "defendantId", randomUUID());

        final CaseUpdatedAndDefendantProceedingsConcludedSteps caseUpdatedAndDefendantProceedingsConcludedSteps = new CaseUpdatedAndDefendantProceedingsConcludedSteps(caseId, hearingData);
        caseUpdatedAndDefendantProceedingsConcludedSteps.whenPublicEventCaseUpdatedAndHearingResultedIsPublished();
        caseUpdatedAndDefendantProceedingsConcludedSteps.verifyHearingForCaseStatusAndDefendantProceedingsConcludedNotSetFromAPIWithJmsDelay(UNALLOCATED);
    }
}
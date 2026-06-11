package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;

import com.google.common.collect.ImmutableMap;
import uk.gov.moj.cpp.listing.steps.AddDefendantSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DefendantsAddedIT extends AbstractIT {

    @Test
    void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsUnallocated() {
        HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.publishUntilDefendantsAddedReflected(false);
    }

    @Test
    void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsUnallocatedEnabled() {
        HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.publishUntilDefendantsAddedReflected(true);
    }


    @Test
    void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsAllocated() {
        HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.publishUntilDefendantsAddedConsumed();
    }

    @Test
    void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsAllocatedHmiEnabled() {
        HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneOffset.UTC)
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.publishUntilDefendantsAddedConsumed();
    }
}

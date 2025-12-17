package uk.gov.moj.cpp.listing.it;


import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData.updatedDefendantData;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DefendantsChangedIT extends AbstractIT {

    @Test
    void shouldUpdateDefendantsFollowingPublicDefendantsChangedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedDefendantData updatedDefendantData = updatedDefendantData(defendantData);

        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenPublicEventProgressionCaseDefendantsUpdatedIsPublished();
        updateDefendantSteps.verifyHearingListedFromAPIWithJmsDelay(false);
    }

    @Test
    void shouldUpdateDefendantsFollowingPublicDefendantsChangedEventFromProgressionHmiEnabled() {
        String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final UUID courtCentreId = getRandomCourtCenterId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);        stubListHearingInCourtSessions(hearingsData.getHearingData().get(0).getId().toString(),
                courtScheduleId, hearingsData.getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedDefendantData updatedDefendantData = updatedDefendantData(defendantData);

        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenPublicEventProgressionCaseDefendantsUpdatedIsPublished();
        updateDefendantSteps.verifyHearingListedFromAPIWithJmsDelay(true);
    }
}

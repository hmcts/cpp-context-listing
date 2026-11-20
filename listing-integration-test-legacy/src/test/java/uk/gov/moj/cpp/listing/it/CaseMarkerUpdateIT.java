package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateCaseMarkersSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseMarkerData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S2925")
public class CaseMarkerUpdateIT extends AbstractIT {

    @Test

    void shouldUpdateCaseMarkersForListedCase() {
        final HearingsData hearingsData = listCourtHearing();
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData caseToUpdateMarkers = hearingData.getListedCases().get(0);
        final CaseMarkerData caseMarkerData = caseToUpdateMarkers.getCaseMarkers().get(0);
        final UUID caseIdToUpdateMarkers = caseToUpdateMarkers.getCaseId();


        final UpdateCaseMarkersSteps steps = new UpdateCaseMarkersSteps(caseIdToUpdateMarkers, hearingData, caseMarkerData);
        final ZonedDateTime hearingStartTime = hearingData.getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final String courtroomId = "8e837de0-743a-4a2c-9db3-b2e678c48728";
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = hearingData.getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId);
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        // Re-publishes until the case<->hearing link (Case.hearingIds, populated by the async
        // add-hearing-to-case command) is established and the update lands. A single publish can be
        // silently dropped on slow environments — see UpdateCaseMarkersSteps#publishUntilCaseMarkersReflected.
        steps.publishUntilCaseMarkersReflected(caseIdToUpdateMarkers);
    }


    private HearingsData listCourtHearing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        return hearingsData;
    }
}

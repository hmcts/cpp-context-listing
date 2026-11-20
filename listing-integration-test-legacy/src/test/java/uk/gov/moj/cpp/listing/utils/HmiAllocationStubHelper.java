package uk.gov.moj.cpp.listing.utils;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;

import uk.gov.moj.cpp.listing.steps.data.HearingData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aligns court-scheduler WireMock stubs with the hearing under test (start time, court centre, room).
 * Using {@code ZonedDateTime.now()} at a fixed clock time causes pipeline-only allocation failures.
 */
public final class HmiAllocationStubHelper {

    private static final String DEFAULT_COURT_SCHEDULE_ID = "8e837de0-743a-4a2c-9db3-b2e678c48729";

    private HmiAllocationStubHelper() {
    }

    public static void stubForAllocatedListing(final HearingData hearingData) {
        stubForAllocatedListing(hearingData, DEFAULT_COURT_SCHEDULE_ID);
    }

    public static void stubForAllocatedListing(final HearingData hearingData, final String courtScheduleId) {
        final ZonedDateTime hearingStartTime = hearingData.getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtCentreId = hearingData.getCourtCentreId();
        final UUID courtroomId = hearingData.getCourtRoomId();
        final UUID bookingId = randomUUID();

        final Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());

        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(hearingData.getId().toString(), courtScheduleId, hearingStartTime);
    }
}

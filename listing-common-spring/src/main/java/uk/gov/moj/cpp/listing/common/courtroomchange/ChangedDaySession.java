package uk.gov.moj.cpp.listing.common.courtroomchange;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A single allocated session returned by courtscheduler's
 * {@code change-court-room-for-multiday-hearing} action - one element of the response's
 * {@code allocatedSchedules} array, which serialises the courtscheduler {@code CourtSchedule}
 * entity. Parsed defensively (see {@code CourtSchedulerServiceAdapter}) since not every field is
 * guaranteed present.
 */
public record ChangedDaySession(UUID courtScheduleId,
                                 String courtRoomId,
                                 LocalDate sessionDate,
                                 String sessionStartTime,
                                 Integer durationInMinutes,
                                 Boolean isDraft) {

    /** Convenience constructor for callers/tests that don't care about the session's draft state. */
    public ChangedDaySession(final UUID courtScheduleId, final String courtRoomId, final LocalDate sessionDate,
                             final String sessionStartTime, final Integer durationInMinutes) {
        this(courtScheduleId, courtRoomId, sessionDate, sessionStartTime, durationInMinutes, null);
    }
}

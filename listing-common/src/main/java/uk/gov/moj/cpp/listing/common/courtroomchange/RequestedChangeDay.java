package uk.gov.moj.cpp.listing.common.courtroomchange;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One requested day-change for the courtscheduler {@code change-court-room-for-multiday-hearing}
 * action - the target session (identified by courtScheduleId) that a given day of a CROWN
 * multi-day hearing should move to.
 */
public record RequestedChangeDay(LocalDate sessionDate, UUID courtScheduleId, int durationInMinutes) {
}

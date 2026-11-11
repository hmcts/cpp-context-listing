package uk.gov.moj.cpp.listing.persistence.entity.query;

import java.time.Instant;

/**
 * One judicial id sitting on one hearing day.
 *
 * @param hearingId       the hearing the day belongs to
 * @param courtScheduleId the Court Scheduler session of the matching element of
 *                        {@code properties->'hearingDays'}, or null when the day was not booked
 *                        through Court Scheduler
 * @param startTime       the hearing day start time ({@code hearing_days.start_time})
 * @param judicialId      the judicial id from {@code properties->'judiciary'}
 */
public record JudiciaryHearingDay(String hearingId, String courtScheduleId, Instant startTime, String judicialId) {
}

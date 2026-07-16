package uk.gov.moj.cpp.listing.common.movehearingdate;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Purpose-built result of a successful {@code courtscheduler.move-hearing-date} call for
 * the MAGISTRATES path. Deliberately narrow — unlike the ccsph2n-only {@code CrownFallbackResult}
 * this branch does not carry any crown-fallback/search-and-book concerns, only the slot details
 * needed to enrich {@code listing.command.move-hearing-date-enriched}.
 */
public record MoveHearingDateResult(UUID courtScheduleId,
                                           String courtRoomId,
                                           LocalDate sessionDate,
                                           String sessionStartTime,
                                           String sessionEndTime,
                                           Integer durationInMinutes) {
}

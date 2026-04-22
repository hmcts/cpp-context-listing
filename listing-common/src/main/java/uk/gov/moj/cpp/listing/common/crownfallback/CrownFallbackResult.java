package uk.gov.moj.cpp.listing.common.crownfallback;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Parsed response from courtscheduler's {@code courtscheduler.crown.fallback.search.book.hearing.slots}.
 * Carries the booked session details needed to enrich the listing aggregate.
 */
public record CrownFallbackResult(UUID hearingId,
                                  UUID courtScheduleId,
                                  Integer courtRoomId,
                                  LocalDate sessionDate,
                                  ZonedDateTime sessionStartTime,
                                  ZonedDateTime sessionEndTime,
                                  Integer durationInMinutes,
                                  Boolean isDraft,
                                  String businessType,
                                  String source,
                                  Boolean overbooked) {
}

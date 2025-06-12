package uk.gov.moj.cpp.listing.common.service;

import java.time.LocalDate;
import java.util.UUID;

public record IdResponse(UUID hearingId, UUID courtScheduleId, LocalDate hearingDate,
                         long hearingDayCount, long hearingDayPosition) {
}

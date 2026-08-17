package uk.gov.moj.cpp.listing.command.api.service;

/**
 * Thrown when courtscheduler returns a non-2xx response while resolving court-schedule session
 * ids ({@code search.court-schedules-by-id}) — a transient outage. Distinct from
 * {@code CrownFallbackInvalidRequestException}: a 2xx with no sessions means the supplied id is
 * invalid, whereas this failure is retryable and surfaces as a server-side error to the caller.
 */
public class CourtScheduleUnavailableException extends RuntimeException {

    public CourtScheduleUnavailableException(final String message) {
        super(message);
    }
}

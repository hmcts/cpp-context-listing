package uk.gov.moj.cpp.listing.common.crownfallback;

/**
 * Thrown when the listing side passes a request that courtscheduler rejects — typically because
 * durationInMinutes exceeds the single-day cap (360), or because a bookingReference resolves to
 * no session at all (a hold that expired at midnight and was purged before the share landed).
 *
 * <p><b>This exception is never caught.</b> Unlike its sibling
 * {@link CrownFallbackNoSessionException}, which is handled by proceeding unallocated, this one
 * escapes the listener, rolls the JMS transaction back, is redelivered a handful of times and
 * then lands in the DLQ. The clerk has already been shown "Shared ✓" and the result is recorded
 * in hearing, so the visible outcome is a result with no hearing listed against it and nothing
 * anywhere saying so.
 *
 * <p>Every throw site therefore logs {@link #LOG_MARKER} at ERROR with the hearingId immediately
 * before throwing. That log line is the only signal this failure produces, and an alert keys on
 * it — so do not throw this without emitting it, and do not reuse the marker for anything
 * recoverable. {@code [CROWN-FB]} already exists for the recoverable cases and is deliberately a
 * different token, because an alert that also fires on routine fallbacks gets muted.
 */
public class CrownFallbackInvalidRequestException extends RuntimeException {

    /**
     * Alert marker. A line carrying this means a share succeeded for the clerk but the hearing
     * was not listed, and nobody has been told. Always accompanied by the hearingId, which is
     * the key needed to find and re-list the hearing.
     */
    public static final String LOG_MARKER = "[LISTING-LOST]";

    public CrownFallbackInvalidRequestException(final String message) {
        super(message);
    }
}

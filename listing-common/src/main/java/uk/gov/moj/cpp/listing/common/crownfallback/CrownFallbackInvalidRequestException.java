package uk.gov.moj.cpp.listing.common.crownfallback;

/**
 * Thrown when the listing side passes a request that courtscheduler rejects — typically because
 * durationInMinutes exceeds the single-day cap (360). Indicates a caller bug or an unexpected
 * multi-day Crown payload arriving without an anchor courtScheduleId.
 */
public class CrownFallbackInvalidRequestException extends RuntimeException {

    public CrownFallbackInvalidRequestException(final String message) {
        super(message);
    }
}

package uk.gov.moj.cpp.listing.common.crownfallback;

/**
 * Thrown when the courtscheduler Crown fallback could not find a session at the supplied
 * courtCentre + hearingDate, even after relaxing businessType / court_session / courtRoomId
 * and permitting overbooking. Surfaces as a command failure to the caller.
 */
public class CrownFallbackNoSessionException extends RuntimeException {

    public CrownFallbackNoSessionException(final String message) {
        super(message);
    }
}

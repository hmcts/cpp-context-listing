package uk.gov.moj.cpp.listing.common.service;

/**
 * A hearing query could not be completed. Unchecked and deliberately not caught by callers: an
 * absent answer already has a meaning ({@code Optional.empty()} — nothing recorded), so a failure
 * that were reported the same way would silently produce a blank result indistinguishable from a
 * legitimately blank one.
 */
public class HearingQueryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HearingQueryException(final String message) {
        super(message);
    }

    public HearingQueryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

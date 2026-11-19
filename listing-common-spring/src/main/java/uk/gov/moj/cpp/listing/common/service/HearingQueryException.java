package uk.gov.moj.cpp.listing.common.service;

/**
 * A hearing query could not be completed (copied from listing-common). Unchecked and deliberately
 * not caught by callers: an absent answer already has a meaning, so a failure reported the same
 * way would silently produce a blank result.
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

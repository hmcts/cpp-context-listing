package uk.gov.moj.cpp.listing.command.api.service;

/**
 * courtscheduler gave no answer to a lookup the listing depends on: an error status, a timeout or
 * no connection. Not the same as an answer that the booking is gone, so it carries no
 * {@code [LISTING-LOST]} marker - the request fails and is retried, and a courtscheduler that
 * recovers lists the hearing.
 */
public class CourtSchedulerUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CourtSchedulerUnavailableException(final String message) {
        super(message);
    }
}

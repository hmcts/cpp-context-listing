package uk.gov.moj.cpp.listing.persistence.repository.outbox;

public class EventOutboxException extends RuntimeException {

    public EventOutboxException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

package uk.gov.moj.cpp.listing.query.view.exception;

/** Replaces the framework's {@code BadRequestException} thrown by query code: the HTTP layer answers 400. */
public class QueryBadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QueryBadRequestException(final String message) {
        super(message);
    }

    public QueryBadRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

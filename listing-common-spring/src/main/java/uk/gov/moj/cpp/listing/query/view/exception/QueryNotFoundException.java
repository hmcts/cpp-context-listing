package uk.gov.moj.cpp.listing.query.view.exception;

/** Replaces JAX-RS {@code NotFoundException} thrown by the query view: the HTTP layer answers 404. */
public class QueryNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QueryNotFoundException(final String message) {
        super(message);
    }
}

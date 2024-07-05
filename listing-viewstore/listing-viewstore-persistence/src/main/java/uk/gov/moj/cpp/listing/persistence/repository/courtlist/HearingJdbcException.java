package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

public class HearingJdbcException extends RuntimeException {

    private static final long serialVersionUID = 5934757852542630746L;

    public HearingJdbcException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

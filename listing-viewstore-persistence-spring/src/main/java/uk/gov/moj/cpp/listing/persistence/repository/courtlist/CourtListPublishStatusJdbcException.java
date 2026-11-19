package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

public class CourtListPublishStatusJdbcException extends RuntimeException {

    private static final long serialVersionUID = 5934757852541630746L;

    public CourtListPublishStatusJdbcException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

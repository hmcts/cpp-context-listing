package uk.gov.moj.cpp.listing.persistence.repository.snapshot;

public class AggregateSnapshotException extends RuntimeException {

    public AggregateSnapshotException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

package uk.gov.moj.cpp.listing.common.client;

/** A remote query answered 400 (the framework threw {@code BadRequestException}; the caller's request gets 400). */
public class RemoteBadRequestException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public RemoteBadRequestException(final String message) {
        super(message);
    }
}

package uk.gov.moj.cpp.listing.common.client;

/** A remote query answered 403 (the framework threw {@code AccessControlViolationException}; the caller's request gets 403). */
public class RemoteAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RemoteAccessDeniedException(final String message) {
        super(message);
    }
}

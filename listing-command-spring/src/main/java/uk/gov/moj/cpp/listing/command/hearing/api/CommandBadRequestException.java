package uk.gov.moj.cpp.listing.command.hearing.api;

/**
 * The framework REST adapter's {@code BadRequestException} thrown by the command API (validation of
 * a command before it is handled): {@code 400} with the message (listing-api maps it).
 */
public class CommandBadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CommandBadRequestException(final String message) {
        super(message);
    }

    public CommandBadRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

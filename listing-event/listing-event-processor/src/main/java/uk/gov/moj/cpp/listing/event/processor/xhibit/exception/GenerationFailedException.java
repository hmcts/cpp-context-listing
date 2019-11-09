package uk.gov.moj.cpp.listing.event.processor.xhibit.exception;

public class GenerationFailedException extends RuntimeException {
    public GenerationFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

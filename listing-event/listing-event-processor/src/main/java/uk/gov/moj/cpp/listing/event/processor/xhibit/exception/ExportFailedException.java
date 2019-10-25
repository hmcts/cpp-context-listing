package uk.gov.moj.cpp.listing.event.processor.xhibit.exception;

public class ExportFailedException extends Exception {

    public ExportFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

package uk.gov.moj.cpp.listing.event.processor.xhibit.exception;

import java.net.MalformedURLException;

public class UrlCreationFailedException extends RuntimeException {
    public UrlCreationFailedException(final String message, final MalformedURLException e) {
        super(message, e);
    }
}

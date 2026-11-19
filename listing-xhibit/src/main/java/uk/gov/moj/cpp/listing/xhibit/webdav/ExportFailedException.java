package uk.gov.moj.cpp.listing.xhibit.webdav;

/**
 * The WebDAV export to XHIBIT failed (same name and messages as the JEE
 * {@code uk.gov.moj.cpp.listing.common.xhibit.ExportFailedException}: the recorded
 * {@code errorMessage} of a failed export starts with this class's simple name).
 */
public class ExportFailedException extends Exception {

    public ExportFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

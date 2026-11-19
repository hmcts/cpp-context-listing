package uk.gov.moj.cpp.listing.messaging.envelope;

/** A message body that is not a framework JSON envelope (no {@code _metadata}, bad id or name, not JSON). */
public class EnvelopeFormatException extends RuntimeException {

    public EnvelopeFormatException(final String message) {
        super(message);
    }

    public EnvelopeFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

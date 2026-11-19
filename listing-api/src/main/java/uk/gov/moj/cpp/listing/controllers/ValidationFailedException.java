package uk.gov.moj.cpp.listing.controllers;

import java.util.List;

/**
 * Thrown by services to signal that input failed validation. Mapped to
 * HTTP 400 by {@link GlobalExceptionHandler} with the legacy
 * {@code {"error":"<msg1>; <msg2>"}} response shape.
 */
public class ValidationFailedException extends RuntimeException {

    private final List<String> errorMessages;

    public ValidationFailedException(final List<String> errorMessages) {
        super(String.join("; ", errorMessages));
        this.errorMessages = List.copyOf(errorMessages);
    }

    public ValidationFailedException(final String singleMessage) {
        this(List.of(singleMessage));
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}

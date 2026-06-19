package uk.gov.moj.cpp.listing.persistence.repository;

public class JsonUpdateException extends RuntimeException {
    public JsonUpdateException() {
    }

    public JsonUpdateException(String message) {
        super(message);
    }

    public JsonUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonUpdateException(Throwable cause) {
        super(cause);
    }

    public JsonUpdateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package uk.gov.moj.cpp.listing.query.document.generator.exception;

public class DocumentGenerationFailedException extends RuntimeException {

    public DocumentGenerationFailedException(String message) {
        super(message);
    }
    public DocumentGenerationFailedException(Exception e) {
        super(e);
    }
}

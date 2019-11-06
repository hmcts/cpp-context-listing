package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

public class CourtListMetadata {

    private String filename;
    private String documentUniqueId;

    public CourtListMetadata(final String filename, final String documentUniqueId) {
        this.filename = filename;
        this.documentUniqueId = documentUniqueId;
    }

    public String getFilename() {
        return filename;
    }

    public String getDocumentUniqueId() {
        return documentUniqueId;
    }
}

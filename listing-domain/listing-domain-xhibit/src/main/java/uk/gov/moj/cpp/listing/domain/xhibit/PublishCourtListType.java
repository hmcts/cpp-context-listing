package uk.gov.moj.cpp.listing.domain.xhibit;

public enum PublishCourtListType {
    WARN("WarnedList", "WL", "WarnedList.xsd"),
    DRAFT("DraftList", "DL", "DailyList.xsd"),
    FINAL("DailyList", "DL", "DailyList.xsd"),
    FIRM("DailyList", "FL", "FirmList.xsd");

    private final String filenamePrefix;
    private final String documentType;
    private final String schemaName;

    PublishCourtListType(final String filenamePrefix, final String documentType, final String schemaName) {
        this.filenamePrefix = filenamePrefix;
        this.documentType = documentType;
        this.schemaName = schemaName;
    }

    public String getFilenamePrefix() {
        return filenamePrefix;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getSchemaName() {
        return schemaName;
    }

}

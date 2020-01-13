package uk.gov.moj.cpp.listing.domain.xhibit;

public enum PublishCourtListType {
    WARN("WarnedList", "WL", "WarnedList.xsd", true),
    DRAFT("DailyList", "DL", "DailyList.xsd", false),
    FINAL("DailyList", "DL", "DailyList.xsd", false),
    FIRM("FirmedList", "FL", "FirmList.xsd", true);

    private final String filenamePrefix;
    private final String documentType;
    private final String schemaName;
    private final boolean isWeekCommencing;

    PublishCourtListType(final String filenamePrefix, final String documentType, final String schemaName, final boolean isWeekCommencing) {
        this.filenamePrefix = filenamePrefix;
        this.documentType = documentType;
        this.schemaName = schemaName;
        this.isWeekCommencing = isWeekCommencing;
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

    public boolean isWeekCommencing() {
        return isWeekCommencing;
    }
}

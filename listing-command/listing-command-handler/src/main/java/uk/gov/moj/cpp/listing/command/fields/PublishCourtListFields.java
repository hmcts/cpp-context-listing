package uk.gov.moj.cpp.listing.command.fields;

public enum PublishCourtListFields {

    COURT_CENTRE_ID("courtCentreId"),
    START_DATE("startDate"),
    END_DATE("endDate"),
    TYPE("publishCourtListType"),
    REQUESTED_TIME("requestedTime");

    private String internalName;

    PublishCourtListFields(final String internalName) {
        this.internalName = internalName;
    }

    public String getInternalName() {
        return internalName;
    }
}

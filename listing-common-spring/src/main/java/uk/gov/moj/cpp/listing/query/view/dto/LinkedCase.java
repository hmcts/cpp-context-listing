package uk.gov.moj.cpp.listing.query.view.dto;

public class LinkedCase {

    private final String caseId;
    private final String caseUrn;

    public LinkedCase(final String caseId, final String caseUrn) {
        this.caseId = caseId;
        this.caseUrn = caseUrn;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

}

package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class LinkedCaseData {
    private final UUID caseId;
    private final String caseUrn;

    public LinkedCaseData(UUID caseId, String caseUrn) {
        this.caseId = caseId;
        this.caseUrn = caseUrn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }
}

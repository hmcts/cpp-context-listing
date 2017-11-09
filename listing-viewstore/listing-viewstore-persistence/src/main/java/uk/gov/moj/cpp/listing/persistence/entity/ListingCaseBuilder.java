package uk.gov.moj.cpp.listing.persistence.entity;

import java.util.UUID;

public class ListingCaseBuilder {
    private UUID caseId;
    private String urn;

    public ListingCaseBuilder setCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public ListingCaseBuilder setUrn(final String urn) {
        this.urn = urn;
        return this;
    }

    public ListingCase build() {
        return new ListingCase(caseId, urn);
    }
}
package uk.gov.moj.cpp.listing.persistence.entity;

import java.util.UUID;

public class ListingCaseBuilder {
    private UUID caseProgressionId;
    private String urn;

    public ListingCaseBuilder setCaseProgressionId(final UUID caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
        return this;
    }

    public ListingCaseBuilder setUrn(final String urn) {
        this.urn = urn;
        return this;
    }

    public ListingCase build() {
        return new ListingCase(caseProgressionId, urn);
    }
}
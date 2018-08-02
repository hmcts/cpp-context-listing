package uk.gov.moj.cpp.listing.persistence.entity;

import java.util.UUID;

public class CompositeDefendantIdBuilder {
    private UUID hearingId;
    private UUID defendantId;

    public CompositeDefendantIdBuilder setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
        return this;
    }

    public CompositeDefendantIdBuilder setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
        return this;
    }

    public CompositeDefendantId build() {
        return new CompositeDefendantId(hearingId, defendantId);
    }
}
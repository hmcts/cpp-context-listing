package uk.gov.moj.cpp.listing.persistence.entity;

import java.util.UUID;

public class CompositeOffenceIdBuilder {
    private UUID hearingId;
    private UUID offenceId;
    private UUID defendantId;

    public CompositeOffenceIdBuilder setOffenceId(UUID offenceId) {
        this.offenceId = offenceId;
        return this;
    }

    public CompositeOffenceIdBuilder setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
        return this;
    }

    public CompositeOffenceIdBuilder setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
        return this;
    }

    public CompositeOffenceId build() {
        return new CompositeOffenceId(hearingId, offenceId, defendantId);
    }
}
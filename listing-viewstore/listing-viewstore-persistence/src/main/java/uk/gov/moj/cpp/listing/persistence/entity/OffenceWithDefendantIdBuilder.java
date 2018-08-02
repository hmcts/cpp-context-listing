package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

public class OffenceWithDefendantIdBuilder {
    private CompositeOffenceId id;
    private String offenceCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private StatementOfOffence statementOfOffence;
    private UUID mappedDefendantId;
    private UUID mappedHearingId;

    public OffenceWithDefendantIdBuilder setId(CompositeOffenceId id) {
        this.id = id;
        return this;
    }

    public OffenceWithDefendantIdBuilder setOffenceCode(String offenceCode) {
        this.offenceCode = offenceCode;
        return this;
    }

    public OffenceWithDefendantIdBuilder setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public OffenceWithDefendantIdBuilder setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public OffenceWithDefendantIdBuilder setStatementOfOffence(StatementOfOffence statementOfOffence) {
        this.statementOfOffence = statementOfOffence;
        return this;
    }

    public OffenceWithDefendantIdBuilder setMappedDefendantId(UUID mappedDefendantId) {
        this.mappedDefendantId = mappedDefendantId;
        return this;
    }

    public OffenceWithDefendantIdBuilder setMappedHearingId(UUID mappedHearingId) {
        this.mappedHearingId = mappedHearingId;
        return this;
    }

    public OffenceWithDefendantId build() {
        return new OffenceWithDefendantId(id, offenceCode, startDate, endDate, statementOfOffence, mappedDefendantId, mappedHearingId);
    }
}
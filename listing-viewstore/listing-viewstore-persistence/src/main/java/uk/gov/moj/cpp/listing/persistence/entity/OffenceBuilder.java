package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;

public class OffenceBuilder {
    private CompositeOffenceId id;
    private String offenceCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private StatementOfOffence statementOfOffence;
    private Defendant defendant;

    public OffenceBuilder setId(CompositeOffenceId id) {
        this.id = id;
        return this;
    }

    public OffenceBuilder setOffenceCode(String offenceCode) {
        this.offenceCode = offenceCode;
        return this;
    }

    public OffenceBuilder setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public OffenceBuilder setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public OffenceBuilder setStatementOfOffence(StatementOfOffence statementOfOffence) {
        this.statementOfOffence = statementOfOffence;
        return this;
    }

    public OffenceBuilder setDefendant(Defendant defendant) {
        this.defendant = defendant;
        return this;
    }

    public Offence build() {
        return new Offence(id, offenceCode, startDate, endDate, statementOfOffence, defendant);
    }
}
package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;

public class BaseOffenceBuilder {
    private CompositeOffenceId id;
    private String offenceCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private StatementOfOffence statementOfOffence;

    public BaseOffenceBuilder setId(CompositeOffenceId id) {
        this.id = id;
        return this;
    }

    public BaseOffenceBuilder setOffenceCode(String offenceCode) {
        this.offenceCode = offenceCode;
        return this;
    }

    public BaseOffenceBuilder setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public BaseOffenceBuilder setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public BaseOffenceBuilder setStatementOfOffence(StatementOfOffence statementOfOffence) {
        this.statementOfOffence = statementOfOffence;
        return this;
    }

    public BaseOffence build() {
        return new BaseOffence(id, offenceCode, startDate, endDate, id.getDefendantId(), statementOfOffence);
    }
}
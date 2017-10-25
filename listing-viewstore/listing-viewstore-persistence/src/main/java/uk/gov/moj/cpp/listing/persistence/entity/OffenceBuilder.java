package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

public class OffenceBuilder {
    private UUID listingOffenceId;
    private UUID offenceId;
    private String offenceCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private StatementOfOffence statementOfOffence;
    private Defendant defendant;

    public OffenceBuilder setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
        return this;
    }

    public OffenceBuilder setListingOffenceId(final UUID listingOffenceId) {
        this.listingOffenceId = listingOffenceId;
        return this;
    }

    public OffenceBuilder setOffenceCode(final String offenceCode) {
        this.offenceCode = offenceCode;
        return this;
    }

    public OffenceBuilder setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public OffenceBuilder setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public OffenceBuilder setStatementOfOffence(final StatementOfOffence statementOfOffence) {
        this.statementOfOffence = statementOfOffence;
        return this;
    }

    public OffenceBuilder setDefendant(final Defendant defendant) {
        this.defendant = defendant;
        return this;
    }

    public Offence build() {
        return new Offence(listingOffenceId, offenceId, offenceCode, statementOfOffence, defendant, new Offence.OffencePeriod(startDate, endDate));
    }
}
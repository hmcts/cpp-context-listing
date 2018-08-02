package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings({"squid:S2160"}) // Super class uses unique ID to test for equality
@Entity
@Table(name = "offence")
public class BaseOffence extends AbstractOffence {

    @Column(name = "mapped_defendant_id", nullable = false)
    private UUID defendantId;

    @Embedded
    private StatementOfOffence statementOfOffence;

    public BaseOffence() {
        //Required for JPA
    }

    public BaseOffence(
            CompositeOffenceId id,
            String offenceCode,
            LocalDate startDate,
            LocalDate endDate,
            UUID defendantId,
            StatementOfOffence statementOfOffence) {
        super(id, offenceCode, startDate, endDate);
        this.defendantId = defendantId;
        this.statementOfOffence = statementOfOffence;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public StatementOfOffence getStatementOfOffence() { return statementOfOffence; }
}


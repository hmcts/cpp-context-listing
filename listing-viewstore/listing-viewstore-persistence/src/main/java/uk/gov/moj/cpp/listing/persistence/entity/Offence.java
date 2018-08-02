package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.time.LocalDate;

@SuppressWarnings({"squid:S2160"}) // Super class uses unique ID to test for equality
@Entity
@Table(name = "offence")
public class Offence extends AbstractOffence {

    @ManyToOne
    @JoinColumns({
            @JoinColumn(
                    name = "mapped_defendant_id",
                    referencedColumnName = "defendant_id"),
            @JoinColumn(
                    name = "mapped_hearing_id",
                    referencedColumnName = "hearing_id")
    })
    private Defendant defendant;


    @Embedded
    private StatementOfOffence statementOfOffence;

    public Offence() {
        //Required for JPA
    }

    public Offence(CompositeOffenceId id,
                   String offenceCode,
                   LocalDate startDate,
                   LocalDate endDate,
                   StatementOfOffence statementOfOffence,
                   Defendant defendant) {
        super(id, offenceCode, startDate, endDate);
        this.defendant = defendant;
        this.statementOfOffence = statementOfOffence;
    }

    public Defendant getDefendant() { return defendant; }

    public StatementOfOffence getStatementOfOffence() { return statementOfOffence; }

    public void setDefendant(Defendant defendant) { this.defendant = defendant; }

}


package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "offence")
public class Offence implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "offence_code")
    private String offenceCode;

    @Column(name = "start_date_time")
    private LocalDate startDate;

    @Column(name = "end_date_time")
    private LocalDate endDate;

    @Column(name = "plea")
    private String plea;

    @Embedded
    private StatementOfOffence statementOfOffence;

    @ManyToOne
    @JoinColumn(name = "defendant_id")
    private Defendant defendant;

    public Offence() {
        //Required for JPA
    }

    public Offence(final UUID id, final String offenceCode, final LocalDate startDate,
                   final LocalDate endDate, final String plea,
                   final StatementOfOffence statementOfOffence, final Defendant defendant) {
        this.id = id;
        this.offenceCode = offenceCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.plea = plea;
        this.statementOfOffence = statementOfOffence;
        this.defendant = defendant;
    }

    public UUID getId() { return id; }

    public String getOffenceCode() { return offenceCode; }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getEndDate() { return endDate; }

    public String getPlea() { return plea; }

    public StatementOfOffence getStatementOfOffence() { return statementOfOffence; }

    public Defendant getDefendant() { return defendant; }

    public void setDefendant(Defendant defendant) { this.defendant = defendant; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Offence offence = (Offence) o;

        return id.equals(offence.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}


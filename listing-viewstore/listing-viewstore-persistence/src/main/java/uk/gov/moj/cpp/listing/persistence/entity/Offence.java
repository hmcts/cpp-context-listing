package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "offence")
public class Offence implements Serializable {

    @Id
    @Column(name = "listing_offence_id", unique = true, nullable = false)
    private UUID listingOffenceId;

    @Column(name = "offence_id", nullable = false)
    private UUID offenceId;

    @Column(name = "offence_code")
    private String offenceCode;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Embedded
    private StatementOfOffence statementOfOffence;

    @ManyToOne
    @JoinColumn(name = "listing_defendant_id")
    private Defendant defendant;

    public Offence() {
        //Required for JPA
    }

    public Offence(final UUID listingOffenceId, final UUID offenceId, final String offenceCode,
                   final StatementOfOffence statementOfOffence, final Defendant defendant,
                   final OffencePeriod offencePeriod) {
        this.offenceId = offenceId;
        this.listingOffenceId = listingOffenceId;
        this.offenceCode = offenceCode;
        this.startDate = offencePeriod.getStartDate();
        this.endDate = offencePeriod.getEndDate();
        this.statementOfOffence = statementOfOffence;
        this.defendant = defendant;
    }

    public UUID getListingOffenceId() {
        return listingOffenceId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public StatementOfOffence getStatementOfOffence() {
        return statementOfOffence;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public void setDefendant(Defendant defendant) {
        this.defendant = defendant;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Offence offence = (Offence) o;
        return listingOffenceId.equals(offence.listingOffenceId);
    }

    @Override
    public int hashCode() {
        return listingOffenceId.hashCode();
    }

    public static class OffencePeriod {

        private LocalDate startDate;
        private LocalDate endDate;

        public OffencePeriod(final LocalDate startDate, final LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }


        public LocalDate getEndDate() {
            return endDate;
        }

    }
}


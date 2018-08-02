package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@MappedSuperclass
public class AbstractOffence implements Serializable {

    @EmbeddedId
    private CompositeOffenceId id;

    @Column(name = "offence_code")
    private String offenceCode;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public AbstractOffence() {
        //Required for JPA
    }

    public AbstractOffence(
            CompositeOffenceId id,
            String offenceCode,
            LocalDate startDate,
            LocalDate endDate) {
        this.id = id;
        this.offenceCode = offenceCode;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public CompositeOffenceId getId() { return id; }

    public String getOffenceCode() { return offenceCode; }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getEndDate() { return endDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AbstractOffence that = (AbstractOffence) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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


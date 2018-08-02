package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class Offence extends SimpleOffence implements Serializable {

    private final String offenceCode;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final StatementOfOffence statementOfOffence;

    @JsonCreator
    public Offence(@JsonProperty(value = "id") final String id,
                   @JsonProperty(value = "offenceCode") final String offenceCode,
                   @JsonProperty(value = "startDate") final LocalDate startDate,
                   @JsonProperty(value = "endDate") final LocalDate endDate,
                   @JsonProperty(value = "statementOfOffence") final StatementOfOffence statementOfOffence,
                   @JsonProperty(value = "defendantId") final String defendantId) {
        super(id, defendantId);
        this.offenceCode = offenceCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.statementOfOffence = statementOfOffence;
    }

    public String getOffenceCode() { return offenceCode; }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public StatementOfOffence getStatementOfOffence() {
        return statementOfOffence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Offence offence = (Offence) o;
        return Objects.equals(offenceCode, offence.offenceCode) &&
                Objects.equals(startDate, offence.startDate) &&
                Objects.equals(endDate, offence.endDate) &&
                Objects.equals(statementOfOffence, offence.statementOfOffence);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), offenceCode, startDate, endDate, statementOfOffence);
    }

    @Override
    public String toString() {
        return "Offence{" +
                "offenceCode='" + offenceCode + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", statementOfOffence=" + statementOfOffence +
                '}';
    }


    public static OffenceBuilder createOffenceBuilder() {
        return new OffenceBuilder();
    }

    public static class OffenceBuilder {
        private String id;
        private String offenceCode;
        private LocalDate startDate;
        private LocalDate endDate;
        private StatementOfOffence statementOfOffence;
        private String defendantId;

        public OffenceBuilder setId(String id) {
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

        public OffenceBuilder setDefendantId(String defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Offence build() {
            return new Offence(id, offenceCode, startDate, endDate, statementOfOffence, defendantId);
        }
    }}

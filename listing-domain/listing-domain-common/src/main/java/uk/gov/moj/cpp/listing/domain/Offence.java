package uk.gov.moj.cpp.listing.domain;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00121", "PMD:BeanMembersShouldSerialize", "squid:S00107", "squid:S00122"})
public class Offence {
    private final Optional<String> endDate;

    private final UUID id;

    private final String offenceCode;

    private final String offenceWording;

    private final String startDate;

    private final StatementOfOffence statementOfOffence;

    private final Optional<CustodyTimeLimit> custodyTimeLimit;

    private final Optional<LaaReference> laaApplnReference;

    private final Optional<String> laidDate;

    public Offence(final Optional<String> endDate, final UUID id, final String offenceCode, final String offenceWording, final String startDate, final StatementOfOffence statementOfOffence, final Optional<CustodyTimeLimit> custodyTimeLimit, final Optional<LaaReference> laaApplnReference, final Optional<String> laidDate) {
        this.endDate = endDate;
        this.id = id;
        this.offenceCode = offenceCode;
        this.offenceWording = offenceWording;
        this.startDate = startDate;
        this.statementOfOffence = statementOfOffence;
        this.custodyTimeLimit = custodyTimeLimit;
        this.laaApplnReference = laaApplnReference;
        this.laidDate = laidDate;
    }

    public Optional<String> getEndDate() {
        return endDate;
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public Optional<LaaReference> getLaaApplnReference() {
        return laaApplnReference;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public String getStartDate() {
        return startDate;
    }

    public StatementOfOffence getStatementOfOffence() {
        return statementOfOffence;
    }

    public Optional<CustodyTimeLimit> getCustodyTimeLimit(){ return custodyTimeLimit;}

    public Optional<String> getLaidDate() {
        return laidDate;
    }

    public static Builder offence() {
        return new Offence.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final Offence that = (Offence) obj;

        return java.util.Objects.equals(this.endDate, that.endDate) &&
                java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.offenceCode, that.offenceCode) &&
                java.util.Objects.equals(this.offenceWording, that.offenceWording) &&
                java.util.Objects.equals(this.startDate, that.startDate) &&
                java.util.Objects.equals(this.statementOfOffence, that.statementOfOffence) &&
                java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
                java.util.Objects.equals(this.laidDate, that.laidDate) &&
                java.util.Objects.equals(this.laaApplnReference, that.laaApplnReference);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(endDate, id, offenceCode, offenceWording, startDate, statementOfOffence, custodyTimeLimit, laaApplnReference, laidDate);
    }

    @Override
    public String toString() {
        return "Offence{" +
                "endDate='" + endDate + "'," +
                "id='" + id + "'," +
                "offenceCode='" + offenceCode + "'," +
                "offenceWording='" + offenceWording + "'," +
                "startDate='" + startDate + "'," +
                "statementOfOffence='" + statementOfOffence + "'," +
                "custodyTimeLimit='" + custodyTimeLimit + "'" +
                "laaApplnReference='" + laaApplnReference + "'" +
                "laidDate='" + laidDate + "'" +
                "}";
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {
        private Optional<String> endDate;

        private UUID id;

        private String offenceCode;

        private String offenceWording;

        private String startDate;

        private StatementOfOffence statementOfOffence;

        private Optional<CustodyTimeLimit> custodyTimeLimit;

        private Optional<String> laidDate;

        private Optional<LaaReference> laaApplnReference;

        public Builder withEndDate(final Optional<String> endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withOffenceCode(final String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder withOffenceWording(final String offenceWording) {
            this.offenceWording = offenceWording;
            return this;
        }

        public Builder withStartDate(final String startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withCustodyTimeLimit(final Optional<CustodyTimeLimit> custodyTimeLimit) {
            this.custodyTimeLimit = custodyTimeLimit;
            return this;
        }

        public Builder withStatementOfOffence(final StatementOfOffence statementOfOffence) {
            this.statementOfOffence = statementOfOffence;
            return this;
        }

        public Builder withLaaApplnReference(final Optional<LaaReference> laaApplnReferences) {
            this.laaApplnReference = laaApplnReferences;
            return this;
        }

        public Builder withLaidDate(final Optional<String> laidDate) {
            this.laidDate = laidDate;
            return this;
        }

        public Offence build() {
            return new Offence(endDate, id, offenceCode, offenceWording, startDate, statementOfOffence, custodyTimeLimit, laaApplnReference, laidDate);
        }
    }
}

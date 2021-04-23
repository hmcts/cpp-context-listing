package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00121", "pmd:BeanMembersShouldSerialize", "squid:S00107", "squid:S00122", "squid:S2384"})
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

    private final Optional<Boolean> shadowListed;

    private final Optional<CommittingCourt> committingCourt;

    private final List<ReportingRestriction> reportingRestrictions;

    private final Optional<SeedingHearing> seedingHearing;

    public Offence(final Optional<String> endDate, final UUID id, final String offenceCode, final String offenceWording,
                   final String startDate, final StatementOfOffence statementOfOffence, final Optional<CustodyTimeLimit> custodyTimeLimit,
                   final Optional<LaaReference> laaApplnReference, final Optional<String> laidDate, final Optional<Boolean> shadowListed,
                   final Optional<CommittingCourt> committingCourt, final List<ReportingRestriction> reportingRestrictions,
                   final Optional<SeedingHearing> seedingHearing ) {
        this.endDate = endDate;
        this.id = id;
        this.offenceCode = offenceCode;
        this.offenceWording = offenceWording;
        this.startDate = startDate;
        this.statementOfOffence = statementOfOffence;
        this.custodyTimeLimit = custodyTimeLimit;
        this.laaApplnReference = laaApplnReference;
        this.laidDate = laidDate;
        this.shadowListed = shadowListed;
        this.committingCourt = committingCourt;
        this.reportingRestrictions = reportingRestrictions;
        this.seedingHearing = seedingHearing;
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

    public Optional<CustodyTimeLimit> getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public Optional<String> getLaidDate() {
        return laidDate;
    }

    public Optional<Boolean> getShadowListed() {
        return shadowListed;
    }

    public Optional<CommittingCourt> getCommittingCourt() {
        return committingCourt;
    }

    public List<ReportingRestriction> getReportingRestrictions() {
        return reportingRestrictions;
    }

    public Optional<SeedingHearing> getSeedingHearing() {
        return seedingHearing;
    }

    public static Builder offence() {
        return new Offence.Builder();
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
        return Objects.equals(endDate, offence.endDate) &&
                Objects.equals(id, offence.id) &&
                Objects.equals(offenceCode, offence.offenceCode) &&
                Objects.equals(offenceWording, offence.offenceWording) &&
                Objects.equals(startDate, offence.startDate) &&
                Objects.equals(statementOfOffence, offence.statementOfOffence) &&
                Objects.equals(custodyTimeLimit, offence.custodyTimeLimit) &&
                Objects.equals(laaApplnReference, offence.laaApplnReference) &&
                Objects.equals(laidDate, offence.laidDate) &&
                Objects.equals(shadowListed, offence.shadowListed) &&
                Objects.equals(committingCourt, offence.committingCourt) &&
                Objects.equals(reportingRestrictions, offence.reportingRestrictions) &&
                Objects.equals(seedingHearing, offence.seedingHearing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endDate, id, offenceCode, offenceWording, startDate, statementOfOffence, custodyTimeLimit, laaApplnReference, laidDate, shadowListed, committingCourt, reportingRestrictions, seedingHearing);
    }

    @Override
    public String toString() {
        return "Offence{" +
                "endDate=" + endDate +
                ", id=" + id +
                ", offenceCode='" + offenceCode + '\'' +
                ", offenceWording='" + offenceWording + '\'' +
                ", startDate='" + startDate + '\'' +
                ", statementOfOffence=" + statementOfOffence +
                ", custodyTimeLimit=" + custodyTimeLimit +
                ", laaApplnReference=" + laaApplnReference +
                ", laidDate=" + laidDate +
                ", shadowListed=" + shadowListed +
                ", committingCourt=" + committingCourt +
                ", reportingRestrictions=" + reportingRestrictions +
                ", seedingHearing=" + seedingHearing +
                '}';
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

        private Optional<Boolean> shadowListed;

        private Optional<CommittingCourt> committingCourt;

        private List<ReportingRestriction> reportingRestrictions;

        private Optional<SeedingHearing> seedingHearing;

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

        public Builder withShadowListed(final Optional<Boolean> shadowListed) {
            this.shadowListed = shadowListed;
            return this;
        }

        public Builder withCommittingCourt(final Optional<CommittingCourt> committingCourt) {
            this.committingCourt = committingCourt;
            return this;
        }

        public Builder withReportingRestrictions(final List<ReportingRestriction> reportingRestrictions) {
            this.reportingRestrictions = reportingRestrictions;
            return this;
        }

        public Builder withSeedingHearing(final Optional<SeedingHearing> seedingHearing) {
            this.seedingHearing = seedingHearing;
            return this;
        }

        public Offence build() {
            return new Offence(endDate, id, offenceCode, offenceWording, startDate, statementOfOffence, custodyTimeLimit, laaApplnReference, laidDate, shadowListed, committingCourt, reportingRestrictions, seedingHearing);
        }
    }
}

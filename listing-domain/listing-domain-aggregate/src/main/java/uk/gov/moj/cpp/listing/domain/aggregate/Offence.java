package uk.gov.moj.cpp.listing.domain.aggregate;

import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize"})
public class Offence implements Serializable {
    private final String endDate;

    private final UUID id;

    private final LaaReference laaApplnReference;

    private final String laidDate;

    private final String offenceCode;

    private final String offenceWording;

    private final Boolean restrictFromCourtList;

    private final String startDate;

    private final StatementOfOffence statementOfOffence;

    public Offence(final String endDate, final UUID id, final LaaReference laaApplnReference, final String laidDate, final String offenceCode, final String offenceWording, final Boolean restrictFromCourtList, final String startDate, final StatementOfOffence statementOfOffence) {
        this.endDate = endDate;
        this.id = id;
        this.laaApplnReference = laaApplnReference;
        this.laidDate = laidDate;
        this.offenceCode = offenceCode;
        this.offenceWording = offenceWording;
        this.restrictFromCourtList = restrictFromCourtList;
        this.startDate = startDate;
        this.statementOfOffence = statementOfOffence;
    }

    public String getEndDate() {
        return endDate;
    }

    public UUID getId() {
        return id;
    }

    public LaaReference getLaaApplnReference() {
        return laaApplnReference;
    }

    public String getLaidDate() {
        return laidDate;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public Boolean getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public String getStartDate() {
        return startDate;
    }

    public StatementOfOffence getStatementOfOffence() {
        return statementOfOffence;
    }

    public static Builder offence() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Offence that = (Offence) obj;

        return java.util.Objects.equals(this.endDate, that.endDate) &&
                java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.laaApplnReference, that.laaApplnReference) &&
                java.util.Objects.equals(this.laidDate, that.laidDate) &&
                java.util.Objects.equals(this.offenceCode, that.offenceCode) &&
                java.util.Objects.equals(this.offenceWording, that.offenceWording) &&
                java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList) &&
                java.util.Objects.equals(this.startDate, that.startDate) &&
                java.util.Objects.equals(this.statementOfOffence, that.statementOfOffence);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(endDate, id, laaApplnReference, laidDate, offenceCode, offenceWording, restrictFromCourtList, startDate, statementOfOffence);
    }

    @Override
    public String toString() {
        return "Offence{" +
                "endDate='" + endDate + "'," +
                "id='" + id + "'," +
                "laaApplnReference='" + laaApplnReference + "'," +
                "laidDate='" + laidDate + "'," +
                "offenceCode='" + offenceCode + "'," +
                "offenceWording='" + offenceWording + "'," +
                "restrictFromCourtList='" + restrictFromCourtList + "'," +
                "startDate='" + startDate + "'," +
                "statementOfOffence='" + statementOfOffence + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static final class Builder {
        private String endDate;

        private UUID id;

        private LaaReference laaApplnReference;

        private String laidDate;

        private String offenceCode;

        private String offenceWording;

        private Boolean restrictFromCourtList;

        private String startDate;

        private StatementOfOffence statementOfOffence;

        public Builder withEndDate(final String endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withLaaApplnReference(final LaaReference laaApplnReference) {
            this.laaApplnReference = laaApplnReference;
            return this;
        }

        public Builder withLaidDate(final String laidDate) {
            this.laidDate = laidDate;
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

        public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
            this.restrictFromCourtList = restrictFromCourtList;
            return this;
        }

        public Builder withStartDate(final String startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withStatementOfOffence(final StatementOfOffence statementOfOffence) {
            this.statementOfOffence = statementOfOffence;
            return this;
        }

        public Offence build() {
            return new Offence(endDate, id, laaApplnReference, laidDate, offenceCode, offenceWording, restrictFromCourtList, startDate, statementOfOffence);
        }
    }
}

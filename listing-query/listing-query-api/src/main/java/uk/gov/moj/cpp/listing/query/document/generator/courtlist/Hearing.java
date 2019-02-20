package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;

@SuppressWarnings({"squid:S1067", "squid:S00107", "squid:S00121"})
public class Hearing {
    private Integer sequence;
    private String reportingRestrictionReason;
    private String welshReportingRestrictionReason;
    private String startTime;
    private String hearingType;
    private String welshHearingType;
    private String caseNumber;
    private String prosecutorType;
    private List<Defendant> defendants;

    public Integer getSequence() {
        return sequence;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getHearingType() {
        return hearingType;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getProsecutorType() {
        return prosecutorType;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public String getReportingRestrictionReason() {
        return reportingRestrictionReason;
    }

    public String getWelshReportingRestrictionReason() {
        return welshReportingRestrictionReason;
    }

    public String getWelshHearingType() {
        return welshHearingType;
    }

    public static Builder hearing() {
        return new Hearing.Builder();
    }


    public static final class Builder {
        private Integer sequence;
        private String startTime;
        private String hearingType;
        private String welshHearingType;
        private String caseNumber;
        private String prosecutorType;
        private List<Defendant> defendants;
        private String reportingRestrictionReason;
        private String welshReportingRestrictionReason;

        private Builder() {
        }


        public Builder withSequence(Integer sequence) {
            this.sequence = sequence;
            return this;
        }


        public Builder withHearingType(String hearingType) {
            this.hearingType = hearingType;
            return this;
        }

        public Builder withWelshHearingType(String welshHearingType) {
            this.welshHearingType = welshHearingType;
            return this;
        }

        public Builder withCaseNumber(String caseNumber) {
            this.caseNumber = caseNumber;
            return this;
        }


        public Builder withStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }


        public Builder withProsecutorType(String prosecutorType) {
            this.prosecutorType = prosecutorType;
            return this;
        }

        public Builder withReportingRestrictionReason(String reportingRestrictionReason) {
            this.reportingRestrictionReason = reportingRestrictionReason;
            return this;
        }

        public Builder withWelshReportingRestrictionReason(String welshReportingRestrictionReason) {
            this.welshReportingRestrictionReason = welshReportingRestrictionReason;
            return this;
        }

        public Builder withDefendants(List<Defendant> defendants) {
            this.defendants = defendants;
            return this;
        }


        public Hearing build() {
            final Hearing hearing = new Hearing();
            hearing.sequence = this.sequence;
            hearing.hearingType = this.hearingType;
            hearing.welshHearingType = this.welshHearingType;
            hearing.caseNumber = this.caseNumber;
            hearing.startTime = this.startTime;
            hearing.prosecutorType = this.prosecutorType;
            hearing.defendants = this.defendants;
            hearing.reportingRestrictionReason = this.reportingRestrictionReason;
            hearing.welshReportingRestrictionReason = this.welshReportingRestrictionReason;
            return hearing;
        }
    }

    @Override
    public String toString() {
        return "Hearing{" +
                "sequence=" + sequence +
                ", reportingRestrictionReason='" + reportingRestrictionReason + '\'' +
                ", welshReportingRestrictionReason='" + welshReportingRestrictionReason + '\'' +
                ", startTime='" + startTime + '\'' +
                ", hearingType='" + hearingType + '\'' +
                ", welshHearingType='" + welshHearingType + '\'' +
                ", caseNumber='" + caseNumber + '\'' +
                ", prosecutorType='" + prosecutorType + '\'' +
                ", defendants=" + defendants +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hearing)) return false;
        Hearing hearing = (Hearing) o;
        return Objects.equals(sequence, hearing.sequence) &&
                Objects.equals(reportingRestrictionReason, hearing.reportingRestrictionReason) &&
                Objects.equals(welshReportingRestrictionReason, hearing.welshReportingRestrictionReason) &&
                Objects.equals(startTime, hearing.startTime) &&
                Objects.equals(hearingType, hearing.hearingType) &&
                Objects.equals(welshHearingType, hearing.welshHearingType) &&
                Objects.equals(caseNumber, hearing.caseNumber) &&
                Objects.equals(prosecutorType, hearing.prosecutorType) &&
                Objects.equals(defendants, hearing.defendants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, reportingRestrictionReason, welshReportingRestrictionReason, startTime, hearingType, welshHearingType, caseNumber, prosecutorType, defendants);
    }
}

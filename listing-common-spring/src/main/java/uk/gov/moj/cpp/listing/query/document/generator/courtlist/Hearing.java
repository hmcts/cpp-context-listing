package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00107", "squid:S00121", "squid:S2384"})
public class Hearing {
    private UUID id;
    private Integer sequence;
    private String reportingRestrictionReason;
    private String welshReportingRestrictionReason;
    private String startTime;
    private String hearingType;
    private String welshHearingType;
    private String caseNumber;
    private UUID caseId;
    private String prosecutorType;
    private String hearingPublicListNote;
    private List<Defendant> defendants;
    private Defendant subject;
    private String adjournedHearingDate;
    private String panel;
    private UUID courtApplicationId;
    private List<Offence> applicationOffences;

    public String getAdjournedHearingDate() {
        return adjournedHearingDate;
    }

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

    public UUID getCaseId() {
        return caseId;
    }

    public String getProsecutorType() {
        return prosecutorType;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public Defendant getSubject() {
        return subject;
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

    public UUID getId() {
        return id;
    }

    public String getPanel() {
        return panel;
    }

    public UUID getCourtApplicationId() {
        return courtApplicationId;
    }

    public List<Offence> getApplicationOffences() { return applicationOffences; }

    public String getHearingPublicListNote() {
        return hearingPublicListNote;
    }
    public static Builder hearing() {
        return new Hearing.Builder();
    }


    public static final class Builder {
        private UUID id;
        private Integer sequence;
        private String startTime;
        private String hearingType;
        private String welshHearingType;
        private String caseNumber;
        private UUID caseId;
        private String prosecutorType;
        private List<Defendant> defendants;
        private Defendant subject;
        private String reportingRestrictionReason;
        private String welshReportingRestrictionReason;
        private String adjournedHearingDate;
        private String panel;
        private UUID courtApplicationId;
        private List<Offence> applicationOffences;
        private String hearingPublicListNote;

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

        public Builder withCaseId(UUID caseId) {
            this.caseId = caseId;
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

        public Builder withSubject(Defendant subject) {
            this.subject = subject;
            return this;
        }

        public Builder withAdjournedHearingDate(String adjournedHearingDate) {
            this.adjournedHearingDate = adjournedHearingDate;
            return this;
        }

        public Builder withId(UUID id){
            this.id = id;
            return this;
        }

        public Builder withPanel(String panel){
            this.panel = panel;
            return this;
        }

        public Builder withCourtApplicationId(UUID courtApplicationId){
            this.courtApplicationId = courtApplicationId;
            return this;
        }

        public Builder withApplicationOffences(List<Offence> applicationOffences){
            this.applicationOffences = applicationOffences;
            return this;
        }

        public Builder withHearingPublicListNote(final String hearingPublicListNote) {
            this.hearingPublicListNote = hearingPublicListNote;
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
            hearing.subject = this.subject;
            hearing.reportingRestrictionReason = this.reportingRestrictionReason;
            hearing.welshReportingRestrictionReason = this.welshReportingRestrictionReason;
            hearing.adjournedHearingDate = this.adjournedHearingDate;
            hearing.id = this.id;
            hearing.caseId = this.caseId;
            hearing.panel = this.panel;
            hearing.courtApplicationId = this.courtApplicationId;
            hearing.applicationOffences = this.applicationOffences;
            hearing.hearingPublicListNote = this.hearingPublicListNote;
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
                ", caseId='" + caseId + '\'' +
                ", prosecutorType='" + prosecutorType + '\'' +
                ", defendants=" + defendants +
                ", subject=" + subject +
                ", id=" + id +
                ", panel=" + panel +
                ", courtApplicationId=" + courtApplicationId +
                ", hearingPublicListNote=" + hearingPublicListNote +
                ", applicationOffences=" + applicationOffences +
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
                Objects.equals(caseId, hearing.caseId) &&
                Objects.equals(prosecutorType, hearing.prosecutorType) &&
                Objects.equals(defendants, hearing.defendants) &&
                Objects.equals(id, hearing.id) &&
                Objects.equals(panel, hearing.panel) &&
                Objects.equals(courtApplicationId, hearing.courtApplicationId) &&
                Objects.equals(hearingPublicListNote, hearing.hearingPublicListNote) &&
                Objects.equals(applicationOffences, hearing.applicationOffences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, reportingRestrictionReason, welshReportingRestrictionReason, startTime, hearingType, welshHearingType, caseNumber, caseId, prosecutorType, defendants, subject, id, panel, courtApplicationId, applicationOffences);
    }
}

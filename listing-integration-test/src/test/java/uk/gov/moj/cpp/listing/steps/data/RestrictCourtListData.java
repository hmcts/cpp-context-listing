package uk.gov.moj.cpp.listing.steps.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RestrictCourtListData {
    private final List<UUID> caseIds;

    private final List<UUID> defendantIds;

    private final UUID hearingId;

    private final List<UUID> offenceIds;

    private final Boolean restrictCourtList;

    private final List<UUID> courtApplicationApplicantIds;

    private final List<UUID> courtApplicationIds;

    private final List<UUID> courtApplicationRespondentIds;

    private final List<UUID> courtApplicationSubjectIds;

    private final Optional<String> courtApplicationType;

    public RestrictCourtListData(final List<UUID> caseIds, final List<UUID> courtApplicationApplicantIds, final List<UUID> courtApplicationIds, final List<UUID> courtApplicationRespondentIds, final List<UUID> courtApplicationSubjectIds, final List<UUID> defendantIds, final UUID hearingId, final List<UUID> offenceIds, final Boolean restrictCourtList, final Optional<String> courtApplicationType) {
        this.caseIds = caseIds;
        this.courtApplicationApplicantIds = courtApplicationApplicantIds;
        this.courtApplicationIds = courtApplicationIds;
        this.courtApplicationRespondentIds = courtApplicationRespondentIds;
        this.courtApplicationSubjectIds = courtApplicationSubjectIds;
        this.defendantIds = defendantIds;
        this.hearingId = hearingId;
        this.offenceIds = offenceIds;
        this.restrictCourtList = restrictCourtList;
        this.courtApplicationType = courtApplicationType;
    }

    public static Builder restrictCourtList() {
        return new RestrictCourtListData.Builder();
    }

    public List<UUID> getCaseIds() {
        return caseIds;
    }

    public List<UUID> getDefendantIds() {
        return defendantIds;
    }

    public Boolean getRestrictCourtList() {
        return restrictCourtList;
    }

    public List<UUID> getCourtApplicationApplicantIds() {
        return courtApplicationApplicantIds;
    }

    public List<UUID> getCourtApplicationIds() {
        return courtApplicationIds;
    }

    public List<UUID> getCourtApplicationRespondentIds() {
        return courtApplicationRespondentIds;
    }

    public List<UUID> getCourtApplicationSubjectIds() {
        return courtApplicationSubjectIds;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public List<UUID> getOffenceIds() {
        return offenceIds;
    }

    public Optional<String> getCourtApplicationType() {
        return courtApplicationType;
    }

    public static class Builder {
        private List<UUID> caseIds;

        private List<UUID> defendantIds;

        private UUID hearingId;

        private List<UUID> offenceIds;

        private Boolean restrictCourtList;

        private List<UUID> courtApplicationApplicantIds;

        private List<UUID> courtApplicatonIds;

        private List<UUID> courtApplicatonRespondentIds;

        private List<UUID> courtApplicationSubjectIds;

        private Optional<String> courtApplicationType;

        public Builder withCaseIds(final List<UUID> casesId) {
            this.caseIds = casesId;
            return this;
        }
        public Builder withCourtApplicationApplicantIds(final List<UUID> courtApplicationApplicantIds) {
            this.courtApplicationApplicantIds = courtApplicationApplicantIds;
            return this;
        }

        public Builder withCourtApplicatonIds(final List<UUID> courtApplicatonIds) {
            this.courtApplicatonIds = courtApplicatonIds;
            return this;
        }

        public Builder withCourtApplicatonRespondentIds(final List<UUID> courtApplicatonRespondentIds) {
            this.courtApplicatonRespondentIds = courtApplicatonRespondentIds;
            return this;
        }

        public Builder withCourtApplicationSubjectIds(final List<UUID> courtApplicationSubjectIds) {
            this.courtApplicationSubjectIds = courtApplicationSubjectIds;
            return this;
        }

        public Builder withDefendantIds(final List<UUID> defendantsId) {
            this.defendantIds = defendantsId;
            return this;
        }

        public Builder withHearingId(final UUID hearingId) {
            this.hearingId = hearingId;
            return this;
        }

        public Builder withOffenceIds(final List<UUID> offencesId) {
            this.offenceIds = offencesId;
            return this;
        }

        public Builder withRestrictCourtList(final Boolean restrictCourtList) {
            this.restrictCourtList = restrictCourtList;
            return this;
        }

        public Builder withCourtApplicationType(final Optional<String> courtApplicationType) {
            this.courtApplicationType = courtApplicationType;
            return this;
        }

        public RestrictCourtListData build() {
            return new RestrictCourtListData(caseIds, courtApplicationApplicantIds, courtApplicatonIds, courtApplicatonRespondentIds, courtApplicationSubjectIds, defendantIds, hearingId, offenceIds, restrictCourtList, courtApplicationType);
        }
    }
}

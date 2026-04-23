package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

@SuppressWarnings({"squid:S00107", "squid:S1067", "squid:S2384", "PMD:BeanMembersShouldSerialize"})
public class RestrictCourtList {
  private final List<UUID> caseIds;

  private final List<UUID> courtApplicationApplicantIds;

  private final List<UUID> courtApplicationIds;

  private final List<UUID> courtApplicationRespondentIds;

  private final List<UUID> courtApplicationSubjectIds;

  private final List<UUID> defendantIds;

  private final UUID hearingId;

  private final List<UUID> offenceIds;

  private final String courtApplicationType;

  private final Boolean restrictFromCourtList;

  public RestrictCourtList(final List<UUID> caseIds, final List<UUID> courtApplicationApplicantIds, final List<UUID> courtApplicationIds, final List<UUID> courtApplicationRespondentIds, final List<UUID> courtApplicationSubjectIds, final List<UUID> defendantIds, final UUID hearingId, final List<UUID> offenceIds, final String courtApplicationType, final Boolean restrictFromCourtList) {
    this.caseIds = caseIds;
    this.courtApplicationApplicantIds = courtApplicationApplicantIds;
    this.courtApplicationIds = courtApplicationIds;
    this.courtApplicationRespondentIds = courtApplicationRespondentIds;
    this.courtApplicationSubjectIds = courtApplicationSubjectIds;
    this.defendantIds = defendantIds;
    this.hearingId = hearingId;
    this.offenceIds = offenceIds;
    this.courtApplicationType = courtApplicationType;
    this.restrictFromCourtList = restrictFromCourtList;
  }

  public List<UUID> getCaseIds() {
    return caseIds;
  }

  public List<UUID> getDefendantIds() {
    return defendantIds;
  }

  public Boolean getRestrictFromCourtList() {
    return restrictFromCourtList;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

    public Optional<String> getCourtApplicationType() {
        return Optional.ofNullable(courtApplicationType);
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

  public static Builder restrictCourtList() {
    return new RestrictCourtList.Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RestrictCourtList that = (RestrictCourtList) o;

    return new EqualsBuilder()
            .append(caseIds,that.caseIds)
            .append(courtApplicationApplicantIds, that.courtApplicationApplicantIds)
            .append(courtApplicationIds, that.courtApplicationIds)
            .append(courtApplicationRespondentIds, that.courtApplicationRespondentIds)
            .append(courtApplicationSubjectIds, that.courtApplicationSubjectIds)
            .append(defendantIds, that.defendantIds)
            .append(hearingId, that.hearingId)
            .append(offenceIds, that.offenceIds)
            .append(courtApplicationType, that.courtApplicationType)
            .append(restrictFromCourtList,that.restrictFromCourtList)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseIds, courtApplicationApplicantIds, courtApplicationIds, courtApplicationRespondentIds, courtApplicationSubjectIds, defendantIds, hearingId, offenceIds, courtApplicationType,restrictFromCourtList);
}

  @Override
  public String toString() {
    return "RestrictCourtList{" +
            "caseIds='" + caseIds + "'," +
            "courtApplicationApplicantIds='" + courtApplicationApplicantIds + "'," +
            "courtApplicationIds='" + courtApplicationIds + "'," +
            "courtApplicationRespondentIds='" + courtApplicationRespondentIds + "'," +
            "courtApplicationSubjectIds='" + courtApplicationSubjectIds + "'," +
            "defendantIds='" + defendantIds + "'," +
            "hearingId='" + hearingId + "'," +
            "offenceIds='" + offenceIds + "'," +
            "restrictCourtList='" + restrictFromCourtList + "'," +
            "courtApplicationType='" + courtApplicationType + "'" +
            "}";
  }
  public static class Builder {
    private List<UUID> caseIds;

    private List<UUID> courtApplicationApplicantIds;

    private List<UUID> courtApplicatonIds;

    private List<UUID> courtApplicatonRespondentIds;

    private List<UUID> courtApplicationSubjectIds;

    private List<UUID> defendantsId;

    private UUID hearingId;

    private List<UUID> offencesId;

    private Boolean restrictFromCourtList;

    private String courtApplicationType;

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
      this.defendantsId = defendantsId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withOffenceIds(final List<UUID> offencesId) {
      this.offencesId = offencesId;
      return this;
    }

    public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
      this.restrictFromCourtList = restrictFromCourtList;
      return this;
    }

    public Builder withCourtApplicationType(final String courtApplicationType) {
      this.courtApplicationType = courtApplicationType;
      return this;
    }

    public RestrictCourtList build() {
      return new RestrictCourtList(caseIds, courtApplicationApplicantIds, courtApplicatonIds, courtApplicatonRespondentIds, courtApplicationSubjectIds, defendantsId, hearingId, offencesId, courtApplicationType, restrictFromCourtList);
    }
  }

}

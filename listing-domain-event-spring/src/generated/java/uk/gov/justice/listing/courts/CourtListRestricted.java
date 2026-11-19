package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CourtListRestricted implements Serializable {
  private static final long serialVersionUID = 3367818526890492381L;

  private final List<UUID> caseIds;

  private final List<UUID> courtApplicationApplicantIds;

  private final List<UUID> courtApplicationIds;

  private final List<UUID> courtApplicationRespondentIds;

  private final List<UUID> courtApplicationSubjectIds;

  private final String courtApplicationType;

  private final List<UUID> defendantIds;

  private final UUID hearingId;

  private final List<UUID> offenceIds;

  private final Boolean restrictCourtList;

  @JsonCreator
  public CourtListRestricted(final List<UUID> caseIds, final List<UUID> courtApplicationApplicantIds, final List<UUID> courtApplicationIds, final List<UUID> courtApplicationRespondentIds, final List<UUID> courtApplicationSubjectIds, final String courtApplicationType, final List<UUID> defendantIds, final UUID hearingId, final List<UUID> offenceIds, final Boolean restrictCourtList) {
    this.caseIds = caseIds;
    this.courtApplicationApplicantIds = courtApplicationApplicantIds;
    this.courtApplicationIds = courtApplicationIds;
    this.courtApplicationRespondentIds = courtApplicationRespondentIds;
    this.courtApplicationSubjectIds = courtApplicationSubjectIds;
    this.courtApplicationType = courtApplicationType;
    this.defendantIds = defendantIds;
    this.hearingId = hearingId;
    this.offenceIds = offenceIds;
    this.restrictCourtList = restrictCourtList;
  }

  public List<UUID> getCaseIds() {
    return caseIds;
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

  public String getCourtApplicationType() {
    return courtApplicationType;
  }

  public List<UUID> getDefendantIds() {
    return defendantIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

  public Boolean getRestrictCourtList() {
    return restrictCourtList;
  }

  public static Builder courtListRestricted() {
    return new uk.gov.justice.listing.courts.CourtListRestricted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CourtListRestricted that = (uk.gov.justice.listing.courts.CourtListRestricted) obj;

    return java.util.Objects.equals(this.caseIds, that.caseIds) &&
    java.util.Objects.equals(this.courtApplicationApplicantIds, that.courtApplicationApplicantIds) &&
    java.util.Objects.equals(this.courtApplicationIds, that.courtApplicationIds) &&
    java.util.Objects.equals(this.courtApplicationRespondentIds, that.courtApplicationRespondentIds) &&
    java.util.Objects.equals(this.courtApplicationSubjectIds, that.courtApplicationSubjectIds) &&
    java.util.Objects.equals(this.courtApplicationType, that.courtApplicationType) &&
    java.util.Objects.equals(this.defendantIds, that.defendantIds) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds) &&
    java.util.Objects.equals(this.restrictCourtList, that.restrictCourtList);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseIds, courtApplicationApplicantIds, courtApplicationIds, courtApplicationRespondentIds, courtApplicationSubjectIds, courtApplicationType, defendantIds, hearingId, offenceIds, restrictCourtList);}

  public static class Builder {
    private List<UUID> caseIds;

    private List<UUID> courtApplicationApplicantIds;

    private List<UUID> courtApplicationIds;

    private List<UUID> courtApplicationRespondentIds;

    private List<UUID> courtApplicationSubjectIds;

    private String courtApplicationType;

    private List<UUID> defendantIds;

    private UUID hearingId;

    private List<UUID> offenceIds;

    private Boolean restrictCourtList;

    public Builder withCaseIds(final List<UUID> caseIds) {
      this.caseIds = caseIds;
      return this;
    }

    public Builder withCourtApplicationApplicantIds(final List<UUID> courtApplicationApplicantIds) {
      this.courtApplicationApplicantIds = courtApplicationApplicantIds;
      return this;
    }

    public Builder withCourtApplicationIds(final List<UUID> courtApplicationIds) {
      this.courtApplicationIds = courtApplicationIds;
      return this;
    }

    public Builder withCourtApplicationRespondentIds(final List<UUID> courtApplicationRespondentIds) {
      this.courtApplicationRespondentIds = courtApplicationRespondentIds;
      return this;
    }

    public Builder withCourtApplicationSubjectIds(final List<UUID> courtApplicationSubjectIds) {
      this.courtApplicationSubjectIds = courtApplicationSubjectIds;
      return this;
    }

    public Builder withCourtApplicationType(final String courtApplicationType) {
      this.courtApplicationType = courtApplicationType;
      return this;
    }

    public Builder withDefendantIds(final List<UUID> defendantIds) {
      this.defendantIds = defendantIds;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withOffenceIds(final List<UUID> offenceIds) {
      this.offenceIds = offenceIds;
      return this;
    }

    public Builder withRestrictCourtList(final Boolean restrictCourtList) {
      this.restrictCourtList = restrictCourtList;
      return this;
    }

    public Builder withValuesFrom(final CourtListRestricted courtListRestricted) {
      this.caseIds = courtListRestricted.getCaseIds();
      this.courtApplicationApplicantIds = courtListRestricted.getCourtApplicationApplicantIds();
      this.courtApplicationIds = courtListRestricted.getCourtApplicationIds();
      this.courtApplicationRespondentIds = courtListRestricted.getCourtApplicationRespondentIds();
      this.courtApplicationSubjectIds = courtListRestricted.getCourtApplicationSubjectIds();
      this.courtApplicationType = courtListRestricted.getCourtApplicationType();
      this.defendantIds = courtListRestricted.getDefendantIds();
      this.hearingId = courtListRestricted.getHearingId();
      this.offenceIds = courtListRestricted.getOffenceIds();
      this.restrictCourtList = courtListRestricted.getRestrictCourtList();
      return this;
    }

    public CourtListRestricted build() {
      return new uk.gov.justice.listing.courts.CourtListRestricted(caseIds, courtApplicationApplicantIds, courtApplicationIds, courtApplicationRespondentIds, courtApplicationSubjectIds, courtApplicationType, defendantIds, hearingId, offenceIds, restrictCourtList);
    }
  }
}

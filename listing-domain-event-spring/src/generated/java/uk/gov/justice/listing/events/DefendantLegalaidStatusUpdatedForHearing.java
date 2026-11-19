package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class DefendantLegalaidStatusUpdatedForHearing implements Serializable {
  private static final long serialVersionUID = 2177252047543107210L;

  private final UUID caseId;

  private final UUID defendantId;

  private final UUID hearingId;

  private final String legalAidStatus;

  @JsonCreator
  public DefendantLegalaidStatusUpdatedForHearing(final UUID caseId, final UUID defendantId, final UUID hearingId, final String legalAidStatus) {
    this.caseId = caseId;
    this.defendantId = defendantId;
    this.hearingId = hearingId;
    this.legalAidStatus = legalAidStatus;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getLegalAidStatus() {
    return legalAidStatus;
  }

  public static Builder defendantLegalaidStatusUpdatedForHearing() {
    return new uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing that = (uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.legalAidStatus, that.legalAidStatus);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantId, hearingId, legalAidStatus);}

  public static class Builder {
    private UUID caseId;

    private UUID defendantId;

    private UUID hearingId;

    private String legalAidStatus;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withLegalAidStatus(final String legalAidStatus) {
      this.legalAidStatus = legalAidStatus;
      return this;
    }

    public Builder withValuesFrom(final DefendantLegalaidStatusUpdatedForHearing defendantLegalaidStatusUpdatedForHearing) {
      this.caseId = defendantLegalaidStatusUpdatedForHearing.getCaseId();
      this.defendantId = defendantLegalaidStatusUpdatedForHearing.getDefendantId();
      this.hearingId = defendantLegalaidStatusUpdatedForHearing.getHearingId();
      this.legalAidStatus = defendantLegalaidStatusUpdatedForHearing.getLegalAidStatus();
      return this;
    }

    public DefendantLegalaidStatusUpdatedForHearing build() {
      return new uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing(caseId, defendantId, hearingId, legalAidStatus);
    }
  }
}

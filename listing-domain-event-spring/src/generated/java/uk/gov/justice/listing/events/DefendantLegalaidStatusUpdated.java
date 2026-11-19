package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class DefendantLegalaidStatusUpdated implements Serializable {
  private static final long serialVersionUID = -1031127058403936606L;

  private final UUID caseId;

  private final UUID defendantId;

  private final List<UUID> hearingIds;

  private final String legalAidStatus;

  @JsonCreator
  public DefendantLegalaidStatusUpdated(final UUID caseId, final UUID defendantId, final List<UUID> hearingIds, final String legalAidStatus) {
    this.caseId = caseId;
    this.defendantId = defendantId;
    this.hearingIds = hearingIds;
    this.legalAidStatus = legalAidStatus;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public String getLegalAidStatus() {
    return legalAidStatus;
  }

  public static Builder defendantLegalaidStatusUpdated() {
    return new uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated that = (uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.legalAidStatus, that.legalAidStatus);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantId, hearingIds, legalAidStatus);}

  public static class Builder {
    private UUID caseId;

    private UUID defendantId;

    private List<UUID> hearingIds;

    private String legalAidStatus;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
      return this;
    }

    public Builder withLegalAidStatus(final String legalAidStatus) {
      this.legalAidStatus = legalAidStatus;
      return this;
    }

    public Builder withValuesFrom(final DefendantLegalaidStatusUpdated defendantLegalaidStatusUpdated) {
      this.caseId = defendantLegalaidStatusUpdated.getCaseId();
      this.defendantId = defendantLegalaidStatusUpdated.getDefendantId();
      this.hearingIds = defendantLegalaidStatusUpdated.getHearingIds();
      this.legalAidStatus = defendantLegalaidStatusUpdated.getLegalAidStatus();
      return this;
    }

    public DefendantLegalaidStatusUpdated build() {
      return new uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated(caseId, defendantId, hearingIds, legalAidStatus);
    }
  }
}

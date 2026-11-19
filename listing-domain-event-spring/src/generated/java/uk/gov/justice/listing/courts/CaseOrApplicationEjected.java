package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CaseOrApplicationEjected implements Serializable {
  private static final long serialVersionUID = 5755209204982909562L;

  private final UUID applicationId;

  private final List<UUID> hearingIds;

  private final UUID prosecutionCaseId;

  private final String removalReason;

  @JsonCreator
  public CaseOrApplicationEjected(final UUID applicationId, final List<UUID> hearingIds, final UUID prosecutionCaseId, final String removalReason) {
    this.applicationId = applicationId;
    this.hearingIds = hearingIds;
    this.prosecutionCaseId = prosecutionCaseId;
    this.removalReason = removalReason;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public static Builder caseOrApplicationEjected() {
    return new uk.gov.justice.listing.courts.CaseOrApplicationEjected.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CaseOrApplicationEjected that = (uk.gov.justice.listing.courts.CaseOrApplicationEjected) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.removalReason, that.removalReason);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, hearingIds, prosecutionCaseId, removalReason);}

  public static class Builder {
    private UUID applicationId;

    private List<UUID> hearingIds;

    private UUID prosecutionCaseId;

    private String removalReason;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withRemovalReason(final String removalReason) {
      this.removalReason = removalReason;
      return this;
    }

    public Builder withValuesFrom(final CaseOrApplicationEjected caseOrApplicationEjected) {
      this.applicationId = caseOrApplicationEjected.getApplicationId();
      this.hearingIds = caseOrApplicationEjected.getHearingIds();
      this.prosecutionCaseId = caseOrApplicationEjected.getProsecutionCaseId();
      this.removalReason = caseOrApplicationEjected.getRemovalReason();
      return this;
    }

    public CaseOrApplicationEjected build() {
      return new uk.gov.justice.listing.courts.CaseOrApplicationEjected(applicationId, hearingIds, prosecutionCaseId, removalReason);
    }
  }
}

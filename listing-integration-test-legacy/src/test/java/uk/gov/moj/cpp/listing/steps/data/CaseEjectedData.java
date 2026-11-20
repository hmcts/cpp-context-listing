package uk.gov.moj.cpp.listing.steps.data;

import java.util.List;
import java.util.UUID;

public class CaseEjectedData {
  private final List<UUID> hearingIds;

  private final UUID prosecutionCaseId;

  private final String removalReason;

  public CaseEjectedData(final List<UUID> hearingIds, final UUID prosecutionCaseId, final String removalReason) {
    this.hearingIds = hearingIds;
    this.prosecutionCaseId = prosecutionCaseId;
    this.removalReason = removalReason;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder caseEjected() {
    return new CaseEjectedData.Builder();
  }

  public static class Builder {
    private List<UUID> hearingIds;

    private UUID prosecutionCaseId;

    private String removalReason;

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

    public CaseEjectedData build() {
      return new CaseEjectedData(hearingIds, prosecutionCaseId, removalReason);
    }
  }
}

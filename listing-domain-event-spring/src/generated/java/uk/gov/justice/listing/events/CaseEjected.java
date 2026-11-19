package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CaseEjected implements Serializable {
  private static final long serialVersionUID = -4962151690955551129L;

  private final UUID hearingId;

  private final UUID prosecutionCaseId;

  private final String removalReason;

  @JsonCreator
  public CaseEjected(final UUID hearingId, final UUID prosecutionCaseId, final String removalReason) {
    this.hearingId = hearingId;
    this.prosecutionCaseId = prosecutionCaseId;
    this.removalReason = removalReason;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public static Builder caseEjected() {
    return new uk.gov.justice.listing.events.CaseEjected.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseEjected that = (uk.gov.justice.listing.events.CaseEjected) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.removalReason, that.removalReason);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, prosecutionCaseId, removalReason);}

  public static class Builder {
    private UUID hearingId;

    private UUID prosecutionCaseId;

    private String removalReason;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
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

    public Builder withValuesFrom(final CaseEjected caseEjected) {
      this.hearingId = caseEjected.getHearingId();
      this.prosecutionCaseId = caseEjected.getProsecutionCaseId();
      this.removalReason = caseEjected.getRemovalReason();
      return this;
    }

    public CaseEjected build() {
      return new uk.gov.justice.listing.events.CaseEjected(hearingId, prosecutionCaseId, removalReason);
    }
  }
}

package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.case-ejected-for-hearings")
public class CaseEjectedForHearings implements Serializable {
  private static final long serialVersionUID = 3759558673721310400L;

  private final List<UUID> hearingIds;

  private final UUID prosecutionCaseId;

  private final String removalReason;

  @JsonCreator
  public CaseEjectedForHearings(final List<UUID> hearingIds, final UUID prosecutionCaseId, final String removalReason) {
    this.hearingIds = hearingIds;
    this.prosecutionCaseId = prosecutionCaseId;
    this.removalReason = removalReason;
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

  public static Builder caseEjectedForHearings() {
    return new uk.gov.justice.listing.events.CaseEjectedForHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseEjectedForHearings that = (uk.gov.justice.listing.events.CaseEjectedForHearings) obj;

    return java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.removalReason, that.removalReason);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIds, prosecutionCaseId, removalReason);}

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

    public Builder withValuesFrom(final CaseEjectedForHearings caseEjectedForHearings) {
      this.hearingIds = caseEjectedForHearings.getHearingIds();
      this.prosecutionCaseId = caseEjectedForHearings.getProsecutionCaseId();
      this.removalReason = caseEjectedForHearings.getRemovalReason();
      return this;
    }

    public CaseEjectedForHearings build() {
      return new uk.gov.justice.listing.events.CaseEjectedForHearings(hearingIds, prosecutionCaseId, removalReason);
    }
  }
}

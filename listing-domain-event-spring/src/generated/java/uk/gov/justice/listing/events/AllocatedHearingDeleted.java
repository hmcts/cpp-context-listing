package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.allocated-hearing-deleted")
public class AllocatedHearingDeleted implements Serializable {
  private static final long serialVersionUID = -7020921339408851673L;

  private final List<UUID> caseIds;

  private final UUID hearingId;

  @JsonCreator
  public AllocatedHearingDeleted(final List<UUID> caseIds, final UUID hearingId) {
    this.caseIds = caseIds;
    this.hearingId = hearingId;
  }

  public List<UUID> getCaseIds() {
    return caseIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder allocatedHearingDeleted() {
    return new uk.gov.justice.listing.events.AllocatedHearingDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.AllocatedHearingDeleted that = (uk.gov.justice.listing.events.AllocatedHearingDeleted) obj;

    return java.util.Objects.equals(this.caseIds, that.caseIds) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseIds, hearingId);}

  public static class Builder {
    private List<UUID> caseIds;

    private UUID hearingId;

    public Builder withCaseIds(final List<UUID> caseIds) {
      this.caseIds = caseIds;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final AllocatedHearingDeleted allocatedHearingDeleted) {
      this.caseIds = allocatedHearingDeleted.getCaseIds();
      this.hearingId = allocatedHearingDeleted.getHearingId();
      return this;
    }

    public AllocatedHearingDeleted build() {
      return new uk.gov.justice.listing.events.AllocatedHearingDeleted(caseIds, hearingId);
    }
  }
}

package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class AllocatedHearingDeleted implements Serializable {
  private static final long serialVersionUID = -3362892950239613951L;

  private final UUID hearingId;

  @JsonCreator
  public AllocatedHearingDeleted(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder allocatedHearingDeleted() {
    return new uk.gov.justice.listing.courts.AllocatedHearingDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.AllocatedHearingDeleted that = (uk.gov.justice.listing.courts.AllocatedHearingDeleted) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId);}

  public static class Builder {
    private UUID hearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final AllocatedHearingDeleted allocatedHearingDeleted) {
      this.hearingId = allocatedHearingDeleted.getHearingId();
      return this;
    }

    public AllocatedHearingDeleted build() {
      return new uk.gov.justice.listing.courts.AllocatedHearingDeleted(hearingId);
    }
  }
}

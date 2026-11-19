package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class UnallocatedHearingDeleted implements Serializable {
  private static final long serialVersionUID = 3662492487228210888L;

  private final UUID hearingId;

  @JsonCreator
  public UnallocatedHearingDeleted(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder unallocatedHearingDeleted() {
    return new uk.gov.justice.listing.courts.UnallocatedHearingDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UnallocatedHearingDeleted that = (uk.gov.justice.listing.courts.UnallocatedHearingDeleted) obj;

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

    public Builder withValuesFrom(final UnallocatedHearingDeleted unallocatedHearingDeleted) {
      this.hearingId = unallocatedHearingDeleted.getHearingId();
      return this;
    }

    public UnallocatedHearingDeleted build() {
      return new uk.gov.justice.listing.courts.UnallocatedHearingDeleted(hearingId);
    }
  }
}

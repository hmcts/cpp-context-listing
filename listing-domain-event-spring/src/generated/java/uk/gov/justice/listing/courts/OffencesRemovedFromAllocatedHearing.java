package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class OffencesRemovedFromAllocatedHearing implements Serializable {
  private static final long serialVersionUID = 7184846618692950833L;

  private final UUID hearingId;

  private final List<UUID> offenceIds;

  @JsonCreator
  public OffencesRemovedFromAllocatedHearing(final UUID hearingId, final List<UUID> offenceIds) {
    this.hearingId = hearingId;
    this.offenceIds = offenceIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

  public static Builder offencesRemovedFromAllocatedHearing() {
    return new uk.gov.justice.listing.courts.OffencesRemovedFromAllocatedHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.OffencesRemovedFromAllocatedHearing that = (uk.gov.justice.listing.courts.OffencesRemovedFromAllocatedHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, offenceIds);}

  public static class Builder {
    private UUID hearingId;

    private List<UUID> offenceIds;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withOffenceIds(final List<UUID> offenceIds) {
      this.offenceIds = offenceIds;
      return this;
    }

    public Builder withValuesFrom(final OffencesRemovedFromAllocatedHearing offencesRemovedFromAllocatedHearing) {
      this.hearingId = offencesRemovedFromAllocatedHearing.getHearingId();
      this.offenceIds = offencesRemovedFromAllocatedHearing.getOffenceIds();
      return this;
    }

    public OffencesRemovedFromAllocatedHearing build() {
      return new uk.gov.justice.listing.courts.OffencesRemovedFromAllocatedHearing(hearingId, offenceIds);
    }
  }
}

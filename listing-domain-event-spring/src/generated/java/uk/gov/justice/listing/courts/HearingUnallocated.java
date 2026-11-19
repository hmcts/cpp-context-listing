package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class HearingUnallocated implements Serializable {
  private static final long serialVersionUID = -7873844994916259654L;

  private final UUID hearingId;

  private final List<UUID> offenceIds;

  @JsonCreator
  public HearingUnallocated(final UUID hearingId, final List<UUID> offenceIds) {
    this.hearingId = hearingId;
    this.offenceIds = offenceIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

  public static Builder hearingUnallocated() {
    return new uk.gov.justice.listing.courts.HearingUnallocated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.HearingUnallocated that = (uk.gov.justice.listing.courts.HearingUnallocated) obj;

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

    public Builder withValuesFrom(final HearingUnallocated hearingUnallocated) {
      this.hearingId = hearingUnallocated.getHearingId();
      this.offenceIds = hearingUnallocated.getOffenceIds();
      return this;
    }

    public HearingUnallocated build() {
      return new uk.gov.justice.listing.courts.HearingUnallocated(hearingId, offenceIds);
    }
  }
}

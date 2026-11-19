package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class AvailableSlotsForHearingFreed implements Serializable {
  private static final long serialVersionUID = -5657532183586601649L;

  private final UUID hearingId;

  @JsonCreator
  public AvailableSlotsForHearingFreed(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder availableSlotsForHearingFreed() {
    return new uk.gov.justice.listing.events.AvailableSlotsForHearingFreed.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.AvailableSlotsForHearingFreed that = (uk.gov.justice.listing.events.AvailableSlotsForHearingFreed) obj;

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

    public Builder withValuesFrom(final AvailableSlotsForHearingFreed availableSlotsForHearingFreed) {
      this.hearingId = availableSlotsForHearingFreed.getHearingId();
      return this;
    }

    public AvailableSlotsForHearingFreed build() {
      return new uk.gov.justice.listing.events.AvailableSlotsForHearingFreed(hearingId);
    }
  }
}

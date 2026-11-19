package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class RemoveOffencesFromExistingHearingRequested implements Serializable {
  private static final long serialVersionUID = -3276363453661960174L;

  private final UUID hearingId;

  private final UUID seedingHearingId;

  @JsonCreator
  public RemoveOffencesFromExistingHearingRequested(final UUID hearingId, final UUID seedingHearingId) {
    this.hearingId = hearingId;
    this.seedingHearingId = seedingHearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public static Builder removeOffencesFromExistingHearingRequested() {
    return new uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested that = (uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, seedingHearingId);}

  public static class Builder {
    private UUID hearingId;

    private UUID seedingHearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withValuesFrom(final RemoveOffencesFromExistingHearingRequested removeOffencesFromExistingHearingRequested) {
      this.hearingId = removeOffencesFromExistingHearingRequested.getHearingId();
      this.seedingHearingId = removeOffencesFromExistingHearingRequested.getSeedingHearingId();
      return this;
    }

    public RemoveOffencesFromExistingHearingRequested build() {
      return new uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested(hearingId, seedingHearingId);
    }
  }
}

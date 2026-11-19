package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class RemoveOffencesFromExistingHearing implements Serializable {
  private static final long serialVersionUID = -4690633475610499754L;

  private final UUID hearingId;

  private final UUID seedingHearingId;

  @JsonCreator
  public RemoveOffencesFromExistingHearing(final UUID hearingId, final UUID seedingHearingId) {
    this.hearingId = hearingId;
    this.seedingHearingId = seedingHearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public static Builder removeOffencesFromExistingHearing() {
    return new uk.gov.justice.listing.courts.RemoveOffencesFromExistingHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.RemoveOffencesFromExistingHearing that = (uk.gov.justice.listing.courts.RemoveOffencesFromExistingHearing) obj;

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

    public Builder withValuesFrom(final RemoveOffencesFromExistingHearing removeOffencesFromExistingHearing) {
      this.hearingId = removeOffencesFromExistingHearing.getHearingId();
      this.seedingHearingId = removeOffencesFromExistingHearing.getSeedingHearingId();
      return this;
    }

    public RemoveOffencesFromExistingHearing build() {
      return new uk.gov.justice.listing.courts.RemoveOffencesFromExistingHearing(hearingId, seedingHearingId);
    }
  }
}

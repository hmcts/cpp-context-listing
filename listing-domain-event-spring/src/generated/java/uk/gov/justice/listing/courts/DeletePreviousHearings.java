package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.SeedingHearing;

public class DeletePreviousHearings implements Serializable {
  private static final long serialVersionUID = 5216642992639585539L;

  private final UUID hearingId;

  private final SeedingHearing seedingHearing;

  @JsonCreator
  public DeletePreviousHearings(final UUID hearingId, final SeedingHearing seedingHearing) {
    this.hearingId = hearingId;
    this.seedingHearing = seedingHearing;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public static Builder deletePreviousHearings() {
    return new uk.gov.justice.listing.courts.DeletePreviousHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DeletePreviousHearings that = (uk.gov.justice.listing.courts.DeletePreviousHearings) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, seedingHearing);}

  public static class Builder {
    private UUID hearingId;

    private SeedingHearing seedingHearing;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withValuesFrom(final DeletePreviousHearings deletePreviousHearings) {
      this.hearingId = deletePreviousHearings.getHearingId();
      this.seedingHearing = deletePreviousHearings.getSeedingHearing();
      return this;
    }

    public DeletePreviousHearings build() {
      return new uk.gov.justice.listing.courts.DeletePreviousHearings(hearingId, seedingHearing);
    }
  }
}

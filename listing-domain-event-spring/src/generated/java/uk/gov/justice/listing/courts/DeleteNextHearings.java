package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.SeedingHearing;

public class DeleteNextHearings implements Serializable {
  private static final long serialVersionUID = -2345470554692288956L;

  private final SeedingHearing seedingHearing;

  @JsonCreator
  public DeleteNextHearings(final SeedingHearing seedingHearing) {
    this.seedingHearing = seedingHearing;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public static Builder deleteNextHearings() {
    return new uk.gov.justice.listing.courts.DeleteNextHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DeleteNextHearings that = (uk.gov.justice.listing.courts.DeleteNextHearings) obj;

    return java.util.Objects.equals(this.seedingHearing, that.seedingHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(seedingHearing);}

  public static class Builder {
    private SeedingHearing seedingHearing;

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withValuesFrom(final DeleteNextHearings deleteNextHearings) {
      this.seedingHearing = deleteNextHearings.getSeedingHearing();
      return this;
    }

    public DeleteNextHearings build() {
      return new uk.gov.justice.listing.courts.DeleteNextHearings(seedingHearing);
    }
  }
}

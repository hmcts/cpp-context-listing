package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.SeedingHearing;

public class ListUnscheduledNextHearings implements Serializable {
  private static final long serialVersionUID = -4792556061967163981L;

  private final List<HearingUnscheduledListingNeeds> hearings;

  private final SeedingHearing seedingHearing;

  @JsonCreator
  public ListUnscheduledNextHearings(final List<HearingUnscheduledListingNeeds> hearings, final SeedingHearing seedingHearing) {
    this.hearings = hearings;
    this.seedingHearing = seedingHearing;
  }

  public List<HearingUnscheduledListingNeeds> getHearings() {
    return hearings;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public static Builder listUnscheduledNextHearings() {
    return new uk.gov.justice.listing.courts.ListUnscheduledNextHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListUnscheduledNextHearings that = (uk.gov.justice.listing.courts.ListUnscheduledNextHearings) obj;

    return java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearings, seedingHearing);}

  public static class Builder {
    private List<HearingUnscheduledListingNeeds> hearings;

    private SeedingHearing seedingHearing;

    public Builder withHearings(final List<HearingUnscheduledListingNeeds> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withValuesFrom(final ListUnscheduledNextHearings listUnscheduledNextHearings) {
      this.hearings = listUnscheduledNextHearings.getHearings();
      this.seedingHearing = listUnscheduledNextHearings.getSeedingHearing();
      return this;
    }

    public ListUnscheduledNextHearings build() {
      return new uk.gov.justice.listing.courts.ListUnscheduledNextHearings(hearings, seedingHearing);
    }
  }
}

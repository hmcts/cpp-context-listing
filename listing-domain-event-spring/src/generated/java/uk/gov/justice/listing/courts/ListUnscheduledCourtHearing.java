package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;

public class ListUnscheduledCourtHearing implements Serializable {
  private static final long serialVersionUID = -8073545902110665383L;

  private final List<HearingUnscheduledListingNeeds> hearings;

  @JsonCreator
  public ListUnscheduledCourtHearing(final List<HearingUnscheduledListingNeeds> hearings) {
    this.hearings = hearings;
  }

  public List<HearingUnscheduledListingNeeds> getHearings() {
    return hearings;
  }

  public static Builder listUnscheduledCourtHearing() {
    return new uk.gov.justice.listing.courts.ListUnscheduledCourtHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListUnscheduledCourtHearing that = (uk.gov.justice.listing.courts.ListUnscheduledCourtHearing) obj;

    return java.util.Objects.equals(this.hearings, that.hearings);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearings);}

  public static class Builder {
    private List<HearingUnscheduledListingNeeds> hearings;

    public Builder withHearings(final List<HearingUnscheduledListingNeeds> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withValuesFrom(final ListUnscheduledCourtHearing listUnscheduledCourtHearing) {
      this.hearings = listUnscheduledCourtHearing.getHearings();
      return this;
    }

    public ListUnscheduledCourtHearing build() {
      return new uk.gov.justice.listing.courts.ListUnscheduledCourtHearing(hearings);
    }
  }
}

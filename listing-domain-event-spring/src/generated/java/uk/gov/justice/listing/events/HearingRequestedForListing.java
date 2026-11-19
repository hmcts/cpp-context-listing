package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.CourtHearingRequest;

public class HearingRequestedForListing implements Serializable {
  private static final long serialVersionUID = 4799816782246862590L;

  private final CourtHearingRequest listNewHearing;

  @JsonCreator
  public HearingRequestedForListing(final CourtHearingRequest listNewHearing) {
    this.listNewHearing = listNewHearing;
  }

  public CourtHearingRequest getListNewHearing() {
    return listNewHearing;
  }

  public static Builder hearingRequestedForListing() {
    return new uk.gov.justice.listing.events.HearingRequestedForListing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingRequestedForListing that = (uk.gov.justice.listing.events.HearingRequestedForListing) obj;

    return java.util.Objects.equals(this.listNewHearing, that.listNewHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(listNewHearing);}

  public static class Builder {
    private CourtHearingRequest listNewHearing;

    public Builder withListNewHearing(final CourtHearingRequest listNewHearing) {
      this.listNewHearing = listNewHearing;
      return this;
    }

    public Builder withValuesFrom(final HearingRequestedForListing hearingRequestedForListing) {
      this.listNewHearing = hearingRequestedForListing.getListNewHearing();
      return this;
    }

    public HearingRequestedForListing build() {
      return new uk.gov.justice.listing.events.HearingRequestedForListing(listNewHearing);
    }
  }
}

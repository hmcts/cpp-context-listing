package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.listing.commands.CourtCentreDetails;

public class ListUnscheduledCourtHearingEnriched implements Serializable {
  private static final long serialVersionUID = 6716282330939640861L;

  private final List<CourtCentreDetails> courtCentresDetails;

  private final List<HearingUnscheduledListingNeeds> hearings;

  @JsonCreator
  public ListUnscheduledCourtHearingEnriched(final List<CourtCentreDetails> courtCentresDetails, final List<HearingUnscheduledListingNeeds> hearings) {
    this.courtCentresDetails = courtCentresDetails;
    this.hearings = hearings;
  }

  public List<CourtCentreDetails> getCourtCentresDetails() {
    return courtCentresDetails;
  }

  public List<HearingUnscheduledListingNeeds> getHearings() {
    return hearings;
  }

  public static Builder listUnscheduledCourtHearingEnriched() {
    return new uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched that = (uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched) obj;

    return java.util.Objects.equals(this.courtCentresDetails, that.courtCentresDetails) &&
    java.util.Objects.equals(this.hearings, that.hearings);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentresDetails, hearings);}

  public static class Builder {
    private List<CourtCentreDetails> courtCentresDetails;

    private List<HearingUnscheduledListingNeeds> hearings;

    public Builder withCourtCentresDetails(final List<CourtCentreDetails> courtCentresDetails) {
      this.courtCentresDetails = courtCentresDetails;
      return this;
    }

    public Builder withHearings(final List<HearingUnscheduledListingNeeds> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withValuesFrom(final ListUnscheduledCourtHearingEnriched listUnscheduledCourtHearingEnriched) {
      this.courtCentresDetails = listUnscheduledCourtHearingEnriched.getCourtCentresDetails();
      this.hearings = listUnscheduledCourtHearingEnriched.getHearings();
      return this;
    }

    public ListUnscheduledCourtHearingEnriched build() {
      return new uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched(courtCentresDetails, hearings);
    }
  }
}

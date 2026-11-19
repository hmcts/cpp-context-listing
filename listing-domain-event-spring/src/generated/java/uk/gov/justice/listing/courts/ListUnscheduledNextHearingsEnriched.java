package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingPtphDetail;

public class ListUnscheduledNextHearingsEnriched implements Serializable {
  private static final long serialVersionUID = 7247812267194165235L;

  private final List<CourtCentreDetails> courtCentresDetails;

  private final List<HearingUnscheduledListingNeeds> hearings;

  private final List<HearingPtphDetail> ptphDetails;

  private final SeedingHearing seedingHearing;

  @JsonCreator
  public ListUnscheduledNextHearingsEnriched(final List<CourtCentreDetails> courtCentresDetails, final List<HearingUnscheduledListingNeeds> hearings, final List<HearingPtphDetail> ptphDetails, final SeedingHearing seedingHearing) {
    this.courtCentresDetails = courtCentresDetails;
    this.hearings = hearings;
    this.ptphDetails = ptphDetails;
    this.seedingHearing = seedingHearing;
  }

  public List<CourtCentreDetails> getCourtCentresDetails() {
    return courtCentresDetails;
  }

  public List<HearingUnscheduledListingNeeds> getHearings() {
    return hearings;
  }

  public List<HearingPtphDetail> getPtphDetails() {
    return ptphDetails;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public static Builder listUnscheduledNextHearingsEnriched() {
    return new uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched that = (uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched) obj;

    return java.util.Objects.equals(this.courtCentresDetails, that.courtCentresDetails) &&
    java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.ptphDetails, that.ptphDetails) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentresDetails, hearings, ptphDetails, seedingHearing);}

  public static class Builder {
    private List<CourtCentreDetails> courtCentresDetails;

    private List<HearingUnscheduledListingNeeds> hearings;

    private List<HearingPtphDetail> ptphDetails;

    private SeedingHearing seedingHearing;

    public Builder withCourtCentresDetails(final List<CourtCentreDetails> courtCentresDetails) {
      this.courtCentresDetails = courtCentresDetails;
      return this;
    }

    public Builder withHearings(final List<HearingUnscheduledListingNeeds> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withPtphDetails(final List<HearingPtphDetail> ptphDetails) {
      this.ptphDetails = ptphDetails;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withValuesFrom(final ListUnscheduledNextHearingsEnriched listUnscheduledNextHearingsEnriched) {
      this.courtCentresDetails = listUnscheduledNextHearingsEnriched.getCourtCentresDetails();
      this.hearings = listUnscheduledNextHearingsEnriched.getHearings();
      this.ptphDetails = listUnscheduledNextHearingsEnriched.getPtphDetails();
      this.seedingHearing = listUnscheduledNextHearingsEnriched.getSeedingHearing();
      return this;
    }

    public ListUnscheduledNextHearingsEnriched build() {
      return new uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched(courtCentresDetails, hearings, ptphDetails, seedingHearing);
    }
  }
}

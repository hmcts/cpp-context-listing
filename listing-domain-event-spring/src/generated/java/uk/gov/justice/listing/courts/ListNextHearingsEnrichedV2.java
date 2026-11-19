package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;

public class ListNextHearingsEnrichedV2 implements Serializable {
  private static final long serialVersionUID = -760103345012707586L;

  private final String adjournedFromDate;

  private final List<CourtCentreDetails> courtCentresDetails;

  private final ListNextHearingsV2 listNextHearings;

  private final SeedingHearing seedingHearing;

  @JsonCreator
  public ListNextHearingsEnrichedV2(final String adjournedFromDate, final List<CourtCentreDetails> courtCentresDetails, final ListNextHearingsV2 listNextHearings, final SeedingHearing seedingHearing) {
    this.adjournedFromDate = adjournedFromDate;
    this.courtCentresDetails = courtCentresDetails;
    this.listNextHearings = listNextHearings;
    this.seedingHearing = seedingHearing;
  }

  public String getAdjournedFromDate() {
    return adjournedFromDate;
  }

  public List<CourtCentreDetails> getCourtCentresDetails() {
    return courtCentresDetails;
  }

  public ListNextHearingsV2 getListNextHearings() {
    return listNextHearings;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public static Builder listNextHearingsEnrichedV2() {
    return new uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2 that = (uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2) obj;

    return java.util.Objects.equals(this.adjournedFromDate, that.adjournedFromDate) &&
    java.util.Objects.equals(this.courtCentresDetails, that.courtCentresDetails) &&
    java.util.Objects.equals(this.listNextHearings, that.listNextHearings) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournedFromDate, courtCentresDetails, listNextHearings, seedingHearing);}

  public static class Builder {
    private String adjournedFromDate;

    private List<CourtCentreDetails> courtCentresDetails;

    private ListNextHearingsV2 listNextHearings;

    private SeedingHearing seedingHearing;

    public Builder withAdjournedFromDate(final String adjournedFromDate) {
      this.adjournedFromDate = adjournedFromDate;
      return this;
    }

    public Builder withCourtCentresDetails(final List<CourtCentreDetails> courtCentresDetails) {
      this.courtCentresDetails = courtCentresDetails;
      return this;
    }

    public Builder withListNextHearings(final ListNextHearingsV2 listNextHearings) {
      this.listNextHearings = listNextHearings;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withValuesFrom(final ListNextHearingsEnrichedV2 listNextHearingsEnrichedV2) {
      this.adjournedFromDate = listNextHearingsEnrichedV2.getAdjournedFromDate();
      this.courtCentresDetails = listNextHearingsEnrichedV2.getCourtCentresDetails();
      this.listNextHearings = listNextHearingsEnrichedV2.getListNextHearings();
      this.seedingHearing = listNextHearingsEnrichedV2.getSeedingHearing();
      return this;
    }

    public ListNextHearingsEnrichedV2 build() {
      return new uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2(adjournedFromDate, courtCentresDetails, listNextHearings, seedingHearing);
    }
  }
}

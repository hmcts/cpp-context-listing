package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingListingNeeds;

public class ListNextHearing implements Serializable {
  private static final long serialVersionUID = -7702142501243691997L;

  private final String adjournedFromDate;

  private final List<CourtCentreDetails> courtCentresDetails;

  private final HearingListingNeeds hearing;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public ListNextHearing(final String adjournedFromDate, final List<CourtCentreDetails> courtCentresDetails, final HearingListingNeeds hearing, final List<UUID> shadowListedOffences) {
    this.adjournedFromDate = adjournedFromDate;
    this.courtCentresDetails = courtCentresDetails;
    this.hearing = hearing;
    this.shadowListedOffences = shadowListedOffences;
  }

  public String getAdjournedFromDate() {
    return adjournedFromDate;
  }

  public List<CourtCentreDetails> getCourtCentresDetails() {
    return courtCentresDetails;
  }

  public HearingListingNeeds getHearing() {
    return hearing;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder listNextHearing() {
    return new uk.gov.justice.listing.courts.ListNextHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListNextHearing that = (uk.gov.justice.listing.courts.ListNextHearing) obj;

    return java.util.Objects.equals(this.adjournedFromDate, that.adjournedFromDate) &&
    java.util.Objects.equals(this.courtCentresDetails, that.courtCentresDetails) &&
    java.util.Objects.equals(this.hearing, that.hearing) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournedFromDate, courtCentresDetails, hearing, shadowListedOffences);}

  public static class Builder {
    private String adjournedFromDate;

    private List<CourtCentreDetails> courtCentresDetails;

    private HearingListingNeeds hearing;

    private List<UUID> shadowListedOffences;

    public Builder withAdjournedFromDate(final String adjournedFromDate) {
      this.adjournedFromDate = adjournedFromDate;
      return this;
    }

    public Builder withCourtCentresDetails(final List<CourtCentreDetails> courtCentresDetails) {
      this.courtCentresDetails = courtCentresDetails;
      return this;
    }

    public Builder withHearing(final HearingListingNeeds hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withValuesFrom(final ListNextHearing listNextHearing) {
      this.adjournedFromDate = listNextHearing.getAdjournedFromDate();
      this.courtCentresDetails = listNextHearing.getCourtCentresDetails();
      this.hearing = listNextHearing.getHearing();
      this.shadowListedOffences = listNextHearing.getShadowListedOffences();
      return this;
    }

    public ListNextHearing build() {
      return new uk.gov.justice.listing.courts.ListNextHearing(adjournedFromDate, courtCentresDetails, hearing, shadowListedOffences);
    }
  }
}

package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.commands.HearingListingNeeds;

public class NextHearingRequested implements Serializable {
  private static final long serialVersionUID = 5338859218929785743L;

  private final String adjournedFromDate;

  private final List<CourtCentreDetails> courtCentreDetails;

  private final HearingListingNeeds hearing;

  private final String hearingDay;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public NextHearingRequested(final String adjournedFromDate, final List<CourtCentreDetails> courtCentreDetails, final HearingListingNeeds hearing, final String hearingDay, final List<UUID> shadowListedOffences) {
    this.adjournedFromDate = adjournedFromDate;
    this.courtCentreDetails = courtCentreDetails;
    this.hearing = hearing;
    this.hearingDay = hearingDay;
    this.shadowListedOffences = shadowListedOffences;
  }

  public String getAdjournedFromDate() {
    return adjournedFromDate;
  }

  public List<CourtCentreDetails> getCourtCentreDetails() {
    return courtCentreDetails;
  }

  public HearingListingNeeds getHearing() {
    return hearing;
  }

  public String getHearingDay() {
    return hearingDay;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder nextHearingRequested() {
    return new uk.gov.justice.listing.events.NextHearingRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NextHearingRequested that = (uk.gov.justice.listing.events.NextHearingRequested) obj;

    return java.util.Objects.equals(this.adjournedFromDate, that.adjournedFromDate) &&
    java.util.Objects.equals(this.courtCentreDetails, that.courtCentreDetails) &&
    java.util.Objects.equals(this.hearing, that.hearing) &&
    java.util.Objects.equals(this.hearingDay, that.hearingDay) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournedFromDate, courtCentreDetails, hearing, hearingDay, shadowListedOffences);}

  public static class Builder {
    private String adjournedFromDate;

    private List<CourtCentreDetails> courtCentreDetails;

    private HearingListingNeeds hearing;

    private String hearingDay;

    private List<UUID> shadowListedOffences;

    public Builder withAdjournedFromDate(final String adjournedFromDate) {
      this.adjournedFromDate = adjournedFromDate;
      return this;
    }

    public Builder withCourtCentreDetails(final List<CourtCentreDetails> courtCentreDetails) {
      this.courtCentreDetails = courtCentreDetails;
      return this;
    }

    public Builder withHearing(final HearingListingNeeds hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withHearingDay(final String hearingDay) {
      this.hearingDay = hearingDay;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withValuesFrom(final NextHearingRequested nextHearingRequested) {
      this.adjournedFromDate = nextHearingRequested.getAdjournedFromDate();
      this.courtCentreDetails = nextHearingRequested.getCourtCentreDetails();
      this.hearing = nextHearingRequested.getHearing();
      this.hearingDay = nextHearingRequested.getHearingDay();
      this.shadowListedOffences = nextHearingRequested.getShadowListedOffences();
      return this;
    }

    public NextHearingRequested build() {
      return new uk.gov.justice.listing.events.NextHearingRequested(adjournedFromDate, courtCentreDetails, hearing, hearingDay, shadowListedOffences);
    }
  }
}

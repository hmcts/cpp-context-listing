package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class ListCourtHearing implements Serializable {
  private static final long serialVersionUID = 7316061296234048358L;

  private final String adjournedFromDate;

  private final List<HearingListingNeeds> hearings;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public ListCourtHearing(final String adjournedFromDate, final List<HearingListingNeeds> hearings, final List<UUID> shadowListedOffences) {
    this.adjournedFromDate = adjournedFromDate;
    this.hearings = hearings;
    this.shadowListedOffences = shadowListedOffences;
  }

  public String getAdjournedFromDate() {
    return adjournedFromDate;
  }

  public List<HearingListingNeeds> getHearings() {
    return hearings;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder listCourtHearing() {
    return new uk.gov.justice.listing.commands.ListCourtHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.ListCourtHearing that = (uk.gov.justice.listing.commands.ListCourtHearing) obj;

    return java.util.Objects.equals(this.adjournedFromDate, that.adjournedFromDate) &&
    java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournedFromDate, hearings, shadowListedOffences);}

  public static class Builder {
    private String adjournedFromDate;

    private List<HearingListingNeeds> hearings;

    private List<UUID> shadowListedOffences;

    public Builder withAdjournedFromDate(final String adjournedFromDate) {
      this.adjournedFromDate = adjournedFromDate;
      return this;
    }

    public Builder withHearings(final List<HearingListingNeeds> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withValuesFrom(final ListCourtHearing listCourtHearing) {
      this.adjournedFromDate = listCourtHearing.getAdjournedFromDate();
      this.hearings = listCourtHearing.getHearings();
      this.shadowListedOffences = listCourtHearing.getShadowListedOffences();
      return this;
    }

    public ListCourtHearing build() {
      return new uk.gov.justice.listing.commands.ListCourtHearing(adjournedFromDate, hearings, shadowListedOffences);
    }
  }
}

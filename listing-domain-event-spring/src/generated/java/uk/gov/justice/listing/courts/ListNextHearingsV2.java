package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.events.BookingReferenceCourtScheduleIds;

public class ListNextHearingsV2 implements Serializable {
  private static final long serialVersionUID = -4790259863836512281L;

  private final String adjournedFromDate;

  private final List<BookingReferenceCourtScheduleIds> bookingReferencesWithCourtScheduleIds;

  private final List<HearingListingNeeds> hearings;

  private final SeedingHearing seedingHearing;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public ListNextHearingsV2(final String adjournedFromDate, final List<BookingReferenceCourtScheduleIds> bookingReferencesWithCourtScheduleIds, final List<HearingListingNeeds> hearings, final SeedingHearing seedingHearing, final List<UUID> shadowListedOffences) {
    this.adjournedFromDate = adjournedFromDate;
    this.bookingReferencesWithCourtScheduleIds = bookingReferencesWithCourtScheduleIds;
    this.hearings = hearings;
    this.seedingHearing = seedingHearing;
    this.shadowListedOffences = shadowListedOffences;
  }

  public String getAdjournedFromDate() {
    return adjournedFromDate;
  }

  public List<BookingReferenceCourtScheduleIds> getBookingReferencesWithCourtScheduleIds() {
    return bookingReferencesWithCourtScheduleIds;
  }

  public List<HearingListingNeeds> getHearings() {
    return hearings;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder listNextHearingsV2() {
    return new uk.gov.justice.listing.courts.ListNextHearingsV2.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ListNextHearingsV2 that = (uk.gov.justice.listing.courts.ListNextHearingsV2) obj;

    return java.util.Objects.equals(this.adjournedFromDate, that.adjournedFromDate) &&
    java.util.Objects.equals(this.bookingReferencesWithCourtScheduleIds, that.bookingReferencesWithCourtScheduleIds) &&
    java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournedFromDate, bookingReferencesWithCourtScheduleIds, hearings, seedingHearing, shadowListedOffences);}

  public static class Builder {
    private String adjournedFromDate;

    private List<BookingReferenceCourtScheduleIds> bookingReferencesWithCourtScheduleIds;

    private List<HearingListingNeeds> hearings;

    private SeedingHearing seedingHearing;

    private List<UUID> shadowListedOffences;

    public Builder withAdjournedFromDate(final String adjournedFromDate) {
      this.adjournedFromDate = adjournedFromDate;
      return this;
    }

    public Builder withBookingReferencesWithCourtScheduleIds(final List<BookingReferenceCourtScheduleIds> bookingReferencesWithCourtScheduleIds) {
      this.bookingReferencesWithCourtScheduleIds = bookingReferencesWithCourtScheduleIds;
      return this;
    }

    public Builder withHearings(final List<HearingListingNeeds> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withValuesFrom(final ListNextHearingsV2 listNextHearingsV2) {
      this.adjournedFromDate = listNextHearingsV2.getAdjournedFromDate();
      this.bookingReferencesWithCourtScheduleIds = listNextHearingsV2.getBookingReferencesWithCourtScheduleIds();
      this.hearings = listNextHearingsV2.getHearings();
      this.seedingHearing = listNextHearingsV2.getSeedingHearing();
      this.shadowListedOffences = listNextHearingsV2.getShadowListedOffences();
      return this;
    }

    public ListNextHearingsV2 build() {
      return new uk.gov.justice.listing.courts.ListNextHearingsV2(adjournedFromDate, bookingReferencesWithCourtScheduleIds, hearings, seedingHearing, shadowListedOffences);
    }
  }
}

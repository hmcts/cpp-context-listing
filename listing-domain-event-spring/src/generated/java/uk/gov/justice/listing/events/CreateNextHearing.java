package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.SeedingHearing;

public class CreateNextHearing implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final CommittingCourt committingCourt;

  private final Hearing hearing;

  private final List<BookingReferenceCourtScheduleIds> previousBookingReferencesWithCourtScheduleIds;

  private final SeedingHearing seedingHearing;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public CreateNextHearing(final CommittingCourt committingCourt, final Hearing hearing, final List<BookingReferenceCourtScheduleIds> previousBookingReferencesWithCourtScheduleIds, final SeedingHearing seedingHearing, final List<UUID> shadowListedOffences) {
    this.committingCourt = committingCourt;
    this.hearing = hearing;
    this.previousBookingReferencesWithCourtScheduleIds = previousBookingReferencesWithCourtScheduleIds;
    this.seedingHearing = seedingHearing;
    this.shadowListedOffences = shadowListedOffences;
  }

  public CommittingCourt getCommittingCourt() {
    return committingCourt;
  }

  public Hearing getHearing() {
    return hearing;
  }

  public List<BookingReferenceCourtScheduleIds> getPreviousBookingReferencesWithCourtScheduleIds() {
    return previousBookingReferencesWithCourtScheduleIds;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder createNextHearing() {
    return new uk.gov.justice.listing.events.CreateNextHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CreateNextHearing that = (uk.gov.justice.listing.events.CreateNextHearing) obj;

    return java.util.Objects.equals(this.committingCourt, that.committingCourt) &&
    java.util.Objects.equals(this.hearing, that.hearing) &&
    java.util.Objects.equals(this.previousBookingReferencesWithCourtScheduleIds, that.previousBookingReferencesWithCourtScheduleIds) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(committingCourt, hearing, previousBookingReferencesWithCourtScheduleIds, seedingHearing, shadowListedOffences);}

  public static class Builder {
    private CommittingCourt committingCourt;

    private Hearing hearing;

    private List<BookingReferenceCourtScheduleIds> previousBookingReferencesWithCourtScheduleIds;

    private SeedingHearing seedingHearing;

    private List<UUID> shadowListedOffences;

    public Builder withCommittingCourt(final CommittingCourt committingCourt) {
      this.committingCourt = committingCourt;
      return this;
    }

    public Builder withHearing(final Hearing hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withPreviousBookingReferencesWithCourtScheduleIds(final List<BookingReferenceCourtScheduleIds> previousBookingReferencesWithCourtScheduleIds) {
      this.previousBookingReferencesWithCourtScheduleIds = previousBookingReferencesWithCourtScheduleIds;
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

    public Builder withValuesFrom(final CreateNextHearing createNextHearing) {
      this.committingCourt = createNextHearing.getCommittingCourt();
      this.hearing = createNextHearing.getHearing();
      this.previousBookingReferencesWithCourtScheduleIds = createNextHearing.getPreviousBookingReferencesWithCourtScheduleIds();
      this.seedingHearing = createNextHearing.getSeedingHearing();
      this.shadowListedOffences = createNextHearing.getShadowListedOffences();
      return this;
    }

    public CreateNextHearing build() {
      return new uk.gov.justice.listing.events.CreateNextHearing(committingCourt, hearing, previousBookingReferencesWithCourtScheduleIds, seedingHearing, shadowListedOffences);
    }
  }
}

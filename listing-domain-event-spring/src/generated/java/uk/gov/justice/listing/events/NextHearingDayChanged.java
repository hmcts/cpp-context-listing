package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.next-hearing-day-changed")
public class NextHearingDayChanged implements Serializable {
  private static final long serialVersionUID = 7212737245309610257L;

  private final UUID hearingId;

  private final ZonedDateTime hearingStartDate;

  private final UUID seedingHearingId;

  @JsonCreator
  public NextHearingDayChanged(final UUID hearingId, final ZonedDateTime hearingStartDate, final UUID seedingHearingId) {
    this.hearingId = hearingId;
    this.hearingStartDate = hearingStartDate;
    this.seedingHearingId = seedingHearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public ZonedDateTime getHearingStartDate() {
    return hearingStartDate;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public static Builder nextHearingDayChanged() {
    return new uk.gov.justice.listing.events.NextHearingDayChanged.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NextHearingDayChanged that = (uk.gov.justice.listing.events.NextHearingDayChanged) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.hearingStartDate, that.hearingStartDate) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, hearingStartDate, seedingHearingId);}

  public static class Builder {
    private UUID hearingId;

    private ZonedDateTime hearingStartDate;

    private UUID seedingHearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withHearingStartDate(final ZonedDateTime hearingStartDate) {
      this.hearingStartDate = hearingStartDate;
      return this;
    }

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withValuesFrom(final NextHearingDayChanged nextHearingDayChanged) {
      this.hearingId = nextHearingDayChanged.getHearingId();
      this.hearingStartDate = nextHearingDayChanged.getHearingStartDate();
      this.seedingHearingId = nextHearingDayChanged.getSeedingHearingId();
      return this;
    }

    public NextHearingDayChanged build() {
      return new uk.gov.justice.listing.events.NextHearingDayChanged(hearingId, hearingStartDate, seedingHearingId);
    }
  }
}

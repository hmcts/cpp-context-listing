package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.delete-next-hearing-requested")
public class DeleteNextHearingRequested implements Serializable {
  private static final long serialVersionUID = 1009950526982039722L;

  private final String hearingDay;

  private final UUID hearingId;

  private final UUID seedingHearingId;

  @JsonCreator
  public DeleteNextHearingRequested(final String hearingDay, final UUID hearingId, final UUID seedingHearingId) {
    this.hearingDay = hearingDay;
    this.hearingId = hearingId;
    this.seedingHearingId = seedingHearingId;
  }

  public String getHearingDay() {
    return hearingDay;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public static Builder deleteNextHearingRequested() {
    return new uk.gov.justice.listing.events.DeleteNextHearingRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DeleteNextHearingRequested that = (uk.gov.justice.listing.events.DeleteNextHearingRequested) obj;

    return java.util.Objects.equals(this.hearingDay, that.hearingDay) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingDay, hearingId, seedingHearingId);}

  public static class Builder {
    private String hearingDay;

    private UUID hearingId;

    private UUID seedingHearingId;

    public Builder withHearingDay(final String hearingDay) {
      this.hearingDay = hearingDay;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withValuesFrom(final DeleteNextHearingRequested deleteNextHearingRequested) {
      this.hearingDay = deleteNextHearingRequested.getHearingDay();
      this.hearingId = deleteNextHearingRequested.getHearingId();
      this.seedingHearingId = deleteNextHearingRequested.getSeedingHearingId();
      return this;
    }

    public DeleteNextHearingRequested build() {
      return new uk.gov.justice.listing.events.DeleteNextHearingRequested(hearingDay, hearingId, seedingHearingId);
    }
  }
}

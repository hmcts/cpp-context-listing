package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class HearingDaysCancelled implements Serializable {
  private static final long serialVersionUID = 5270179082673629228L;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  @JsonCreator
  public HearingDaysCancelled(final List<HearingDay> hearingDays, final UUID hearingId) {
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingDaysCancelled() {
    return new uk.gov.justice.listing.events.HearingDaysCancelled.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingDaysCancelled that = (uk.gov.justice.listing.events.HearingDaysCancelled) obj;

    return java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingDays, hearingId);}

  public static class Builder {
    private List<HearingDay> hearingDays;

    private UUID hearingId;

    public Builder withHearingDays(final List<HearingDay> hearingDays) {
      this.hearingDays = hearingDays;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final HearingDaysCancelled hearingDaysCancelled) {
      this.hearingDays = hearingDaysCancelled.getHearingDays();
      this.hearingId = hearingDaysCancelled.getHearingId();
      return this;
    }

    public HearingDaysCancelled build() {
      return new uk.gov.justice.listing.events.HearingDaysCancelled(hearingDays, hearingId);
    }
  }
}

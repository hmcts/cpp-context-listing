package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-day-court-schedule-updated")
public class HearingDayCourtScheduleUpdated implements Serializable {
  private static final long serialVersionUID = -5648254759709368917L;

  private final List<HearingDayCourtSchedule> hearingDayCourtSchedules;

  private final UUID hearingId;

  @JsonCreator
  public HearingDayCourtScheduleUpdated(final List<HearingDayCourtSchedule> hearingDayCourtSchedules, final UUID hearingId) {
    this.hearingDayCourtSchedules = hearingDayCourtSchedules;
    this.hearingId = hearingId;
  }

  public List<HearingDayCourtSchedule> getHearingDayCourtSchedules() {
    return hearingDayCourtSchedules;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingDayCourtScheduleUpdated() {
    return new uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated that = (uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated) obj;

    return java.util.Objects.equals(this.hearingDayCourtSchedules, that.hearingDayCourtSchedules) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingDayCourtSchedules, hearingId);}

  public static class Builder {
    private List<HearingDayCourtSchedule> hearingDayCourtSchedules;

    private UUID hearingId;

    public Builder withHearingDayCourtSchedules(final List<HearingDayCourtSchedule> hearingDayCourtSchedules) {
      this.hearingDayCourtSchedules = hearingDayCourtSchedules;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final HearingDayCourtScheduleUpdated hearingDayCourtScheduleUpdated) {
      this.hearingDayCourtSchedules = hearingDayCourtScheduleUpdated.getHearingDayCourtSchedules();
      this.hearingId = hearingDayCourtScheduleUpdated.getHearingId();
      return this;
    }

    public HearingDayCourtScheduleUpdated build() {
      return new uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated(hearingDayCourtSchedules, hearingId);
    }
  }
}

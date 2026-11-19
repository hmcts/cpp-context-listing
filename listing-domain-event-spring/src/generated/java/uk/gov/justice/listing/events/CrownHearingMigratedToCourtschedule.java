package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.crown-hearing-migrated-to-courtschedule")
public class CrownHearingMigratedToCourtschedule implements Serializable {
  private static final long serialVersionUID = 1205007454664578991L;

  private final List<HearingDayCourtSchedule> hearingDayCourtSchedules;

  private final UUID hearingId;

  @JsonCreator
  public CrownHearingMigratedToCourtschedule(final List<HearingDayCourtSchedule> hearingDayCourtSchedules, final UUID hearingId) {
    this.hearingDayCourtSchedules = hearingDayCourtSchedules;
    this.hearingId = hearingId;
  }

  public List<HearingDayCourtSchedule> getHearingDayCourtSchedules() {
    return hearingDayCourtSchedules;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder crownHearingMigratedToCourtschedule() {
    return new uk.gov.justice.listing.events.CrownHearingMigratedToCourtschedule.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CrownHearingMigratedToCourtschedule that = (uk.gov.justice.listing.events.CrownHearingMigratedToCourtschedule) obj;

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

    public Builder withValuesFrom(final CrownHearingMigratedToCourtschedule crownHearingMigratedToCourtschedule) {
      this.hearingDayCourtSchedules = crownHearingMigratedToCourtschedule.getHearingDayCourtSchedules();
      this.hearingId = crownHearingMigratedToCourtschedule.getHearingId();
      return this;
    }

    public CrownHearingMigratedToCourtschedule build() {
      return new uk.gov.justice.listing.events.CrownHearingMigratedToCourtschedule(hearingDayCourtSchedules, hearingId);
    }
  }
}

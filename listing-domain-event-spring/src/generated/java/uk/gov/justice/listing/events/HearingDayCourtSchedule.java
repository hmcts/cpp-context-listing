package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;
import java.util.UUID;

public class HearingDayCourtSchedule implements Serializable {
  private static final long serialVersionUID = -5648254759709368917L;

  private final UUID courtScheduleId;

  private final LocalDate hearingDate;

  @JsonCreator
  public HearingDayCourtSchedule(final UUID courtScheduleId, final LocalDate hearingDate) {
    this.courtScheduleId = courtScheduleId;
    this.hearingDate = hearingDate;
  }

  public UUID getCourtScheduleId() {
    return courtScheduleId;
  }

  public LocalDate getHearingDate() {
    return hearingDate;
  }

  public static Builder hearingDayCourtSchedule() {
    return new uk.gov.justice.listing.events.HearingDayCourtSchedule.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingDayCourtSchedule that = (uk.gov.justice.listing.events.HearingDayCourtSchedule) obj;

    return java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.hearingDate, that.hearingDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtScheduleId, hearingDate);}

  public static class Builder {
    private UUID courtScheduleId;

    private LocalDate hearingDate;

    public Builder withCourtScheduleId(final UUID courtScheduleId) {
      this.courtScheduleId = courtScheduleId;
      return this;
    }

    public Builder withHearingDate(final LocalDate hearingDate) {
      this.hearingDate = hearingDate;
      return this;
    }

    public Builder withValuesFrom(final HearingDayCourtSchedule hearingDayCourtSchedule) {
      this.courtScheduleId = hearingDayCourtSchedule.getCourtScheduleId();
      this.hearingDate = hearingDayCourtSchedule.getHearingDate();
      return this;
    }

    public HearingDayCourtSchedule build() {
      return new uk.gov.justice.listing.events.HearingDayCourtSchedule(courtScheduleId, hearingDate);
    }
  }
}

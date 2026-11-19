package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.events.HearingDayCourtSchedule;

public class UpdateHearingDayCourtSchedule implements Serializable {
  private static final long serialVersionUID = -7837651215225143599L;

  private final List<HearingDayCourtSchedule> hearingDayCourtSchedules;

  private final UUID hearingId;

  @JsonCreator
  public UpdateHearingDayCourtSchedule(final List<HearingDayCourtSchedule> hearingDayCourtSchedules, final UUID hearingId) {
    this.hearingDayCourtSchedules = hearingDayCourtSchedules;
    this.hearingId = hearingId;
  }

  public List<HearingDayCourtSchedule> getHearingDayCourtSchedules() {
    return hearingDayCourtSchedules;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder updateHearingDayCourtSchedule() {
    return new uk.gov.justice.listing.courts.UpdateHearingDayCourtSchedule.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateHearingDayCourtSchedule that = (uk.gov.justice.listing.courts.UpdateHearingDayCourtSchedule) obj;

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

    public Builder withValuesFrom(final UpdateHearingDayCourtSchedule updateHearingDayCourtSchedule) {
      this.hearingDayCourtSchedules = updateHearingDayCourtSchedule.getHearingDayCourtSchedules();
      this.hearingId = updateHearingDayCourtSchedule.getHearingId();
      return this;
    }

    public UpdateHearingDayCourtSchedule build() {
      return new uk.gov.justice.listing.courts.UpdateHearingDayCourtSchedule(hearingDayCourtSchedules, hearingId);
    }
  }
}

package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.UUID;

public class NonDefaultDays implements Serializable {
  private static final long serialVersionUID = 5252722378752488892L;

  private final UUID courtCentreId;

  private final UUID courtScheduleId;

  private final Integer durationMinutes;

  private final UUID roomId;

  private final ZonedDateTime startTime;

  @JsonCreator
  public NonDefaultDays(final UUID courtCentreId, final UUID courtScheduleId, final Integer durationMinutes, final UUID roomId, final ZonedDateTime startTime) {
    this.courtCentreId = courtCentreId;
    this.courtScheduleId = courtScheduleId;
    this.durationMinutes = durationMinutes;
    this.roomId = roomId;
    this.startTime = startTime;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtScheduleId() {
    return courtScheduleId;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public static Builder nonDefaultDays() {
    return new uk.gov.justice.listing.courts.NonDefaultDays.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.NonDefaultDays that = (uk.gov.justice.listing.courts.NonDefaultDays) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.durationMinutes, that.durationMinutes) &&
    java.util.Objects.equals(this.roomId, that.roomId) &&
    java.util.Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtScheduleId, durationMinutes, roomId, startTime);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtScheduleId;

    private Integer durationMinutes;

    private UUID roomId;

    private ZonedDateTime startTime;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtScheduleId(final UUID courtScheduleId) {
      this.courtScheduleId = courtScheduleId;
      return this;
    }

    public Builder withDurationMinutes(final Integer durationMinutes) {
      this.durationMinutes = durationMinutes;
      return this;
    }

    public Builder withRoomId(final UUID roomId) {
      this.roomId = roomId;
      return this;
    }

    public Builder withStartTime(final ZonedDateTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder withValuesFrom(final NonDefaultDays nonDefaultDays) {
      this.courtCentreId = nonDefaultDays.getCourtCentreId();
      this.courtScheduleId = nonDefaultDays.getCourtScheduleId();
      this.durationMinutes = nonDefaultDays.getDurationMinutes();
      this.roomId = nonDefaultDays.getRoomId();
      this.startTime = nonDefaultDays.getStartTime();
      return this;
    }

    public NonDefaultDays build() {
      return new uk.gov.justice.listing.courts.NonDefaultDays(courtCentreId, courtScheduleId, durationMinutes, roomId, startTime);
    }
  }
}

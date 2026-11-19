package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.UUID;

public class ChangedDays implements Serializable {
  private static final long serialVersionUID = 5252722378752488892L;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final UUID courtScheduleId;

  private final Integer durationMinutes;

  private final String hearingDate;

  private final Boolean isDraft;

  private final ZonedDateTime startTime;

  @JsonCreator
  public ChangedDays(final UUID courtCentreId, final UUID courtRoomId, final UUID courtScheduleId, final Integer durationMinutes, final String hearingDate, final Boolean isDraft, final ZonedDateTime startTime) {
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.courtScheduleId = courtScheduleId;
    this.durationMinutes = durationMinutes;
    this.hearingDate = hearingDate;
    this.isDraft = isDraft;
    this.startTime = startTime;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public UUID getCourtScheduleId() {
    return courtScheduleId;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public String getHearingDate() {
    return hearingDate;
  }

  public Boolean getIsDraft() {
    return isDraft;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public static Builder changedDays() {
    return new uk.gov.justice.listing.courts.ChangedDays.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ChangedDays that = (uk.gov.justice.listing.courts.ChangedDays) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.durationMinutes, that.durationMinutes) &&
    java.util.Objects.equals(this.hearingDate, that.hearingDate) &&
    java.util.Objects.equals(this.isDraft, that.isDraft) &&
    java.util.Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtRoomId, courtScheduleId, durationMinutes, hearingDate, isDraft, startTime);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtRoomId;

    private UUID courtScheduleId;

    private Integer durationMinutes;

    private String hearingDate;

    private Boolean isDraft;

    private ZonedDateTime startTime;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
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

    public Builder withHearingDate(final String hearingDate) {
      this.hearingDate = hearingDate;
      return this;
    }

    public Builder withIsDraft(final Boolean isDraft) {
      this.isDraft = isDraft;
      return this;
    }

    public Builder withStartTime(final ZonedDateTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder withValuesFrom(final ChangedDays changedDays) {
      this.courtCentreId = changedDays.getCourtCentreId();
      this.courtRoomId = changedDays.getCourtRoomId();
      this.courtScheduleId = changedDays.getCourtScheduleId();
      this.durationMinutes = changedDays.getDurationMinutes();
      this.hearingDate = changedDays.getHearingDate();
      this.isDraft = changedDays.getIsDraft();
      this.startTime = changedDays.getStartTime();
      return this;
    }

    public ChangedDays build() {
      return new uk.gov.justice.listing.courts.ChangedDays(courtCentreId, courtRoomId, courtScheduleId, durationMinutes, hearingDate, isDraft, startTime);
    }
  }
}

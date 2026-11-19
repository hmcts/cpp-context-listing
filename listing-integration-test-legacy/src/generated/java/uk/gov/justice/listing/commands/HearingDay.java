package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class HearingDay implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final UUID courtScheduleId;

  private final Integer durationMinutes;

  private final ZonedDateTime endTime;

  private final Boolean hasSharedResults;

  private final LocalDate hearingDate;

  private final Boolean isCancelled;

  private final Boolean isDraft;

  private final Integer sequence;

  private final ZonedDateTime startTime;

  @JsonCreator
  public HearingDay(final UUID courtCentreId, final UUID courtRoomId, final UUID courtScheduleId, final Integer durationMinutes, final ZonedDateTime endTime, final Boolean hasSharedResults, final LocalDate hearingDate, final Boolean isCancelled, final Boolean isDraft, final Integer sequence, final ZonedDateTime startTime) {
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.courtScheduleId = courtScheduleId;
    this.durationMinutes = durationMinutes;
    this.endTime = endTime;
    this.hasSharedResults = hasSharedResults;
    this.hearingDate = hearingDate;
    this.isCancelled = isCancelled;
    this.isDraft = isDraft;
    this.sequence = sequence;
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

  public ZonedDateTime getEndTime() {
    return endTime;
  }

  public Boolean getHasSharedResults() {
    return hasSharedResults;
  }

  public LocalDate getHearingDate() {
    return hearingDate;
  }

  public Boolean getIsCancelled() {
    return isCancelled;
  }

  public Boolean getIsDraft() {
    return isDraft;
  }

  public Integer getSequence() {
    return sequence;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public static Builder hearingDay() {
    return new uk.gov.justice.listing.commands.HearingDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.HearingDay that = (uk.gov.justice.listing.commands.HearingDay) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.durationMinutes, that.durationMinutes) &&
    java.util.Objects.equals(this.endTime, that.endTime) &&
    java.util.Objects.equals(this.hasSharedResults, that.hasSharedResults) &&
    java.util.Objects.equals(this.hearingDate, that.hearingDate) &&
    java.util.Objects.equals(this.isCancelled, that.isCancelled) &&
    java.util.Objects.equals(this.isDraft, that.isDraft) &&
    java.util.Objects.equals(this.sequence, that.sequence) &&
    java.util.Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtRoomId, courtScheduleId, durationMinutes, endTime, hasSharedResults, hearingDate, isCancelled, isDraft, sequence, startTime);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtRoomId;

    private UUID courtScheduleId;

    private Integer durationMinutes;

    private ZonedDateTime endTime;

    private Boolean hasSharedResults;

    private LocalDate hearingDate;

    private Boolean isCancelled;

    private Boolean isDraft;

    private Integer sequence;

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

    public Builder withEndTime(final ZonedDateTime endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder withHasSharedResults(final Boolean hasSharedResults) {
      this.hasSharedResults = hasSharedResults;
      return this;
    }

    public Builder withHearingDate(final LocalDate hearingDate) {
      this.hearingDate = hearingDate;
      return this;
    }

    public Builder withIsCancelled(final Boolean isCancelled) {
      this.isCancelled = isCancelled;
      return this;
    }

    public Builder withIsDraft(final Boolean isDraft) {
      this.isDraft = isDraft;
      return this;
    }

    public Builder withSequence(final Integer sequence) {
      this.sequence = sequence;
      return this;
    }

    public Builder withStartTime(final ZonedDateTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder withValuesFrom(final HearingDay hearingDay) {
      this.courtCentreId = hearingDay.getCourtCentreId();
      this.courtRoomId = hearingDay.getCourtRoomId();
      this.courtScheduleId = hearingDay.getCourtScheduleId();
      this.durationMinutes = hearingDay.getDurationMinutes();
      this.endTime = hearingDay.getEndTime();
      this.hasSharedResults = hearingDay.getHasSharedResults();
      this.hearingDate = hearingDay.getHearingDate();
      this.isCancelled = hearingDay.getIsCancelled();
      this.isDraft = hearingDay.getIsDraft();
      this.sequence = hearingDay.getSequence();
      this.startTime = hearingDay.getStartTime();
      return this;
    }

    public HearingDay build() {
      return new uk.gov.justice.listing.commands.HearingDay(courtCentreId, courtRoomId, courtScheduleId, durationMinutes, endTime, hasSharedResults, hearingDate, isCancelled, isDraft, sequence, startTime);
    }
  }
}

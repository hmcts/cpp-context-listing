package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Sessions implements Serializable {
  private static final long serialVersionUID = -5855383537364288420L;

  private final UUID courtCentreId;

  private final String courtRoomId;

  private final UUID courtScheduleId;

  private final Integer durationInMinutes;

  private final Boolean isDraft;

  private final String sessionDate;

  private final String sessionEndTime;

  private final String sessionStartTime;

  @JsonCreator
  public Sessions(final UUID courtCentreId, final String courtRoomId, final UUID courtScheduleId, final Integer durationInMinutes, final Boolean isDraft, final String sessionDate, final String sessionEndTime, final String sessionStartTime) {
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.courtScheduleId = courtScheduleId;
    this.durationInMinutes = durationInMinutes;
    this.isDraft = isDraft;
    this.sessionDate = sessionDate;
    this.sessionEndTime = sessionEndTime;
    this.sessionStartTime = sessionStartTime;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public String getCourtRoomId() {
    return courtRoomId;
  }

  public UUID getCourtScheduleId() {
    return courtScheduleId;
  }

  public Integer getDurationInMinutes() {
    return durationInMinutes;
  }

  public Boolean getIsDraft() {
    return isDraft;
  }

  public String getSessionDate() {
    return sessionDate;
  }

  public String getSessionEndTime() {
    return sessionEndTime;
  }

  public String getSessionStartTime() {
    return sessionStartTime;
  }

  public static Builder sessions() {
    return new uk.gov.justice.listing.courts.Sessions.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Sessions that = (uk.gov.justice.listing.courts.Sessions) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.durationInMinutes, that.durationInMinutes) &&
    java.util.Objects.equals(this.isDraft, that.isDraft) &&
    java.util.Objects.equals(this.sessionDate, that.sessionDate) &&
    java.util.Objects.equals(this.sessionEndTime, that.sessionEndTime) &&
    java.util.Objects.equals(this.sessionStartTime, that.sessionStartTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtRoomId, courtScheduleId, durationInMinutes, isDraft, sessionDate, sessionEndTime, sessionStartTime);}

  public static class Builder {
    private UUID courtCentreId;

    private String courtRoomId;

    private UUID courtScheduleId;

    private Integer durationInMinutes;

    private Boolean isDraft;

    private String sessionDate;

    private String sessionEndTime;

    private String sessionStartTime;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final String courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withCourtScheduleId(final UUID courtScheduleId) {
      this.courtScheduleId = courtScheduleId;
      return this;
    }

    public Builder withDurationInMinutes(final Integer durationInMinutes) {
      this.durationInMinutes = durationInMinutes;
      return this;
    }

    public Builder withIsDraft(final Boolean isDraft) {
      this.isDraft = isDraft;
      return this;
    }

    public Builder withSessionDate(final String sessionDate) {
      this.sessionDate = sessionDate;
      return this;
    }

    public Builder withSessionEndTime(final String sessionEndTime) {
      this.sessionEndTime = sessionEndTime;
      return this;
    }

    public Builder withSessionStartTime(final String sessionStartTime) {
      this.sessionStartTime = sessionStartTime;
      return this;
    }

    public Builder withValuesFrom(final Sessions sessions) {
      this.courtCentreId = sessions.getCourtCentreId();
      this.courtRoomId = sessions.getCourtRoomId();
      this.courtScheduleId = sessions.getCourtScheduleId();
      this.durationInMinutes = sessions.getDurationInMinutes();
      this.isDraft = sessions.getIsDraft();
      this.sessionDate = sessions.getSessionDate();
      this.sessionEndTime = sessions.getSessionEndTime();
      this.sessionStartTime = sessions.getSessionStartTime();
      return this;
    }

    public Sessions build() {
      return new uk.gov.justice.listing.courts.Sessions(courtCentreId, courtRoomId, courtScheduleId, durationInMinutes, isDraft, sessionDate, sessionEndTime, sessionStartTime);
    }
  }
}

package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class MoveHearingToPastDateEnriched implements Serializable {
  private static final long serialVersionUID = -5855383537364288420L;

  private final UUID courtCentreId;

  private final String courtRoomId;

  private final UUID courtScheduleId;

  private final Integer durationInMinutes;

  private final String endDate;

  private final UUID hearingId;

  private final Jurisdiction jurisdiction;

  private final String sessionDate;

  private final String sessionEndTime;

  private final String sessionStartTime;

  private final List<Sessions> sessions;

  private final String startDate;

  @JsonCreator
  public MoveHearingToPastDateEnriched(final UUID courtCentreId, final String courtRoomId, final UUID courtScheduleId, final Integer durationInMinutes, final String endDate, final UUID hearingId, final Jurisdiction jurisdiction, final String sessionDate, final String sessionEndTime, final String sessionStartTime, final List<Sessions> sessions, final String startDate) {
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.courtScheduleId = courtScheduleId;
    this.durationInMinutes = durationInMinutes;
    this.endDate = endDate;
    this.hearingId = hearingId;
    this.jurisdiction = jurisdiction;
    this.sessionDate = sessionDate;
    this.sessionEndTime = sessionEndTime;
    this.sessionStartTime = sessionStartTime;
    this.sessions = sessions;
    this.startDate = startDate;
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

  public String getEndDate() {
    return endDate;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Jurisdiction getJurisdiction() {
    return jurisdiction;
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

  public List<Sessions> getSessions() {
    return sessions;
  }

  public String getStartDate() {
    return startDate;
  }

  public static Builder moveHearingToPastDateEnriched() {
    return new uk.gov.justice.listing.courts.MoveHearingToPastDateEnriched.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.MoveHearingToPastDateEnriched that = (uk.gov.justice.listing.courts.MoveHearingToPastDateEnriched) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.durationInMinutes, that.durationInMinutes) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.jurisdiction, that.jurisdiction) &&
    java.util.Objects.equals(this.sessionDate, that.sessionDate) &&
    java.util.Objects.equals(this.sessionEndTime, that.sessionEndTime) &&
    java.util.Objects.equals(this.sessionStartTime, that.sessionStartTime) &&
    java.util.Objects.equals(this.sessions, that.sessions) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtRoomId, courtScheduleId, durationInMinutes, endDate, hearingId, jurisdiction, sessionDate, sessionEndTime, sessionStartTime, sessions, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private String courtRoomId;

    private UUID courtScheduleId;

    private Integer durationInMinutes;

    private String endDate;

    private UUID hearingId;

    private Jurisdiction jurisdiction;

    private String sessionDate;

    private String sessionEndTime;

    private String sessionStartTime;

    private List<Sessions> sessions;

    private String startDate;

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

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withJurisdiction(final Jurisdiction jurisdiction) {
      this.jurisdiction = jurisdiction;
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

    public Builder withSessions(final List<Sessions> sessions) {
      this.sessions = sessions;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final MoveHearingToPastDateEnriched moveHearingToPastDateEnriched) {
      this.courtCentreId = moveHearingToPastDateEnriched.getCourtCentreId();
      this.courtRoomId = moveHearingToPastDateEnriched.getCourtRoomId();
      this.courtScheduleId = moveHearingToPastDateEnriched.getCourtScheduleId();
      this.durationInMinutes = moveHearingToPastDateEnriched.getDurationInMinutes();
      this.endDate = moveHearingToPastDateEnriched.getEndDate();
      this.hearingId = moveHearingToPastDateEnriched.getHearingId();
      this.jurisdiction = moveHearingToPastDateEnriched.getJurisdiction();
      this.sessionDate = moveHearingToPastDateEnriched.getSessionDate();
      this.sessionEndTime = moveHearingToPastDateEnriched.getSessionEndTime();
      this.sessionStartTime = moveHearingToPastDateEnriched.getSessionStartTime();
      this.sessions = moveHearingToPastDateEnriched.getSessions();
      this.startDate = moveHearingToPastDateEnriched.getStartDate();
      return this;
    }

    public MoveHearingToPastDateEnriched build() {
      return new uk.gov.justice.listing.courts.MoveHearingToPastDateEnriched(courtCentreId, courtRoomId, courtScheduleId, durationInMinutes, endDate, hearingId, jurisdiction, sessionDate, sessionEndTime, sessionStartTime, sessions, startDate);
    }
  }
}

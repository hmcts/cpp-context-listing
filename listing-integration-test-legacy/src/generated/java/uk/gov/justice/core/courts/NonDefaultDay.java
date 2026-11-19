package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;

public class NonDefaultDay implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String courtCentreId;

  private final Integer courtRoomId;

  private final String courtScheduleId;

  private final Integer duration;

  private final String oucode;

  private final String roomId;

  private final String session;

  private final ZonedDateTime startTime;

  @JsonCreator
  public NonDefaultDay(final String courtCentreId, final Integer courtRoomId, final String courtScheduleId, final Integer duration, final String oucode, final String roomId, final String session, final ZonedDateTime startTime) {
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.courtScheduleId = courtScheduleId;
    this.duration = duration;
    this.oucode = oucode;
    this.roomId = roomId;
    this.session = session;
    this.startTime = startTime;
  }

  public String getCourtCentreId() {
    return courtCentreId;
  }

  public Integer getCourtRoomId() {
    return courtRoomId;
  }

  public String getCourtScheduleId() {
    return courtScheduleId;
  }

  public Integer getDuration() {
    return duration;
  }

  public String getOucode() {
    return oucode;
  }

  public String getRoomId() {
    return roomId;
  }

  public String getSession() {
    return session;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public static Builder nonDefaultDay() {
    return new uk.gov.justice.core.courts.NonDefaultDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.NonDefaultDay that = (uk.gov.justice.core.courts.NonDefaultDay) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.duration, that.duration) &&
    java.util.Objects.equals(this.oucode, that.oucode) &&
    java.util.Objects.equals(this.roomId, that.roomId) &&
    java.util.Objects.equals(this.session, that.session) &&
    java.util.Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtRoomId, courtScheduleId, duration, oucode, roomId, session, startTime);}

  public static class Builder {
    private String courtCentreId;

    private Integer courtRoomId;

    private String courtScheduleId;

    private Integer duration;

    private String oucode;

    private String roomId;

    private String session;

    private ZonedDateTime startTime;

    public Builder withCourtCentreId(final String courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final Integer courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withCourtScheduleId(final String courtScheduleId) {
      this.courtScheduleId = courtScheduleId;
      return this;
    }

    public Builder withDuration(final Integer duration) {
      this.duration = duration;
      return this;
    }

    public Builder withOucode(final String oucode) {
      this.oucode = oucode;
      return this;
    }

    public Builder withRoomId(final String roomId) {
      this.roomId = roomId;
      return this;
    }

    public Builder withSession(final String session) {
      this.session = session;
      return this;
    }

    public Builder withStartTime(final ZonedDateTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder withValuesFrom(final NonDefaultDay nonDefaultDay) {
      this.courtCentreId = nonDefaultDay.getCourtCentreId();
      this.courtRoomId = nonDefaultDay.getCourtRoomId();
      this.courtScheduleId = nonDefaultDay.getCourtScheduleId();
      this.duration = nonDefaultDay.getDuration();
      this.oucode = nonDefaultDay.getOucode();
      this.roomId = nonDefaultDay.getRoomId();
      this.session = nonDefaultDay.getSession();
      this.startTime = nonDefaultDay.getStartTime();
      return this;
    }

    public NonDefaultDay build() {
      return new uk.gov.justice.core.courts.NonDefaultDay(courtCentreId, courtRoomId, courtScheduleId, duration, oucode, roomId, session, startTime);
    }
  }
}

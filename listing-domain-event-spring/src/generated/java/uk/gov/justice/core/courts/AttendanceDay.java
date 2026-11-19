package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import uk.gov.justice.listing.events.AttendanceType;

public class AttendanceDay implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final AttendanceType attendanceType;

  private final String day;

  @JsonCreator
  public AttendanceDay(final AttendanceType attendanceType, final String day) {
    this.attendanceType = attendanceType;
    this.day = day;
  }

  public AttendanceType getAttendanceType() {
    return attendanceType;
  }

  public String getDay() {
    return day;
  }

  public static Builder attendanceDay() {
    return new uk.gov.justice.core.courts.AttendanceDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.AttendanceDay that = (uk.gov.justice.core.courts.AttendanceDay) obj;

    return java.util.Objects.equals(this.attendanceType, that.attendanceType) &&
    java.util.Objects.equals(this.day, that.day);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendanceType, day);}

  public static class Builder {
    private AttendanceType attendanceType;

    private String day;

    public Builder withAttendanceType(final AttendanceType attendanceType) {
      this.attendanceType = attendanceType;
      return this;
    }

    public Builder withDay(final String day) {
      this.day = day;
      return this;
    }

    public Builder withValuesFrom(final AttendanceDay attendanceDay) {
      this.attendanceType = attendanceDay.getAttendanceType();
      this.day = attendanceDay.getDay();
      return this;
    }

    public AttendanceDay build() {
      return new uk.gov.justice.core.courts.AttendanceDay(attendanceType, day);
    }
  }
}

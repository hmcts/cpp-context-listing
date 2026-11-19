package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class DefendantAttendance implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<AttendanceDay> attendanceDays;

  private final UUID defendantId;

  @JsonCreator
  public DefendantAttendance(final List<AttendanceDay> attendanceDays, final UUID defendantId) {
    this.attendanceDays = attendanceDays;
    this.defendantId = defendantId;
  }

  public List<AttendanceDay> getAttendanceDays() {
    return attendanceDays;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public static Builder defendantAttendance() {
    return new uk.gov.justice.core.courts.DefendantAttendance.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefendantAttendance that = (uk.gov.justice.core.courts.DefendantAttendance) obj;

    return java.util.Objects.equals(this.attendanceDays, that.attendanceDays) &&
    java.util.Objects.equals(this.defendantId, that.defendantId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendanceDays, defendantId);}

  public static class Builder {
    private List<AttendanceDay> attendanceDays;

    private UUID defendantId;

    public Builder withAttendanceDays(final List<AttendanceDay> attendanceDays) {
      this.attendanceDays = attendanceDays;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withValuesFrom(final DefendantAttendance defendantAttendance) {
      this.attendanceDays = defendantAttendance.getAttendanceDays();
      this.defendantId = defendantAttendance.getDefendantId();
      return this;
    }

    public DefendantAttendance build() {
      return new uk.gov.justice.core.courts.DefendantAttendance(attendanceDays, defendantId);
    }
  }
}

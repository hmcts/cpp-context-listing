package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class CourtApplicationPartyAttendance implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<AttendanceDay> attendanceDays;

  private final UUID courtApplicationPartyId;

  @JsonCreator
  public CourtApplicationPartyAttendance(final List<AttendanceDay> attendanceDays, final UUID courtApplicationPartyId) {
    this.attendanceDays = attendanceDays;
    this.courtApplicationPartyId = courtApplicationPartyId;
  }

  public List<AttendanceDay> getAttendanceDays() {
    return attendanceDays;
  }

  public UUID getCourtApplicationPartyId() {
    return courtApplicationPartyId;
  }

  public static Builder courtApplicationPartyAttendance() {
    return new uk.gov.justice.core.courts.CourtApplicationPartyAttendance.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplicationPartyAttendance that = (uk.gov.justice.core.courts.CourtApplicationPartyAttendance) obj;

    return java.util.Objects.equals(this.attendanceDays, that.attendanceDays) &&
    java.util.Objects.equals(this.courtApplicationPartyId, that.courtApplicationPartyId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendanceDays, courtApplicationPartyId);}

  public static class Builder {
    private List<AttendanceDay> attendanceDays;

    private UUID courtApplicationPartyId;

    public Builder withAttendanceDays(final List<AttendanceDay> attendanceDays) {
      this.attendanceDays = attendanceDays;
      return this;
    }

    public Builder withCourtApplicationPartyId(final UUID courtApplicationPartyId) {
      this.courtApplicationPartyId = courtApplicationPartyId;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationPartyAttendance courtApplicationPartyAttendance) {
      this.attendanceDays = courtApplicationPartyAttendance.getAttendanceDays();
      this.courtApplicationPartyId = courtApplicationPartyAttendance.getCourtApplicationPartyId();
      return this;
    }

    public CourtApplicationPartyAttendance build() {
      return new uk.gov.justice.core.courts.CourtApplicationPartyAttendance(attendanceDays, courtApplicationPartyId);
    }
  }
}

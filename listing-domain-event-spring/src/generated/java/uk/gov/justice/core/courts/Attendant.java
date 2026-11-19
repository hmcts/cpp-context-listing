package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.listing.events.AttendantType;

public class Attendant implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final AttendantType attendantType;

  private final UUID defendantId;

  private final String name;

  @JsonCreator
  public Attendant(final AttendantType attendantType, final UUID defendantId, final String name) {
    this.attendantType = attendantType;
    this.defendantId = defendantId;
    this.name = name;
  }

  public AttendantType getAttendantType() {
    return attendantType;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public String getName() {
    return name;
  }

  public static Builder attendant() {
    return new uk.gov.justice.core.courts.Attendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Attendant that = (uk.gov.justice.core.courts.Attendant) obj;

    return java.util.Objects.equals(this.attendantType, that.attendantType) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendantType, defendantId, name);}

  public static class Builder {
    private AttendantType attendantType;

    private UUID defendantId;

    private String name;

    public Builder withAttendantType(final AttendantType attendantType) {
      this.attendantType = attendantType;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withValuesFrom(final Attendant attendant) {
      this.attendantType = attendant.getAttendantType();
      this.defendantId = attendant.getDefendantId();
      this.name = attendant.getName();
      return this;
    }

    public Attendant build() {
      return new uk.gov.justice.core.courts.Attendant(attendantType, defendantId, name);
    }
  }
}

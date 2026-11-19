package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.events.Role;

public class InterpreterIntermediary implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<String> attendanceDays;

  private final Attendant attendant;

  private final String firstName;

  private final UUID id;

  private final String lastName;

  private final Role role;

  @JsonCreator
  public InterpreterIntermediary(final List<String> attendanceDays, final Attendant attendant, final String firstName, final UUID id, final String lastName, final Role role) {
    this.attendanceDays = attendanceDays;
    this.attendant = attendant;
    this.firstName = firstName;
    this.id = id;
    this.lastName = lastName;
    this.role = role;
  }

  public List<String> getAttendanceDays() {
    return attendanceDays;
  }

  public Attendant getAttendant() {
    return attendant;
  }

  public String getFirstName() {
    return firstName;
  }

  public UUID getId() {
    return id;
  }

  public String getLastName() {
    return lastName;
  }

  public Role getRole() {
    return role;
  }

  public static Builder interpreterIntermediary() {
    return new uk.gov.justice.core.courts.InterpreterIntermediary.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.InterpreterIntermediary that = (uk.gov.justice.core.courts.InterpreterIntermediary) obj;

    return java.util.Objects.equals(this.attendanceDays, that.attendanceDays) &&
    java.util.Objects.equals(this.attendant, that.attendant) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.role, that.role);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendanceDays, attendant, firstName, id, lastName, role);}

  public static class Builder {
    private List<String> attendanceDays;

    private Attendant attendant;

    private String firstName;

    private UUID id;

    private String lastName;

    private Role role;

    public Builder withAttendanceDays(final List<String> attendanceDays) {
      this.attendanceDays = attendanceDays;
      return this;
    }

    public Builder withAttendant(final Attendant attendant) {
      this.attendant = attendant;
      return this;
    }

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withRole(final Role role) {
      this.role = role;
      return this;
    }

    public Builder withValuesFrom(final InterpreterIntermediary interpreterIntermediary) {
      this.attendanceDays = interpreterIntermediary.getAttendanceDays();
      this.attendant = interpreterIntermediary.getAttendant();
      this.firstName = interpreterIntermediary.getFirstName();
      this.id = interpreterIntermediary.getId();
      this.lastName = interpreterIntermediary.getLastName();
      this.role = interpreterIntermediary.getRole();
      return this;
    }

    public InterpreterIntermediary build() {
      return new uk.gov.justice.core.courts.InterpreterIntermediary(attendanceDays, attendant, firstName, id, lastName, role);
    }
  }
}

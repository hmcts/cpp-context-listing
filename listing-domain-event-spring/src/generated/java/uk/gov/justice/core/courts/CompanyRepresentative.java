package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CompanyRepresentative implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<String> attendanceDays;

  private final List<UUID> defendants;

  private final String firstName;

  private final UUID id;

  private final String lastName;

  private final String position;

  private final String title;

  @JsonCreator
  public CompanyRepresentative(final List<String> attendanceDays, final List<UUID> defendants, final String firstName, final UUID id, final String lastName, final String position, final String title) {
    this.attendanceDays = attendanceDays;
    this.defendants = defendants;
    this.firstName = firstName;
    this.id = id;
    this.lastName = lastName;
    this.position = position;
    this.title = title;
  }

  public List<String> getAttendanceDays() {
    return attendanceDays;
  }

  public List<UUID> getDefendants() {
    return defendants;
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

  public String getPosition() {
    return position;
  }

  public String getTitle() {
    return title;
  }

  public static Builder companyRepresentative() {
    return new uk.gov.justice.core.courts.CompanyRepresentative.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CompanyRepresentative that = (uk.gov.justice.core.courts.CompanyRepresentative) obj;

    return java.util.Objects.equals(this.attendanceDays, that.attendanceDays) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.position, that.position) &&
    java.util.Objects.equals(this.title, that.title);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendanceDays, defendants, firstName, id, lastName, position, title);}

  public static class Builder {
    private List<String> attendanceDays;

    private List<UUID> defendants;

    private String firstName;

    private UUID id;

    private String lastName;

    private String position;

    private String title;

    public Builder withAttendanceDays(final List<String> attendanceDays) {
      this.attendanceDays = attendanceDays;
      return this;
    }

    public Builder withDefendants(final List<UUID> defendants) {
      this.defendants = defendants;
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

    public Builder withPosition(final String position) {
      this.position = position;
      return this;
    }

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withValuesFrom(final CompanyRepresentative companyRepresentative) {
      this.attendanceDays = companyRepresentative.getAttendanceDays();
      this.defendants = companyRepresentative.getDefendants();
      this.firstName = companyRepresentative.getFirstName();
      this.id = companyRepresentative.getId();
      this.lastName = companyRepresentative.getLastName();
      this.position = companyRepresentative.getPosition();
      this.title = companyRepresentative.getTitle();
      return this;
    }

    public CompanyRepresentative build() {
      return new uk.gov.justice.core.courts.CompanyRepresentative(attendanceDays, defendants, firstName, id, lastName, position, title);
    }
  }
}

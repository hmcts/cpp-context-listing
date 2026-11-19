package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class AssociatedPerson implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Person person;

  private final String role;

  @JsonCreator
  public AssociatedPerson(final Person person, final String role) {
    this.person = person;
    this.role = role;
  }

  public Person getPerson() {
    return person;
  }

  public String getRole() {
    return role;
  }

  public static Builder associatedPerson() {
    return new uk.gov.justice.core.courts.AssociatedPerson.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.AssociatedPerson that = (uk.gov.justice.core.courts.AssociatedPerson) obj;

    return java.util.Objects.equals(this.person, that.person) &&
    java.util.Objects.equals(this.role, that.role);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(person, role);}

  public static class Builder {
    private Person person;

    private String role;

    public Builder withPerson(final Person person) {
      this.person = person;
      return this;
    }

    public Builder withRole(final String role) {
      this.role = role;
      return this;
    }

    public Builder withValuesFrom(final AssociatedPerson associatedPerson) {
      this.person = associatedPerson.getPerson();
      this.role = associatedPerson.getRole();
      return this;
    }

    public AssociatedPerson build() {
      return new uk.gov.justice.core.courts.AssociatedPerson(person, role);
    }
  }
}

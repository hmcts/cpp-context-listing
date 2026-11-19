package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class DefendantAlias implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String firstName;

  private final String lastName;

  private final String legalEntityName;

  private final String middleName;

  private final String title;

  @JsonCreator
  public DefendantAlias(final String firstName, final String lastName, final String legalEntityName, final String middleName, final String title) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.legalEntityName = legalEntityName;
    this.middleName = middleName;
    this.title = title;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getLegalEntityName() {
    return legalEntityName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public String getTitle() {
    return title;
  }

  public static Builder defendantAlias() {
    return new uk.gov.justice.core.courts.DefendantAlias.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefendantAlias that = (uk.gov.justice.core.courts.DefendantAlias) obj;

    return java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.legalEntityName, that.legalEntityName) &&
    java.util.Objects.equals(this.middleName, that.middleName) &&
    java.util.Objects.equals(this.title, that.title);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(firstName, lastName, legalEntityName, middleName, title);}

  public static class Builder {
    private String firstName;

    private String lastName;

    private String legalEntityName;

    private String middleName;

    private String title;

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withLegalEntityName(final String legalEntityName) {
      this.legalEntityName = legalEntityName;
      return this;
    }

    public Builder withMiddleName(final String middleName) {
      this.middleName = middleName;
      return this;
    }

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withValuesFrom(final DefendantAlias defendantAlias) {
      this.firstName = defendantAlias.getFirstName();
      this.lastName = defendantAlias.getLastName();
      this.legalEntityName = defendantAlias.getLegalEntityName();
      this.middleName = defendantAlias.getMiddleName();
      this.title = defendantAlias.getTitle();
      return this;
    }

    public DefendantAlias build() {
      return new uk.gov.justice.core.courts.DefendantAlias(firstName, lastName, legalEntityName, middleName, title);
    }
  }
}

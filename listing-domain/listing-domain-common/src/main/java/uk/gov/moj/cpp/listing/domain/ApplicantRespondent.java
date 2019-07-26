package uk.gov.moj.cpp.listing.domain;

import java.util.Optional;

public class ApplicantRespondent {

  private final String firstName;

  private final Boolean isRespondent;

  private final String lastName;

  public ApplicantRespondent(final String firstName, final Boolean isRespondent, final String lastName) {
    this.firstName = firstName;
    this.isRespondent = isRespondent;
    this.lastName = lastName;
  }

  public Optional<String> getFirstName() {
    return Optional.ofNullable(firstName);
  }

  public Boolean getIsRespondent() {
    return isRespondent;
  }

  public String getLastName() {
    return lastName;
  }

  public static Builder applicantRespondent() {
    return new ApplicantRespondent.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ApplicantRespondent that = (ApplicantRespondent) obj;

    return java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.isRespondent, that.isRespondent) &&
    java.util.Objects.equals(this.lastName, that.lastName);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(firstName, isRespondent, lastName);}

  @Override
  public String toString() {
    return "ApplicantRespondent{" +
    	"firstName='" + firstName + "'," +
    	"isRespondent='" + isRespondent + "'," +
    	"lastName='" + lastName + "'" +
    "}";
  }

  public static class Builder {

    private String firstName;

    private Boolean isRespondent;

    private String lastName;

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withIsRespondent(final Boolean isRespondent) {
      this.isRespondent = isRespondent;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public ApplicantRespondent build() {
      return new ApplicantRespondent(firstName, isRespondent, lastName);
    }
  }
}

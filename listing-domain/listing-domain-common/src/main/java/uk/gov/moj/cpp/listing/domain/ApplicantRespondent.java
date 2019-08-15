package uk.gov.moj.cpp.listing.domain;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "PMD:BeanMembersShouldSerialize"})
public class ApplicantRespondent {

  private final UUID id ;

  private final String firstName;

  private final Boolean isRespondent;

  private final String lastName;

  private final CourtApplicationPartyType courtApplicationPartyType;

  public ApplicantRespondent(final UUID id, final String firstName, final Boolean isRespondent, final String lastName, final CourtApplicationPartyType courtApplicationPartyType) {
    this.id = id;
    this.firstName = firstName;
    this.isRespondent = isRespondent;
    this.lastName = lastName;
    this.courtApplicationPartyType = courtApplicationPartyType;
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

  public UUID getId() {
    return id;
  }

  public static Builder applicantRespondent() {
    return new ApplicantRespondent.Builder();
  }

  public CourtApplicationPartyType getCourtApplicationPartyType() {
    return courtApplicationPartyType;
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

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.isRespondent, that.isRespondent) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.courtApplicationPartyType, that.courtApplicationPartyType) ;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, firstName, isRespondent, lastName, courtApplicationPartyType);
  }

  @Override
  public String toString() {
    return "ApplicantRespondent{" +
        "id='" + id + "'," +
    	"firstName='" + firstName + "'," +
    	"isRespondent='" + isRespondent + "'," +
    	"lastName='" + lastName + "'," +
        "courtApplicationPartyType='" + courtApplicationPartyType + "'" +
    "}";
  }

  public static class Builder {

    private UUID id ;

    private String firstName;

    private Boolean isRespondent;

    private String lastName;

    private CourtApplicationPartyType courtApplicationPartyType;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

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

    public Builder withCourtApplicationPartyType(final CourtApplicationPartyType courtApplicationPartyType) {
      this.courtApplicationPartyType = courtApplicationPartyType;
      return this;
    }

    public ApplicantRespondent build() {
      return new ApplicantRespondent(id, firstName, isRespondent, lastName, courtApplicationPartyType);
    }
  }
}

package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.core.courts.Address;

public class ApplicantRespondent implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final Address address;

  private final CourtApplicationPartyType courtApplicationPartyType;

  private final String dateOfBirth;

  private final String firstName;

  private final UUID id;

  private final Boolean isRespondent;

  private final String lastName;

  private final UUID masterDefendantId;

  private final Boolean restrictFromCourtList;

  @JsonCreator
  public ApplicantRespondent(final Address address, final CourtApplicationPartyType courtApplicationPartyType, final String dateOfBirth, final String firstName, final UUID id, final Boolean isRespondent, final String lastName, final UUID masterDefendantId, final Boolean restrictFromCourtList) {
    this.address = address;
    this.courtApplicationPartyType = courtApplicationPartyType;
    this.dateOfBirth = dateOfBirth;
    this.firstName = firstName;
    this.id = id;
    this.isRespondent = isRespondent;
    this.lastName = lastName;
    this.masterDefendantId = masterDefendantId;
    this.restrictFromCourtList = restrictFromCourtList;
  }

  public Address getAddress() {
    return address;
  }

  public CourtApplicationPartyType getCourtApplicationPartyType() {
    return courtApplicationPartyType;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public String getFirstName() {
    return firstName;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsRespondent() {
    return isRespondent;
  }

  public String getLastName() {
    return lastName;
  }

  public UUID getMasterDefendantId() {
    return masterDefendantId;
  }

  public Boolean getRestrictFromCourtList() {
    return restrictFromCourtList;
  }

  public static Builder applicantRespondent() {
    return new uk.gov.justice.listing.events.ApplicantRespondent.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ApplicantRespondent that = (uk.gov.justice.listing.events.ApplicantRespondent) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.courtApplicationPartyType, that.courtApplicationPartyType) &&
    java.util.Objects.equals(this.dateOfBirth, that.dateOfBirth) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isRespondent, that.isRespondent) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, courtApplicationPartyType, dateOfBirth, firstName, id, isRespondent, lastName, masterDefendantId, restrictFromCourtList);}

  public static class Builder {
    private Address address;

    private CourtApplicationPartyType courtApplicationPartyType;

    private String dateOfBirth;

    private String firstName;

    private UUID id;

    private Boolean isRespondent;

    private String lastName;

    private UUID masterDefendantId;

    private Boolean restrictFromCourtList;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withCourtApplicationPartyType(final CourtApplicationPartyType courtApplicationPartyType) {
      this.courtApplicationPartyType = courtApplicationPartyType;
      return this;
    }

    public Builder withDateOfBirth(final String dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
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

    public Builder withIsRespondent(final Boolean isRespondent) {
      this.isRespondent = isRespondent;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withMasterDefendantId(final UUID masterDefendantId) {
      this.masterDefendantId = masterDefendantId;
      return this;
    }

    public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
      this.restrictFromCourtList = restrictFromCourtList;
      return this;
    }

    public Builder withValuesFrom(final ApplicantRespondent applicantRespondent) {
      this.address = applicantRespondent.getAddress();
      this.courtApplicationPartyType = applicantRespondent.getCourtApplicationPartyType();
      this.dateOfBirth = applicantRespondent.getDateOfBirth();
      this.firstName = applicantRespondent.getFirstName();
      this.id = applicantRespondent.getId();
      this.isRespondent = applicantRespondent.getIsRespondent();
      this.lastName = applicantRespondent.getLastName();
      this.masterDefendantId = applicantRespondent.getMasterDefendantId();
      this.restrictFromCourtList = applicantRespondent.getRestrictFromCourtList();
      return this;
    }

    public ApplicantRespondent build() {
      return new uk.gov.justice.listing.events.ApplicantRespondent(address, courtApplicationPartyType, dateOfBirth, firstName, id, isRespondent, lastName, masterDefendantId, restrictFromCourtList);
    }
  }
}

package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;

public class NewBaseDefendant implements Serializable {
  private static final long serialVersionUID = -5509604120113450525L;

  private final Address address;

  private final BailStatus bailStatus;

  private final String custodyTimeLimit;

  private final String dateOfBirth;

  private final String defenceOrganisation;

  private final String firstName;

  private final UUID id;

  private final Boolean isYouth;

  private final String lastName;

  private final UUID masterDefendantId;

  private final String nationalityDescription;

  private final String organisationName;

  private final String specificRequirements;

  @JsonCreator
  public NewBaseDefendant(final Address address, final BailStatus bailStatus, final String custodyTimeLimit, final String dateOfBirth, final String defenceOrganisation, final String firstName, final UUID id, final Boolean isYouth, final String lastName, final UUID masterDefendantId, final String nationalityDescription, final String organisationName, final String specificRequirements) {
    this.address = address;
    this.bailStatus = bailStatus;
    this.custodyTimeLimit = custodyTimeLimit;
    this.dateOfBirth = dateOfBirth;
    this.defenceOrganisation = defenceOrganisation;
    this.firstName = firstName;
    this.id = id;
    this.isYouth = isYouth;
    this.lastName = lastName;
    this.masterDefendantId = masterDefendantId;
    this.nationalityDescription = nationalityDescription;
    this.organisationName = organisationName;
    this.specificRequirements = specificRequirements;
  }

  public Address getAddress() {
    return address;
  }

  public BailStatus getBailStatus() {
    return bailStatus;
  }

  public String getCustodyTimeLimit() {
    return custodyTimeLimit;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public String getDefenceOrganisation() {
    return defenceOrganisation;
  }

  public String getFirstName() {
    return firstName;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsYouth() {
    return isYouth;
  }

  public String getLastName() {
    return lastName;
  }

  public UUID getMasterDefendantId() {
    return masterDefendantId;
  }

  public String getNationalityDescription() {
    return nationalityDescription;
  }

  public String getOrganisationName() {
    return organisationName;
  }

  public String getSpecificRequirements() {
    return specificRequirements;
  }

  public static Builder newBaseDefendant() {
    return new uk.gov.justice.listing.events.NewBaseDefendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NewBaseDefendant that = (uk.gov.justice.listing.events.NewBaseDefendant) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.bailStatus, that.bailStatus) &&
    java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
    java.util.Objects.equals(this.dateOfBirth, that.dateOfBirth) &&
    java.util.Objects.equals(this.defenceOrganisation, that.defenceOrganisation) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isYouth, that.isYouth) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.nationalityDescription, that.nationalityDescription) &&
    java.util.Objects.equals(this.organisationName, that.organisationName) &&
    java.util.Objects.equals(this.specificRequirements, that.specificRequirements);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, bailStatus, custodyTimeLimit, dateOfBirth, defenceOrganisation, firstName, id, isYouth, lastName, masterDefendantId, nationalityDescription, organisationName, specificRequirements);}

  public static class Builder {
    private Address address;

    private BailStatus bailStatus;

    private String custodyTimeLimit;

    private String dateOfBirth;

    private String defenceOrganisation;

    private String firstName;

    private UUID id;

    private Boolean isYouth;

    private String lastName;

    private UUID masterDefendantId;

    private String nationalityDescription;

    private String organisationName;

    private String specificRequirements;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withBailStatus(final BailStatus bailStatus) {
      this.bailStatus = bailStatus;
      return this;
    }

    public Builder withCustodyTimeLimit(final String custodyTimeLimit) {
      this.custodyTimeLimit = custodyTimeLimit;
      return this;
    }

    public Builder withDateOfBirth(final String dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public Builder withDefenceOrganisation(final String defenceOrganisation) {
      this.defenceOrganisation = defenceOrganisation;
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

    public Builder withIsYouth(final Boolean isYouth) {
      this.isYouth = isYouth;
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

    public Builder withNationalityDescription(final String nationalityDescription) {
      this.nationalityDescription = nationalityDescription;
      return this;
    }

    public Builder withOrganisationName(final String organisationName) {
      this.organisationName = organisationName;
      return this;
    }

    public Builder withSpecificRequirements(final String specificRequirements) {
      this.specificRequirements = specificRequirements;
      return this;
    }

    public Builder withValuesFrom(final NewBaseDefendant newBaseDefendant) {
      this.address = newBaseDefendant.getAddress();
      this.bailStatus = newBaseDefendant.getBailStatus();
      this.custodyTimeLimit = newBaseDefendant.getCustodyTimeLimit();
      this.dateOfBirth = newBaseDefendant.getDateOfBirth();
      this.defenceOrganisation = newBaseDefendant.getDefenceOrganisation();
      this.firstName = newBaseDefendant.getFirstName();
      this.id = newBaseDefendant.getId();
      this.isYouth = newBaseDefendant.getIsYouth();
      this.lastName = newBaseDefendant.getLastName();
      this.masterDefendantId = newBaseDefendant.getMasterDefendantId();
      this.nationalityDescription = newBaseDefendant.getNationalityDescription();
      this.organisationName = newBaseDefendant.getOrganisationName();
      this.specificRequirements = newBaseDefendant.getSpecificRequirements();
      return this;
    }

    public NewBaseDefendant build() {
      return new uk.gov.justice.listing.events.NewBaseDefendant(address, bailStatus, custodyTimeLimit, dateOfBirth, defenceOrganisation, firstName, id, isYouth, lastName, masterDefendantId, nationalityDescription, organisationName, specificRequirements);
    }
  }
}

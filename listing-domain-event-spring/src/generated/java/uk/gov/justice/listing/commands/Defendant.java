package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.listing.events.Offence;

public class Defendant implements Serializable {
  private static final long serialVersionUID = -2043622465618083810L;

  private final Address address;

  private final BailStatus bailStatus;

  private final ZonedDateTime courtProceedingsInitiated;

  private final String custodyTimeLimit;

  private final String dateOfBirth;

  private final String datesToAvoid;

  private final String defenceOrganisation;

  private final String firstName;

  private final HearingLanguage hearingLanguageNeeds;

  private final UUID id;

  private final Boolean isYouth;

  private final String lastName;

  private final UUID masterDefendantId;

  private final String nationalityDescription;

  private final List<Offence> offences;

  private final String organisationName;

  private final String specificRequirements;

  @JsonCreator
  public Defendant(final Address address, final BailStatus bailStatus, final ZonedDateTime courtProceedingsInitiated, final String custodyTimeLimit, final String dateOfBirth, final String datesToAvoid, final String defenceOrganisation, final String firstName, final HearingLanguage hearingLanguageNeeds, final UUID id, final Boolean isYouth, final String lastName, final UUID masterDefendantId, final String nationalityDescription, final List<Offence> offences, final String organisationName, final String specificRequirements) {
    this.address = address;
    this.bailStatus = bailStatus;
    this.courtProceedingsInitiated = courtProceedingsInitiated;
    this.custodyTimeLimit = custodyTimeLimit;
    this.dateOfBirth = dateOfBirth;
    this.datesToAvoid = datesToAvoid;
    this.defenceOrganisation = defenceOrganisation;
    this.firstName = firstName;
    this.hearingLanguageNeeds = hearingLanguageNeeds;
    this.id = id;
    this.isYouth = isYouth;
    this.lastName = lastName;
    this.masterDefendantId = masterDefendantId;
    this.nationalityDescription = nationalityDescription;
    this.offences = offences;
    this.organisationName = organisationName;
    this.specificRequirements = specificRequirements;
  }

  public Address getAddress() {
    return address;
  }

  public BailStatus getBailStatus() {
    return bailStatus;
  }

  public ZonedDateTime getCourtProceedingsInitiated() {
    return courtProceedingsInitiated;
  }

  public String getCustodyTimeLimit() {
    return custodyTimeLimit;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public String getDatesToAvoid() {
    return datesToAvoid;
  }

  public String getDefenceOrganisation() {
    return defenceOrganisation;
  }

  public String getFirstName() {
    return firstName;
  }

  public HearingLanguage getHearingLanguageNeeds() {
    return hearingLanguageNeeds;
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

  public List<Offence> getOffences() {
    return offences;
  }

  public String getOrganisationName() {
    return organisationName;
  }

  public String getSpecificRequirements() {
    return specificRequirements;
  }

  public static Builder defendant() {
    return new uk.gov.justice.listing.commands.Defendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.Defendant that = (uk.gov.justice.listing.commands.Defendant) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.bailStatus, that.bailStatus) &&
    java.util.Objects.equals(this.courtProceedingsInitiated, that.courtProceedingsInitiated) &&
    java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
    java.util.Objects.equals(this.dateOfBirth, that.dateOfBirth) &&
    java.util.Objects.equals(this.datesToAvoid, that.datesToAvoid) &&
    java.util.Objects.equals(this.defenceOrganisation, that.defenceOrganisation) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isYouth, that.isYouth) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.nationalityDescription, that.nationalityDescription) &&
    java.util.Objects.equals(this.offences, that.offences) &&
    java.util.Objects.equals(this.organisationName, that.organisationName) &&
    java.util.Objects.equals(this.specificRequirements, that.specificRequirements);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, bailStatus, courtProceedingsInitiated, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, isYouth, lastName, masterDefendantId, nationalityDescription, offences, organisationName, specificRequirements);}

  public static class Builder {
    private Address address;

    private BailStatus bailStatus;

    private ZonedDateTime courtProceedingsInitiated;

    private String custodyTimeLimit;

    private String dateOfBirth;

    private String datesToAvoid;

    private String defenceOrganisation;

    private String firstName;

    private HearingLanguage hearingLanguageNeeds;

    private UUID id;

    private Boolean isYouth;

    private String lastName;

    private UUID masterDefendantId;

    private String nationalityDescription;

    private List<Offence> offences;

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

    public Builder withCourtProceedingsInitiated(final ZonedDateTime courtProceedingsInitiated) {
      this.courtProceedingsInitiated = courtProceedingsInitiated;
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

    public Builder withDatesToAvoid(final String datesToAvoid) {
      this.datesToAvoid = datesToAvoid;
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

    public Builder withHearingLanguageNeeds(final HearingLanguage hearingLanguageNeeds) {
      this.hearingLanguageNeeds = hearingLanguageNeeds;
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

    public Builder withOffences(final List<Offence> offences) {
      this.offences = offences;
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

    public Builder withValuesFrom(final Defendant defendant) {
      this.address = defendant.getAddress();
      this.bailStatus = defendant.getBailStatus();
      this.courtProceedingsInitiated = defendant.getCourtProceedingsInitiated();
      this.custodyTimeLimit = defendant.getCustodyTimeLimit();
      this.dateOfBirth = defendant.getDateOfBirth();
      this.datesToAvoid = defendant.getDatesToAvoid();
      this.defenceOrganisation = defendant.getDefenceOrganisation();
      this.firstName = defendant.getFirstName();
      this.hearingLanguageNeeds = defendant.getHearingLanguageNeeds();
      this.id = defendant.getId();
      this.isYouth = defendant.getIsYouth();
      this.lastName = defendant.getLastName();
      this.masterDefendantId = defendant.getMasterDefendantId();
      this.nationalityDescription = defendant.getNationalityDescription();
      this.offences = defendant.getOffences();
      this.organisationName = defendant.getOrganisationName();
      this.specificRequirements = defendant.getSpecificRequirements();
      return this;
    }

    public Defendant build() {
      return new uk.gov.justice.listing.commands.Defendant(address, bailStatus, courtProceedingsInitiated, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, isYouth, lastName, masterDefendantId, nationalityDescription, offences, organisationName, specificRequirements);
    }
  }
}

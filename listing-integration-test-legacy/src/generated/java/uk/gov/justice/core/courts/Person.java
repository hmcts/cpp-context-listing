package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class Person implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String additionalNationalityCode;

  private final String additionalNationalityDescription;

  private final UUID additionalNationalityId;

  private final Address address;

  private final ContactNumber contact;

  private final String dateOfBirth;

  private final String disabilityStatus;

  private final HearingLanguage documentationLanguageNeeds;

  private final Ethnicity ethnicity;

  private final String firstName;

  private final Gender gender;

  private final HearingLanguage hearingLanguageNeeds;

  private final String interpreterLanguageNeeds;

  private final Boolean isAddressConfidential;

  private final String lastName;

  private final String middleName;

  private final String nationalInsuranceNumber;

  private final String nationalityCode;

  private final String nationalityDescription;

  private final UUID nationalityId;

  private final String occupation;

  private final String occupationCode;

  private final List<Marker> personMarkers;

  private final String specificRequirements;

  private final String title;

  @JsonCreator
  public Person(final String additionalNationalityCode, final String additionalNationalityDescription, final UUID additionalNationalityId, final Address address, final ContactNumber contact, final String dateOfBirth, final String disabilityStatus, final HearingLanguage documentationLanguageNeeds, final Ethnicity ethnicity, final String firstName, final Gender gender, final HearingLanguage hearingLanguageNeeds, final String interpreterLanguageNeeds, final Boolean isAddressConfidential, final String lastName, final String middleName, final String nationalInsuranceNumber, final String nationalityCode, final String nationalityDescription, final UUID nationalityId, final String occupation, final String occupationCode, final List<Marker> personMarkers, final String specificRequirements, final String title) {
    this.additionalNationalityCode = additionalNationalityCode;
    this.additionalNationalityDescription = additionalNationalityDescription;
    this.additionalNationalityId = additionalNationalityId;
    this.address = address;
    this.contact = contact;
    this.dateOfBirth = dateOfBirth;
    this.disabilityStatus = disabilityStatus;
    this.documentationLanguageNeeds = documentationLanguageNeeds;
    this.ethnicity = ethnicity;
    this.firstName = firstName;
    this.gender = gender;
    this.hearingLanguageNeeds = hearingLanguageNeeds;
    this.interpreterLanguageNeeds = interpreterLanguageNeeds;
    this.isAddressConfidential = isAddressConfidential;
    this.lastName = lastName;
    this.middleName = middleName;
    this.nationalInsuranceNumber = nationalInsuranceNumber;
    this.nationalityCode = nationalityCode;
    this.nationalityDescription = nationalityDescription;
    this.nationalityId = nationalityId;
    this.occupation = occupation;
    this.occupationCode = occupationCode;
    this.personMarkers = personMarkers;
    this.specificRequirements = specificRequirements;
    this.title = title;
  }

  public String getAdditionalNationalityCode() {
    return additionalNationalityCode;
  }

  public String getAdditionalNationalityDescription() {
    return additionalNationalityDescription;
  }

  public UUID getAdditionalNationalityId() {
    return additionalNationalityId;
  }

  public Address getAddress() {
    return address;
  }

  public ContactNumber getContact() {
    return contact;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public String getDisabilityStatus() {
    return disabilityStatus;
  }

  public HearingLanguage getDocumentationLanguageNeeds() {
    return documentationLanguageNeeds;
  }

  public Ethnicity getEthnicity() {
    return ethnicity;
  }

  public String getFirstName() {
    return firstName;
  }

  public Gender getGender() {
    return gender;
  }

  public HearingLanguage getHearingLanguageNeeds() {
    return hearingLanguageNeeds;
  }

  public String getInterpreterLanguageNeeds() {
    return interpreterLanguageNeeds;
  }

  public Boolean getIsAddressConfidential() {
    return isAddressConfidential;
  }

  public String getLastName() {
    return lastName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public String getNationalInsuranceNumber() {
    return nationalInsuranceNumber;
  }

  public String getNationalityCode() {
    return nationalityCode;
  }

  public String getNationalityDescription() {
    return nationalityDescription;
  }

  public UUID getNationalityId() {
    return nationalityId;
  }

  public String getOccupation() {
    return occupation;
  }

  public String getOccupationCode() {
    return occupationCode;
  }

  public List<Marker> getPersonMarkers() {
    return personMarkers;
  }

  public String getSpecificRequirements() {
    return specificRequirements;
  }

  public String getTitle() {
    return title;
  }

  public static Builder person() {
    return new uk.gov.justice.core.courts.Person.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Person that = (uk.gov.justice.core.courts.Person) obj;

    return java.util.Objects.equals(this.additionalNationalityCode, that.additionalNationalityCode) &&
    java.util.Objects.equals(this.additionalNationalityDescription, that.additionalNationalityDescription) &&
    java.util.Objects.equals(this.additionalNationalityId, that.additionalNationalityId) &&
    java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.contact, that.contact) &&
    java.util.Objects.equals(this.dateOfBirth, that.dateOfBirth) &&
    java.util.Objects.equals(this.disabilityStatus, that.disabilityStatus) &&
    java.util.Objects.equals(this.documentationLanguageNeeds, that.documentationLanguageNeeds) &&
    java.util.Objects.equals(this.ethnicity, that.ethnicity) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.gender, that.gender) &&
    java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds) &&
    java.util.Objects.equals(this.interpreterLanguageNeeds, that.interpreterLanguageNeeds) &&
    java.util.Objects.equals(this.isAddressConfidential, that.isAddressConfidential) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.middleName, that.middleName) &&
    java.util.Objects.equals(this.nationalInsuranceNumber, that.nationalInsuranceNumber) &&
    java.util.Objects.equals(this.nationalityCode, that.nationalityCode) &&
    java.util.Objects.equals(this.nationalityDescription, that.nationalityDescription) &&
    java.util.Objects.equals(this.nationalityId, that.nationalityId) &&
    java.util.Objects.equals(this.occupation, that.occupation) &&
    java.util.Objects.equals(this.occupationCode, that.occupationCode) &&
    java.util.Objects.equals(this.personMarkers, that.personMarkers) &&
    java.util.Objects.equals(this.specificRequirements, that.specificRequirements) &&
    java.util.Objects.equals(this.title, that.title);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(additionalNationalityCode, additionalNationalityDescription, additionalNationalityId, address, contact, dateOfBirth, disabilityStatus, documentationLanguageNeeds, ethnicity, firstName, gender, hearingLanguageNeeds, interpreterLanguageNeeds, isAddressConfidential, lastName, middleName, nationalInsuranceNumber, nationalityCode, nationalityDescription, nationalityId, occupation, occupationCode, personMarkers, specificRequirements, title);}

  public static class Builder {
    private String additionalNationalityCode;

    private String additionalNationalityDescription;

    private UUID additionalNationalityId;

    private Address address;

    private ContactNumber contact;

    private String dateOfBirth;

    private String disabilityStatus;

    private HearingLanguage documentationLanguageNeeds;

    private Ethnicity ethnicity;

    private String firstName;

    private Gender gender;

    private HearingLanguage hearingLanguageNeeds;

    private String interpreterLanguageNeeds;

    private Boolean isAddressConfidential;

    private String lastName;

    private String middleName;

    private String nationalInsuranceNumber;

    private String nationalityCode;

    private String nationalityDescription;

    private UUID nationalityId;

    private String occupation;

    private String occupationCode;

    private List<Marker> personMarkers;

    private String specificRequirements;

    private String title;

    public Builder withAdditionalNationalityCode(final String additionalNationalityCode) {
      this.additionalNationalityCode = additionalNationalityCode;
      return this;
    }

    public Builder withAdditionalNationalityDescription(final String additionalNationalityDescription) {
      this.additionalNationalityDescription = additionalNationalityDescription;
      return this;
    }

    public Builder withAdditionalNationalityId(final UUID additionalNationalityId) {
      this.additionalNationalityId = additionalNationalityId;
      return this;
    }

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withContact(final ContactNumber contact) {
      this.contact = contact;
      return this;
    }

    public Builder withDateOfBirth(final String dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public Builder withDisabilityStatus(final String disabilityStatus) {
      this.disabilityStatus = disabilityStatus;
      return this;
    }

    public Builder withDocumentationLanguageNeeds(final HearingLanguage documentationLanguageNeeds) {
      this.documentationLanguageNeeds = documentationLanguageNeeds;
      return this;
    }

    public Builder withEthnicity(final Ethnicity ethnicity) {
      this.ethnicity = ethnicity;
      return this;
    }

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withGender(final Gender gender) {
      this.gender = gender;
      return this;
    }

    public Builder withHearingLanguageNeeds(final HearingLanguage hearingLanguageNeeds) {
      this.hearingLanguageNeeds = hearingLanguageNeeds;
      return this;
    }

    public Builder withInterpreterLanguageNeeds(final String interpreterLanguageNeeds) {
      this.interpreterLanguageNeeds = interpreterLanguageNeeds;
      return this;
    }

    public Builder withIsAddressConfidential(final Boolean isAddressConfidential) {
      this.isAddressConfidential = isAddressConfidential;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withMiddleName(final String middleName) {
      this.middleName = middleName;
      return this;
    }

    public Builder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
      this.nationalInsuranceNumber = nationalInsuranceNumber;
      return this;
    }

    public Builder withNationalityCode(final String nationalityCode) {
      this.nationalityCode = nationalityCode;
      return this;
    }

    public Builder withNationalityDescription(final String nationalityDescription) {
      this.nationalityDescription = nationalityDescription;
      return this;
    }

    public Builder withNationalityId(final UUID nationalityId) {
      this.nationalityId = nationalityId;
      return this;
    }

    public Builder withOccupation(final String occupation) {
      this.occupation = occupation;
      return this;
    }

    public Builder withOccupationCode(final String occupationCode) {
      this.occupationCode = occupationCode;
      return this;
    }

    public Builder withPersonMarkers(final List<Marker> personMarkers) {
      this.personMarkers = personMarkers;
      return this;
    }

    public Builder withSpecificRequirements(final String specificRequirements) {
      this.specificRequirements = specificRequirements;
      return this;
    }

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withValuesFrom(final Person person) {
      this.additionalNationalityCode = person.getAdditionalNationalityCode();
      this.additionalNationalityDescription = person.getAdditionalNationalityDescription();
      this.additionalNationalityId = person.getAdditionalNationalityId();
      this.address = person.getAddress();
      this.contact = person.getContact();
      this.dateOfBirth = person.getDateOfBirth();
      this.disabilityStatus = person.getDisabilityStatus();
      this.documentationLanguageNeeds = person.getDocumentationLanguageNeeds();
      this.ethnicity = person.getEthnicity();
      this.firstName = person.getFirstName();
      this.gender = person.getGender();
      this.hearingLanguageNeeds = person.getHearingLanguageNeeds();
      this.interpreterLanguageNeeds = person.getInterpreterLanguageNeeds();
      this.isAddressConfidential = person.getIsAddressConfidential();
      this.lastName = person.getLastName();
      this.middleName = person.getMiddleName();
      this.nationalInsuranceNumber = person.getNationalInsuranceNumber();
      this.nationalityCode = person.getNationalityCode();
      this.nationalityDescription = person.getNationalityDescription();
      this.nationalityId = person.getNationalityId();
      this.occupation = person.getOccupation();
      this.occupationCode = person.getOccupationCode();
      this.personMarkers = person.getPersonMarkers();
      this.specificRequirements = person.getSpecificRequirements();
      this.title = person.getTitle();
      return this;
    }

    public Person build() {
      return new uk.gov.justice.core.courts.Person(additionalNationalityCode, additionalNationalityDescription, additionalNationalityId, address, contact, dateOfBirth, disabilityStatus, documentationLanguageNeeds, ethnicity, firstName, gender, hearingLanguageNeeds, interpreterLanguageNeeds, isAddressConfidential, lastName, middleName, nationalInsuranceNumber, nationalityCode, nationalityDescription, nationalityId, occupation, occupationCode, personMarkers, specificRequirements, title);
    }
  }
}

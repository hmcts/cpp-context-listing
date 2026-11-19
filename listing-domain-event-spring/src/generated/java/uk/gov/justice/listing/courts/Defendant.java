package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PersonDefendant;

public class Defendant implements Serializable {
  private static final long serialVersionUID = -9095141849659270350L;

  private final Address address;

  private final List<DefendantAlias> aliases;

  private final AssociatedDefenceOrganisation associatedDefenceOrganisation;

  private final List<AssociatedPerson> associatedPersons;

  private final Organisation defenceOrganisation;

  private final UUID id;

  private final Boolean isYouth;

  private final String legalAidStatus;

  private final LegalEntityDefendant legalEntityDefendant;

  private final UUID masterDefendantId;

  private final String mitigation;

  private final String mitigationWelsh;

  private final String nationalityDescription;

  private final Integer numberOfPreviousConvictionsCited;

  private final PersonDefendant personDefendant;

  private final String pncId;

  private final Boolean proceedingsConcluded;

  private final String prosecutionAuthorityReference;

  private final UUID prosecutionCaseId;

  private final String witnessStatement;

  private final String witnessStatementWelsh;

  @JsonCreator
  public Defendant(final Address address, final List<DefendantAlias> aliases, final AssociatedDefenceOrganisation associatedDefenceOrganisation, final List<AssociatedPerson> associatedPersons, final Organisation defenceOrganisation, final UUID id, final Boolean isYouth, final String legalAidStatus, final LegalEntityDefendant legalEntityDefendant, final UUID masterDefendantId, final String mitigation, final String mitigationWelsh, final String nationalityDescription, final Integer numberOfPreviousConvictionsCited, final PersonDefendant personDefendant, final String pncId, final Boolean proceedingsConcluded, final String prosecutionAuthorityReference, final UUID prosecutionCaseId, final String witnessStatement, final String witnessStatementWelsh) {
    this.address = address;
    this.aliases = aliases;
    this.associatedDefenceOrganisation = associatedDefenceOrganisation;
    this.associatedPersons = associatedPersons;
    this.defenceOrganisation = defenceOrganisation;
    this.id = id;
    this.isYouth = isYouth;
    this.legalAidStatus = legalAidStatus;
    this.legalEntityDefendant = legalEntityDefendant;
    this.masterDefendantId = masterDefendantId;
    this.mitigation = mitigation;
    this.mitigationWelsh = mitigationWelsh;
    this.nationalityDescription = nationalityDescription;
    this.numberOfPreviousConvictionsCited = numberOfPreviousConvictionsCited;
    this.personDefendant = personDefendant;
    this.pncId = pncId;
    this.proceedingsConcluded = proceedingsConcluded;
    this.prosecutionAuthorityReference = prosecutionAuthorityReference;
    this.prosecutionCaseId = prosecutionCaseId;
    this.witnessStatement = witnessStatement;
    this.witnessStatementWelsh = witnessStatementWelsh;
  }

  public Address getAddress() {
    return address;
  }

  public List<DefendantAlias> getAliases() {
    return aliases;
  }

  public AssociatedDefenceOrganisation getAssociatedDefenceOrganisation() {
    return associatedDefenceOrganisation;
  }

  public List<AssociatedPerson> getAssociatedPersons() {
    return associatedPersons;
  }

  public Organisation getDefenceOrganisation() {
    return defenceOrganisation;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsYouth() {
    return isYouth;
  }

  public String getLegalAidStatus() {
    return legalAidStatus;
  }

  public LegalEntityDefendant getLegalEntityDefendant() {
    return legalEntityDefendant;
  }

  public UUID getMasterDefendantId() {
    return masterDefendantId;
  }

  public String getMitigation() {
    return mitigation;
  }

  public String getMitigationWelsh() {
    return mitigationWelsh;
  }

  public String getNationalityDescription() {
    return nationalityDescription;
  }

  public Integer getNumberOfPreviousConvictionsCited() {
    return numberOfPreviousConvictionsCited;
  }

  public PersonDefendant getPersonDefendant() {
    return personDefendant;
  }

  public String getPncId() {
    return pncId;
  }

  public Boolean getProceedingsConcluded() {
    return proceedingsConcluded;
  }

  public String getProsecutionAuthorityReference() {
    return prosecutionAuthorityReference;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public String getWitnessStatement() {
    return witnessStatement;
  }

  public String getWitnessStatementWelsh() {
    return witnessStatementWelsh;
  }

  public static Builder defendant() {
    return new uk.gov.justice.listing.courts.Defendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Defendant that = (uk.gov.justice.listing.courts.Defendant) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.aliases, that.aliases) &&
    java.util.Objects.equals(this.associatedDefenceOrganisation, that.associatedDefenceOrganisation) &&
    java.util.Objects.equals(this.associatedPersons, that.associatedPersons) &&
    java.util.Objects.equals(this.defenceOrganisation, that.defenceOrganisation) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isYouth, that.isYouth) &&
    java.util.Objects.equals(this.legalAidStatus, that.legalAidStatus) &&
    java.util.Objects.equals(this.legalEntityDefendant, that.legalEntityDefendant) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.mitigation, that.mitigation) &&
    java.util.Objects.equals(this.mitigationWelsh, that.mitigationWelsh) &&
    java.util.Objects.equals(this.nationalityDescription, that.nationalityDescription) &&
    java.util.Objects.equals(this.numberOfPreviousConvictionsCited, that.numberOfPreviousConvictionsCited) &&
    java.util.Objects.equals(this.personDefendant, that.personDefendant) &&
    java.util.Objects.equals(this.pncId, that.pncId) &&
    java.util.Objects.equals(this.proceedingsConcluded, that.proceedingsConcluded) &&
    java.util.Objects.equals(this.prosecutionAuthorityReference, that.prosecutionAuthorityReference) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.witnessStatement, that.witnessStatement) &&
    java.util.Objects.equals(this.witnessStatementWelsh, that.witnessStatementWelsh);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, aliases, associatedDefenceOrganisation, associatedPersons, defenceOrganisation, id, isYouth, legalAidStatus, legalEntityDefendant, masterDefendantId, mitigation, mitigationWelsh, nationalityDescription, numberOfPreviousConvictionsCited, personDefendant, pncId, proceedingsConcluded, prosecutionAuthorityReference, prosecutionCaseId, witnessStatement, witnessStatementWelsh);}

  public static class Builder {
    private Address address;

    private List<DefendantAlias> aliases;

    private AssociatedDefenceOrganisation associatedDefenceOrganisation;

    private List<AssociatedPerson> associatedPersons;

    private Organisation defenceOrganisation;

    private UUID id;

    private Boolean isYouth;

    private String legalAidStatus;

    private LegalEntityDefendant legalEntityDefendant;

    private UUID masterDefendantId;

    private String mitigation;

    private String mitigationWelsh;

    private String nationalityDescription;

    private Integer numberOfPreviousConvictionsCited;

    private PersonDefendant personDefendant;

    private String pncId;

    private Boolean proceedingsConcluded;

    private String prosecutionAuthorityReference;

    private UUID prosecutionCaseId;

    private String witnessStatement;

    private String witnessStatementWelsh;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withAliases(final List<DefendantAlias> aliases) {
      this.aliases = aliases;
      return this;
    }

    public Builder withAssociatedDefenceOrganisation(final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
      this.associatedDefenceOrganisation = associatedDefenceOrganisation;
      return this;
    }

    public Builder withAssociatedPersons(final List<AssociatedPerson> associatedPersons) {
      this.associatedPersons = associatedPersons;
      return this;
    }

    public Builder withDefenceOrganisation(final Organisation defenceOrganisation) {
      this.defenceOrganisation = defenceOrganisation;
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

    public Builder withLegalAidStatus(final String legalAidStatus) {
      this.legalAidStatus = legalAidStatus;
      return this;
    }

    public Builder withLegalEntityDefendant(final LegalEntityDefendant legalEntityDefendant) {
      this.legalEntityDefendant = legalEntityDefendant;
      return this;
    }

    public Builder withMasterDefendantId(final UUID masterDefendantId) {
      this.masterDefendantId = masterDefendantId;
      return this;
    }

    public Builder withMitigation(final String mitigation) {
      this.mitigation = mitigation;
      return this;
    }

    public Builder withMitigationWelsh(final String mitigationWelsh) {
      this.mitigationWelsh = mitigationWelsh;
      return this;
    }

    public Builder withNationalityDescription(final String nationalityDescription) {
      this.nationalityDescription = nationalityDescription;
      return this;
    }

    public Builder withNumberOfPreviousConvictionsCited(final Integer numberOfPreviousConvictionsCited) {
      this.numberOfPreviousConvictionsCited = numberOfPreviousConvictionsCited;
      return this;
    }

    public Builder withPersonDefendant(final PersonDefendant personDefendant) {
      this.personDefendant = personDefendant;
      return this;
    }

    public Builder withPncId(final String pncId) {
      this.pncId = pncId;
      return this;
    }

    public Builder withProceedingsConcluded(final Boolean proceedingsConcluded) {
      this.proceedingsConcluded = proceedingsConcluded;
      return this;
    }

    public Builder withProsecutionAuthorityReference(final String prosecutionAuthorityReference) {
      this.prosecutionAuthorityReference = prosecutionAuthorityReference;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withWitnessStatement(final String witnessStatement) {
      this.witnessStatement = witnessStatement;
      return this;
    }

    public Builder withWitnessStatementWelsh(final String witnessStatementWelsh) {
      this.witnessStatementWelsh = witnessStatementWelsh;
      return this;
    }

    public Builder withValuesFrom(final Defendant defendant) {
      this.address = defendant.getAddress();
      this.aliases = defendant.getAliases();
      this.associatedDefenceOrganisation = defendant.getAssociatedDefenceOrganisation();
      this.associatedPersons = defendant.getAssociatedPersons();
      this.defenceOrganisation = defendant.getDefenceOrganisation();
      this.id = defendant.getId();
      this.isYouth = defendant.getIsYouth();
      this.legalAidStatus = defendant.getLegalAidStatus();
      this.legalEntityDefendant = defendant.getLegalEntityDefendant();
      this.masterDefendantId = defendant.getMasterDefendantId();
      this.mitigation = defendant.getMitigation();
      this.mitigationWelsh = defendant.getMitigationWelsh();
      this.nationalityDescription = defendant.getNationalityDescription();
      this.numberOfPreviousConvictionsCited = defendant.getNumberOfPreviousConvictionsCited();
      this.personDefendant = defendant.getPersonDefendant();
      this.pncId = defendant.getPncId();
      this.proceedingsConcluded = defendant.getProceedingsConcluded();
      this.prosecutionAuthorityReference = defendant.getProsecutionAuthorityReference();
      this.prosecutionCaseId = defendant.getProsecutionCaseId();
      this.witnessStatement = defendant.getWitnessStatement();
      this.witnessStatementWelsh = defendant.getWitnessStatementWelsh();
      return this;
    }

    public Defendant build() {
      return new uk.gov.justice.listing.courts.Defendant(address, aliases, associatedDefenceOrganisation, associatedPersons, defenceOrganisation, id, isYouth, legalAidStatus, legalEntityDefendant, masterDefendantId, mitigation, mitigationWelsh, nationalityDescription, numberOfPreviousConvictionsCited, personDefendant, pncId, proceedingsConcluded, prosecutionAuthorityReference, prosecutionCaseId, witnessStatement, witnessStatementWelsh);
    }
  }
}

package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PersonDefendant;

public class Defendant implements Serializable {
  private static final long serialVersionUID = -7781157060675043935L;

  private final List<AssociatedPerson> associatedPersons;

  private final ZonedDateTime courtProceedingsInitiated;

  private final Organisation defenceOrganisation;

  private final UUID id;

  private final UUID idX;

  private final LegalEntityDefendant legalEntityDefendant;

  private final UUID masterDefendantId;

  private final String mitigation;

  private final String mitigationWelsh;

  private final Integer numberOfPreviousConvictionsCited;

  private final PersonDefendant personDefendant;

  private final String prosecutionAuthorityReference;

  private final UUID prosecutionCaseId;

  private final String witnessStatement;

  private final String witnessStatementWelsh;

  @JsonCreator
  public Defendant(final List<AssociatedPerson> associatedPersons, final ZonedDateTime courtProceedingsInitiated, final Organisation defenceOrganisation, final UUID id, final UUID idX, final LegalEntityDefendant legalEntityDefendant, final UUID masterDefendantId, final String mitigation, final String mitigationWelsh, final Integer numberOfPreviousConvictionsCited, final PersonDefendant personDefendant, final String prosecutionAuthorityReference, final UUID prosecutionCaseId, final String witnessStatement, final String witnessStatementWelsh) {
    this.associatedPersons = associatedPersons;
    this.courtProceedingsInitiated = courtProceedingsInitiated;
    this.defenceOrganisation = defenceOrganisation;
    this.id = id;
    this.idX = idX;
    this.legalEntityDefendant = legalEntityDefendant;
    this.masterDefendantId = masterDefendantId;
    this.mitigation = mitigation;
    this.mitigationWelsh = mitigationWelsh;
    this.numberOfPreviousConvictionsCited = numberOfPreviousConvictionsCited;
    this.personDefendant = personDefendant;
    this.prosecutionAuthorityReference = prosecutionAuthorityReference;
    this.prosecutionCaseId = prosecutionCaseId;
    this.witnessStatement = witnessStatement;
    this.witnessStatementWelsh = witnessStatementWelsh;
  }

  public List<AssociatedPerson> getAssociatedPersons() {
    return associatedPersons;
  }

  public ZonedDateTime getCourtProceedingsInitiated() {
    return courtProceedingsInitiated;
  }

  public Organisation getDefenceOrganisation() {
    return defenceOrganisation;
  }

  public UUID getId() {
    return id;
  }

  public UUID getIdX() {
    return idX;
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

  public Integer getNumberOfPreviousConvictionsCited() {
    return numberOfPreviousConvictionsCited;
  }

  public PersonDefendant getPersonDefendant() {
    return personDefendant;
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
    return new uk.gov.justice.progression.courts.Defendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.Defendant that = (uk.gov.justice.progression.courts.Defendant) obj;

    return java.util.Objects.equals(this.associatedPersons, that.associatedPersons) &&
    java.util.Objects.equals(this.courtProceedingsInitiated, that.courtProceedingsInitiated) &&
    java.util.Objects.equals(this.defenceOrganisation, that.defenceOrganisation) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.idX, that.idX) &&
    java.util.Objects.equals(this.legalEntityDefendant, that.legalEntityDefendant) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.mitigation, that.mitigation) &&
    java.util.Objects.equals(this.mitigationWelsh, that.mitigationWelsh) &&
    java.util.Objects.equals(this.numberOfPreviousConvictionsCited, that.numberOfPreviousConvictionsCited) &&
    java.util.Objects.equals(this.personDefendant, that.personDefendant) &&
    java.util.Objects.equals(this.prosecutionAuthorityReference, that.prosecutionAuthorityReference) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.witnessStatement, that.witnessStatement) &&
    java.util.Objects.equals(this.witnessStatementWelsh, that.witnessStatementWelsh);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(associatedPersons, courtProceedingsInitiated, defenceOrganisation, id, idX, legalEntityDefendant, masterDefendantId, mitigation, mitigationWelsh, numberOfPreviousConvictionsCited, personDefendant, prosecutionAuthorityReference, prosecutionCaseId, witnessStatement, witnessStatementWelsh);}

  public static class Builder {
    private List<AssociatedPerson> associatedPersons;

    private ZonedDateTime courtProceedingsInitiated;

    private Organisation defenceOrganisation;

    private UUID id;

    private UUID idX;

    private LegalEntityDefendant legalEntityDefendant;

    private UUID masterDefendantId;

    private String mitigation;

    private String mitigationWelsh;

    private Integer numberOfPreviousConvictionsCited;

    private PersonDefendant personDefendant;

    private String prosecutionAuthorityReference;

    private UUID prosecutionCaseId;

    private String witnessStatement;

    private String witnessStatementWelsh;

    public Builder withAssociatedPersons(final List<AssociatedPerson> associatedPersons) {
      this.associatedPersons = associatedPersons;
      return this;
    }

    public Builder withCourtProceedingsInitiated(final ZonedDateTime courtProceedingsInitiated) {
      this.courtProceedingsInitiated = courtProceedingsInitiated;
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

    public Builder withIdX(final UUID idX) {
      this.idX = idX;
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

    public Builder withNumberOfPreviousConvictionsCited(final Integer numberOfPreviousConvictionsCited) {
      this.numberOfPreviousConvictionsCited = numberOfPreviousConvictionsCited;
      return this;
    }

    public Builder withPersonDefendant(final PersonDefendant personDefendant) {
      this.personDefendant = personDefendant;
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
      this.associatedPersons = defendant.getAssociatedPersons();
      this.courtProceedingsInitiated = defendant.getCourtProceedingsInitiated();
      this.defenceOrganisation = defendant.getDefenceOrganisation();
      this.id = defendant.getId();
      this.idX = defendant.getIdX();
      this.legalEntityDefendant = defendant.getLegalEntityDefendant();
      this.masterDefendantId = defendant.getMasterDefendantId();
      this.mitigation = defendant.getMitigation();
      this.mitigationWelsh = defendant.getMitigationWelsh();
      this.numberOfPreviousConvictionsCited = defendant.getNumberOfPreviousConvictionsCited();
      this.personDefendant = defendant.getPersonDefendant();
      this.prosecutionAuthorityReference = defendant.getProsecutionAuthorityReference();
      this.prosecutionCaseId = defendant.getProsecutionCaseId();
      this.witnessStatement = defendant.getWitnessStatement();
      this.witnessStatementWelsh = defendant.getWitnessStatementWelsh();
      return this;
    }

    public Defendant build() {
      return new uk.gov.justice.progression.courts.Defendant(associatedPersons, courtProceedingsInitiated, defenceOrganisation, id, idX, legalEntityDefendant, masterDefendantId, mitigation, mitigationWelsh, numberOfPreviousConvictionsCited, personDefendant, prosecutionAuthorityReference, prosecutionCaseId, witnessStatement, witnessStatementWelsh);
    }
  }
}

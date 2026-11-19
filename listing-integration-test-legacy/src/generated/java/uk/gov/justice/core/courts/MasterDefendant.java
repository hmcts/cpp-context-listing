package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class MasterDefendant implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final List<AssociatedPerson> associatedPersons;

  private final UUID cpsDefendantId;

  private final List<DefendantCase> defendantCase;

  private final Boolean isYouth;

  private final LegalEntityDefendant legalEntityDefendant;

  private final UUID masterDefendantId;

  private final PersonDefendant personDefendant;

  private final String pncId;

  private final String prosecutionAuthorityReference;

  @JsonCreator
  public MasterDefendant(final List<AssociatedPerson> associatedPersons, final UUID cpsDefendantId, final List<DefendantCase> defendantCase, final Boolean isYouth, final LegalEntityDefendant legalEntityDefendant, final UUID masterDefendantId, final PersonDefendant personDefendant, final String pncId, final String prosecutionAuthorityReference) {
    this.associatedPersons = associatedPersons;
    this.cpsDefendantId = cpsDefendantId;
    this.defendantCase = defendantCase;
    this.isYouth = isYouth;
    this.legalEntityDefendant = legalEntityDefendant;
    this.masterDefendantId = masterDefendantId;
    this.personDefendant = personDefendant;
    this.pncId = pncId;
    this.prosecutionAuthorityReference = prosecutionAuthorityReference;
  }

  public List<AssociatedPerson> getAssociatedPersons() {
    return associatedPersons;
  }

  public UUID getCpsDefendantId() {
    return cpsDefendantId;
  }

  public List<DefendantCase> getDefendantCase() {
    return defendantCase;
  }

  public Boolean getIsYouth() {
    return isYouth;
  }

  public LegalEntityDefendant getLegalEntityDefendant() {
    return legalEntityDefendant;
  }

  public UUID getMasterDefendantId() {
    return masterDefendantId;
  }

  public PersonDefendant getPersonDefendant() {
    return personDefendant;
  }

  public String getPncId() {
    return pncId;
  }

  public String getProsecutionAuthorityReference() {
    return prosecutionAuthorityReference;
  }

  public static Builder masterDefendant() {
    return new uk.gov.justice.core.courts.MasterDefendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.MasterDefendant that = (uk.gov.justice.core.courts.MasterDefendant) obj;

    return java.util.Objects.equals(this.associatedPersons, that.associatedPersons) &&
    java.util.Objects.equals(this.cpsDefendantId, that.cpsDefendantId) &&
    java.util.Objects.equals(this.defendantCase, that.defendantCase) &&
    java.util.Objects.equals(this.isYouth, that.isYouth) &&
    java.util.Objects.equals(this.legalEntityDefendant, that.legalEntityDefendant) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.personDefendant, that.personDefendant) &&
    java.util.Objects.equals(this.pncId, that.pncId) &&
    java.util.Objects.equals(this.prosecutionAuthorityReference, that.prosecutionAuthorityReference);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(associatedPersons, cpsDefendantId, defendantCase, isYouth, legalEntityDefendant, masterDefendantId, personDefendant, pncId, prosecutionAuthorityReference);}

  public static class Builder {
    private List<AssociatedPerson> associatedPersons;

    private UUID cpsDefendantId;

    private List<DefendantCase> defendantCase;

    private Boolean isYouth;

    private LegalEntityDefendant legalEntityDefendant;

    private UUID masterDefendantId;

    private PersonDefendant personDefendant;

    private String pncId;

    private String prosecutionAuthorityReference;

    public Builder withAssociatedPersons(final List<AssociatedPerson> associatedPersons) {
      this.associatedPersons = associatedPersons;
      return this;
    }

    public Builder withCpsDefendantId(final UUID cpsDefendantId) {
      this.cpsDefendantId = cpsDefendantId;
      return this;
    }

    public Builder withDefendantCase(final List<DefendantCase> defendantCase) {
      this.defendantCase = defendantCase;
      return this;
    }

    public Builder withIsYouth(final Boolean isYouth) {
      this.isYouth = isYouth;
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

    public Builder withPersonDefendant(final PersonDefendant personDefendant) {
      this.personDefendant = personDefendant;
      return this;
    }

    public Builder withPncId(final String pncId) {
      this.pncId = pncId;
      return this;
    }

    public Builder withProsecutionAuthorityReference(final String prosecutionAuthorityReference) {
      this.prosecutionAuthorityReference = prosecutionAuthorityReference;
      return this;
    }

    public Builder withValuesFrom(final MasterDefendant masterDefendant) {
      this.associatedPersons = masterDefendant.getAssociatedPersons();
      this.cpsDefendantId = masterDefendant.getCpsDefendantId();
      this.defendantCase = masterDefendant.getDefendantCase();
      this.isYouth = masterDefendant.getIsYouth();
      this.legalEntityDefendant = masterDefendant.getLegalEntityDefendant();
      this.masterDefendantId = masterDefendant.getMasterDefendantId();
      this.personDefendant = masterDefendant.getPersonDefendant();
      this.pncId = masterDefendant.getPncId();
      this.prosecutionAuthorityReference = masterDefendant.getProsecutionAuthorityReference();
      return this;
    }

    public MasterDefendant build() {
      return new uk.gov.justice.core.courts.MasterDefendant(associatedPersons, cpsDefendantId, defendantCase, isYouth, legalEntityDefendant, masterDefendantId, personDefendant, pncId, prosecutionAuthorityReference);
    }
  }
}

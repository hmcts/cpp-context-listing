package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class AssociatedDefenceOrganisation implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String applicationReference;

  private final String associationEndDate;

  private final String associationStartDate;

  private final DefenceOrganisation defenceOrganisation;

  private final FundingType fundingType;

  private final Boolean isAssociatedByLAA;

  @JsonCreator
  public AssociatedDefenceOrganisation(final String applicationReference, final String associationEndDate, final String associationStartDate, final DefenceOrganisation defenceOrganisation, final FundingType fundingType, final Boolean isAssociatedByLAA) {
    this.applicationReference = applicationReference;
    this.associationEndDate = associationEndDate;
    this.associationStartDate = associationStartDate;
    this.defenceOrganisation = defenceOrganisation;
    this.fundingType = fundingType;
    this.isAssociatedByLAA = isAssociatedByLAA;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public String getAssociationEndDate() {
    return associationEndDate;
  }

  public String getAssociationStartDate() {
    return associationStartDate;
  }

  public DefenceOrganisation getDefenceOrganisation() {
    return defenceOrganisation;
  }

  public FundingType getFundingType() {
    return fundingType;
  }

  public Boolean getIsAssociatedByLAA() {
    return isAssociatedByLAA;
  }

  public static Builder associatedDefenceOrganisation() {
    return new uk.gov.justice.core.courts.AssociatedDefenceOrganisation.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.AssociatedDefenceOrganisation that = (uk.gov.justice.core.courts.AssociatedDefenceOrganisation) obj;

    return java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
    java.util.Objects.equals(this.associationEndDate, that.associationEndDate) &&
    java.util.Objects.equals(this.associationStartDate, that.associationStartDate) &&
    java.util.Objects.equals(this.defenceOrganisation, that.defenceOrganisation) &&
    java.util.Objects.equals(this.fundingType, that.fundingType) &&
    java.util.Objects.equals(this.isAssociatedByLAA, that.isAssociatedByLAA);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationReference, associationEndDate, associationStartDate, defenceOrganisation, fundingType, isAssociatedByLAA);}

  public static class Builder {
    private String applicationReference;

    private String associationEndDate;

    private String associationStartDate;

    private DefenceOrganisation defenceOrganisation;

    private FundingType fundingType;

    private Boolean isAssociatedByLAA;

    public Builder withApplicationReference(final String applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public Builder withAssociationEndDate(final String associationEndDate) {
      this.associationEndDate = associationEndDate;
      return this;
    }

    public Builder withAssociationStartDate(final String associationStartDate) {
      this.associationStartDate = associationStartDate;
      return this;
    }

    public Builder withDefenceOrganisation(final DefenceOrganisation defenceOrganisation) {
      this.defenceOrganisation = defenceOrganisation;
      return this;
    }

    public Builder withFundingType(final FundingType fundingType) {
      this.fundingType = fundingType;
      return this;
    }

    public Builder withIsAssociatedByLAA(final Boolean isAssociatedByLAA) {
      this.isAssociatedByLAA = isAssociatedByLAA;
      return this;
    }

    public Builder withValuesFrom(final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
      this.applicationReference = associatedDefenceOrganisation.getApplicationReference();
      this.associationEndDate = associatedDefenceOrganisation.getAssociationEndDate();
      this.associationStartDate = associatedDefenceOrganisation.getAssociationStartDate();
      this.defenceOrganisation = associatedDefenceOrganisation.getDefenceOrganisation();
      this.fundingType = associatedDefenceOrganisation.getFundingType();
      this.isAssociatedByLAA = associatedDefenceOrganisation.getIsAssociatedByLAA();
      return this;
    }

    public AssociatedDefenceOrganisation build() {
      return new uk.gov.justice.core.courts.AssociatedDefenceOrganisation(applicationReference, associationEndDate, associationStartDate, defenceOrganisation, fundingType, isAssociatedByLAA);
    }
  }
}

package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class DefenceOrganisation implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String laaContractNumber;

  private final Organisation organisation;

  @JsonCreator
  public DefenceOrganisation(final String laaContractNumber, final Organisation organisation) {
    this.laaContractNumber = laaContractNumber;
    this.organisation = organisation;
  }

  public String getLaaContractNumber() {
    return laaContractNumber;
  }

  public Organisation getOrganisation() {
    return organisation;
  }

  public static Builder defenceOrganisation() {
    return new uk.gov.justice.core.courts.DefenceOrganisation.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefenceOrganisation that = (uk.gov.justice.core.courts.DefenceOrganisation) obj;

    return java.util.Objects.equals(this.laaContractNumber, that.laaContractNumber) &&
    java.util.Objects.equals(this.organisation, that.organisation);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(laaContractNumber, organisation);}

  public static class Builder {
    private String laaContractNumber;

    private Organisation organisation;

    public Builder withLaaContractNumber(final String laaContractNumber) {
      this.laaContractNumber = laaContractNumber;
      return this;
    }

    public Builder withOrganisation(final Organisation organisation) {
      this.organisation = organisation;
      return this;
    }

    public Builder withValuesFrom(final DefenceOrganisation defenceOrganisation) {
      this.laaContractNumber = defenceOrganisation.getLaaContractNumber();
      this.organisation = defenceOrganisation.getOrganisation();
      return this;
    }

    public DefenceOrganisation build() {
      return new uk.gov.justice.core.courts.DefenceOrganisation(laaContractNumber, organisation);
    }
  }
}

package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;

public class LegalEntityDefendant implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Organisation organisation;

  @JsonCreator
  public LegalEntityDefendant(final Organisation organisation) {
    this.organisation = organisation;
  }

  public Organisation getOrganisation() {
    return organisation;
  }

  public static Builder legalEntityDefendant() {
    return new uk.gov.justice.core.courts.LegalEntityDefendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.LegalEntityDefendant that = (uk.gov.justice.core.courts.LegalEntityDefendant) obj;

    return java.util.Objects.equals(this.organisation, that.organisation);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(organisation);}

  public static class Builder {
    private Organisation organisation;

    public Builder withOrganisation(final Organisation organisation) {
      this.organisation = organisation;
      return this;
    }

    public Builder withValuesFrom(final LegalEntityDefendant legalEntityDefendant) {
      this.organisation = legalEntityDefendant.getOrganisation();
      return this;
    }

    public LegalEntityDefendant build() {
      return new uk.gov.justice.core.courts.LegalEntityDefendant(organisation);
    }
  }
}

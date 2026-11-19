package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;

public class UpdateCaseResultedDefendantProceedingsConcluded implements Serializable {
  private static final long serialVersionUID = -4597053069936115249L;

  private final ProsecutionCase prosecutionCase;

  @JsonCreator
  public UpdateCaseResultedDefendantProceedingsConcluded(final ProsecutionCase prosecutionCase) {
    this.prosecutionCase = prosecutionCase;
  }

  public ProsecutionCase getProsecutionCase() {
    return prosecutionCase;
  }

  public static Builder updateCaseResultedDefendantProceedingsConcluded() {
    return new uk.gov.justice.core.courts.UpdateCaseResultedDefendantProceedingsConcluded.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.UpdateCaseResultedDefendantProceedingsConcluded that = (uk.gov.justice.core.courts.UpdateCaseResultedDefendantProceedingsConcluded) obj;

    return java.util.Objects.equals(this.prosecutionCase, that.prosecutionCase);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(prosecutionCase);}

  public static class Builder {
    private ProsecutionCase prosecutionCase;

    public Builder withProsecutionCase(final ProsecutionCase prosecutionCase) {
      this.prosecutionCase = prosecutionCase;
      return this;
    }

    public Builder withValuesFrom(final UpdateCaseResultedDefendantProceedingsConcluded updateCaseResultedDefendantProceedingsConcluded) {
      this.prosecutionCase = updateCaseResultedDefendantProceedingsConcluded.getProsecutionCase();
      return this;
    }

    public UpdateCaseResultedDefendantProceedingsConcluded build() {
      return new uk.gov.justice.core.courts.UpdateCaseResultedDefendantProceedingsConcluded(prosecutionCase);
    }
  }
}

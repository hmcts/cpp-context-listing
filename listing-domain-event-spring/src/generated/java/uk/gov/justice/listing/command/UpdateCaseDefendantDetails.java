package uk.gov.justice.listing.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;

public class UpdateCaseDefendantDetails implements Serializable {
  private static final long serialVersionUID = 8695591048529576562L;

  private final Defendant defendant;

  @JsonCreator
  public UpdateCaseDefendantDetails(final Defendant defendant) {
    this.defendant = defendant;
  }

  public Defendant getDefendant() {
    return defendant;
  }

  public static Builder updateCaseDefendantDetails() {
    return new uk.gov.justice.listing.command.UpdateCaseDefendantDetails.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.command.UpdateCaseDefendantDetails that = (uk.gov.justice.listing.command.UpdateCaseDefendantDetails) obj;

    return java.util.Objects.equals(this.defendant, that.defendant);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendant);}

  public static class Builder {
    private Defendant defendant;

    public Builder withDefendant(final Defendant defendant) {
      this.defendant = defendant;
      return this;
    }

    public Builder withValuesFrom(final UpdateCaseDefendantDetails updateCaseDefendantDetails) {
      this.defendant = updateCaseDefendantDetails.getDefendant();
      return this;
    }

    public UpdateCaseDefendantDetails build() {
      return new uk.gov.justice.listing.command.UpdateCaseDefendantDetails(defendant);
    }
  }
}

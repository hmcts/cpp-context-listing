package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.ProsecutionCase;

public class UpdateHearingAddCaseBdf implements Serializable {
  private static final long serialVersionUID = -4356520250727807527L;

  private final ProsecutionCase prosecutionCase;

  @JsonCreator
  public UpdateHearingAddCaseBdf(final ProsecutionCase prosecutionCase) {
    this.prosecutionCase = prosecutionCase;
  }

  public ProsecutionCase getProsecutionCase() {
    return prosecutionCase;
  }

  public static Builder updateHearingAddCaseBdf() {
    return new uk.gov.justice.listing.courts.UpdateHearingAddCaseBdf.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateHearingAddCaseBdf that = (uk.gov.justice.listing.courts.UpdateHearingAddCaseBdf) obj;

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

    public Builder withValuesFrom(final UpdateHearingAddCaseBdf updateHearingAddCaseBdf) {
      this.prosecutionCase = updateHearingAddCaseBdf.getProsecutionCase();
      return this;
    }

    public UpdateHearingAddCaseBdf build() {
      return new uk.gov.justice.listing.courts.UpdateHearingAddCaseBdf(prosecutionCase);
    }
  }
}

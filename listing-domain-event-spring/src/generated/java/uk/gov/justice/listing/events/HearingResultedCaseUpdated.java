package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.ProsecutionCase;

public class HearingResultedCaseUpdated implements Serializable {
  private static final long serialVersionUID = 2163427886178235487L;

  private final ProsecutionCase prosecutionCase;

  @JsonCreator
  public HearingResultedCaseUpdated(final ProsecutionCase prosecutionCase) {
    this.prosecutionCase = prosecutionCase;
  }

  public ProsecutionCase getProsecutionCase() {
    return prosecutionCase;
  }

  public static Builder hearingResultedCaseUpdated() {
    return new uk.gov.justice.listing.events.HearingResultedCaseUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingResultedCaseUpdated that = (uk.gov.justice.listing.events.HearingResultedCaseUpdated) obj;

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

    public Builder withValuesFrom(final HearingResultedCaseUpdated hearingResultedCaseUpdated) {
      this.prosecutionCase = hearingResultedCaseUpdated.getProsecutionCase();
      return this;
    }

    public HearingResultedCaseUpdated build() {
      return new uk.gov.justice.listing.events.HearingResultedCaseUpdated(prosecutionCase);
    }
  }
}

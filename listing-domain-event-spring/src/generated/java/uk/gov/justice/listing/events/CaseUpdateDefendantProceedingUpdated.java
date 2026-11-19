package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;

public class CaseUpdateDefendantProceedingUpdated implements Serializable {
  private static final long serialVersionUID = 7019241542547272329L;

  private final UUID hearingId;

  private final ProsecutionCase prosecutionCase;

  @JsonCreator
  public CaseUpdateDefendantProceedingUpdated(final UUID hearingId, final ProsecutionCase prosecutionCase) {
    this.hearingId = hearingId;
    this.prosecutionCase = prosecutionCase;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public ProsecutionCase getProsecutionCase() {
    return prosecutionCase;
  }

  public static Builder caseUpdateDefendantProceedingUpdated() {
    return new uk.gov.justice.listing.events.CaseUpdateDefendantProceedingUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseUpdateDefendantProceedingUpdated that = (uk.gov.justice.listing.events.CaseUpdateDefendantProceedingUpdated) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.prosecutionCase, that.prosecutionCase);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, prosecutionCase);}

  public static class Builder {
    private UUID hearingId;

    private ProsecutionCase prosecutionCase;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withProsecutionCase(final ProsecutionCase prosecutionCase) {
      this.prosecutionCase = prosecutionCase;
      return this;
    }

    public Builder withValuesFrom(final CaseUpdateDefendantProceedingUpdated caseUpdateDefendantProceedingUpdated) {
      this.hearingId = caseUpdateDefendantProceedingUpdated.getHearingId();
      this.prosecutionCase = caseUpdateDefendantProceedingUpdated.getProsecutionCase();
      return this;
    }

    public CaseUpdateDefendantProceedingUpdated build() {
      return new uk.gov.justice.listing.events.CaseUpdateDefendantProceedingUpdated(hearingId, prosecutionCase);
    }
  }
}

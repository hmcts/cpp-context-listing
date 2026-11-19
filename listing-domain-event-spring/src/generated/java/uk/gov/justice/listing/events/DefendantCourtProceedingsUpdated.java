package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class DefendantCourtProceedingsUpdated implements Serializable {
  private static final long serialVersionUID = -5738629687145407936L;

  private final UUID hearingId;

  private final ProsecutionCase prosecutionCase;

  @JsonCreator
  public DefendantCourtProceedingsUpdated(final UUID hearingId, final ProsecutionCase prosecutionCase) {
    this.hearingId = hearingId;
    this.prosecutionCase = prosecutionCase;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public ProsecutionCase getProsecutionCase() {
    return prosecutionCase;
  }

  public static Builder defendantCourtProceedingsUpdated() {
    return new uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated that = (uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated) obj;

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

    public Builder withValuesFrom(final DefendantCourtProceedingsUpdated defendantCourtProceedingsUpdated) {
      this.hearingId = defendantCourtProceedingsUpdated.getHearingId();
      this.prosecutionCase = defendantCourtProceedingsUpdated.getProsecutionCase();
      return this;
    }

    public DefendantCourtProceedingsUpdated build() {
      return new uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated(hearingId, prosecutionCase);
    }
  }
}

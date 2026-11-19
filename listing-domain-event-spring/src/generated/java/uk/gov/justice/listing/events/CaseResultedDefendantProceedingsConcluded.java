package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.case-resulted-defendant-proceedings-updated")
public class CaseResultedDefendantProceedingsConcluded implements Serializable {
  private static final long serialVersionUID = -6987042993769267871L;

  private final List<UUID> hearingIds;

  private final ProsecutionCase prosecutionCase;

  @JsonCreator
  public CaseResultedDefendantProceedingsConcluded(final List<UUID> hearingIds, final ProsecutionCase prosecutionCase) {
    this.hearingIds = hearingIds;
    this.prosecutionCase = prosecutionCase;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public ProsecutionCase getProsecutionCase() {
    return prosecutionCase;
  }

  public static Builder caseResultedDefendantProceedingsConcluded() {
    return new uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded that = (uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded) obj;

    return java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.prosecutionCase, that.prosecutionCase);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIds, prosecutionCase);}

  public static class Builder {
    private List<UUID> hearingIds;

    private ProsecutionCase prosecutionCase;

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
      return this;
    }

    public Builder withProsecutionCase(final ProsecutionCase prosecutionCase) {
      this.prosecutionCase = prosecutionCase;
      return this;
    }

    public Builder withValuesFrom(final CaseResultedDefendantProceedingsConcluded caseResultedDefendantProceedingsConcluded) {
      this.hearingIds = caseResultedDefendantProceedingsConcluded.getHearingIds();
      this.prosecutionCase = caseResultedDefendantProceedingsConcluded.getProsecutionCase();
      return this;
    }

    public CaseResultedDefendantProceedingsConcluded build() {
      return new uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded(hearingIds, prosecutionCase);
    }
  }
}

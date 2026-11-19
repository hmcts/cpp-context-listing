package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class ProsecutionCasesToRemove implements Serializable {
  private static final long serialVersionUID = -6227122385704246944L;

  private final UUID caseId;

  private final List<DefendantsToRemove> defendantsToRemove;

  @JsonCreator
  public ProsecutionCasesToRemove(final UUID caseId, final List<DefendantsToRemove> defendantsToRemove) {
    this.caseId = caseId;
    this.defendantsToRemove = defendantsToRemove;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<DefendantsToRemove> getDefendantsToRemove() {
    return defendantsToRemove;
  }

  public static Builder prosecutionCasesToRemove() {
    return new uk.gov.justice.listing.courts.ProsecutionCasesToRemove.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ProsecutionCasesToRemove that = (uk.gov.justice.listing.courts.ProsecutionCasesToRemove) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantsToRemove, that.defendantsToRemove);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantsToRemove);}

  public static class Builder {
    private UUID caseId;

    private List<DefendantsToRemove> defendantsToRemove;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendantsToRemove(final List<DefendantsToRemove> defendantsToRemove) {
      this.defendantsToRemove = defendantsToRemove;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCasesToRemove prosecutionCasesToRemove) {
      this.caseId = prosecutionCasesToRemove.getCaseId();
      this.defendantsToRemove = prosecutionCasesToRemove.getDefendantsToRemove();
      return this;
    }

    public ProsecutionCasesToRemove build() {
      return new uk.gov.justice.listing.courts.ProsecutionCasesToRemove(caseId, defendantsToRemove);
    }
  }
}

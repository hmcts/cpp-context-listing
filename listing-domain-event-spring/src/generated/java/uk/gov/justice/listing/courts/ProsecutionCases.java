package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class ProsecutionCases implements Serializable {
  private static final long serialVersionUID = 7919779722927081093L;

  private final UUID caseId;

  private final List<Defendants> defendants;

  @JsonCreator
  public ProsecutionCases(final UUID caseId, final List<Defendants> defendants) {
    this.caseId = caseId;
    this.defendants = defendants;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<Defendants> getDefendants() {
    return defendants;
  }

  public static Builder prosecutionCases() {
    return new uk.gov.justice.listing.courts.ProsecutionCases.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ProsecutionCases that = (uk.gov.justice.listing.courts.ProsecutionCases) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendants, that.defendants);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendants);}

  public static class Builder {
    private UUID caseId;

    private List<Defendants> defendants;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendants(final List<Defendants> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCases prosecutionCases) {
      this.caseId = prosecutionCases.getCaseId();
      this.defendants = prosecutionCases.getDefendants();
      return this;
    }

    public ProsecutionCases build() {
      return new uk.gov.justice.listing.courts.ProsecutionCases(caseId, defendants);
    }
  }
}

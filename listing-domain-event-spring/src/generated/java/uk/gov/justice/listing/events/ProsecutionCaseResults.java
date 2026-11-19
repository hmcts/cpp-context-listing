package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.JudicialResult;

public class ProsecutionCaseResults implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final UUID defendantId;

  private final JudicialResult judicialResult;

  private final UUID offenceId;

  @JsonCreator
  public ProsecutionCaseResults(final UUID defendantId, final JudicialResult judicialResult, final UUID offenceId) {
    this.defendantId = defendantId;
    this.judicialResult = judicialResult;
    this.offenceId = offenceId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public JudicialResult getJudicialResult() {
    return judicialResult;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public static Builder prosecutionCaseResults() {
    return new uk.gov.justice.listing.events.ProsecutionCaseResults.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ProsecutionCaseResults that = (uk.gov.justice.listing.events.ProsecutionCaseResults) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.judicialResult, that.judicialResult) &&
    java.util.Objects.equals(this.offenceId, that.offenceId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, judicialResult, offenceId);}

  public static class Builder {
    private UUID defendantId;

    private JudicialResult judicialResult;

    private UUID offenceId;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withJudicialResult(final JudicialResult judicialResult) {
      this.judicialResult = judicialResult;
      return this;
    }

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCaseResults prosecutionCaseResults) {
      this.defendantId = prosecutionCaseResults.getDefendantId();
      this.judicialResult = prosecutionCaseResults.getJudicialResult();
      this.offenceId = prosecutionCaseResults.getOffenceId();
      return this;
    }

    public ProsecutionCaseResults build() {
      return new uk.gov.justice.listing.events.ProsecutionCaseResults(defendantId, judicialResult, offenceId);
    }
  }
}

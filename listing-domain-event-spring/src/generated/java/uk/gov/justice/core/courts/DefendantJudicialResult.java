package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class DefendantJudicialResult implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final UUID defendantId;

  private final JudicialResult judicialResult;

  private final UUID masterDefendantId;

  @JsonCreator
  public DefendantJudicialResult(final UUID defendantId, final JudicialResult judicialResult, final UUID masterDefendantId) {
    this.defendantId = defendantId;
    this.judicialResult = judicialResult;
    this.masterDefendantId = masterDefendantId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public JudicialResult getJudicialResult() {
    return judicialResult;
  }

  public UUID getMasterDefendantId() {
    return masterDefendantId;
  }

  public static Builder defendantJudicialResult() {
    return new uk.gov.justice.core.courts.DefendantJudicialResult.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefendantJudicialResult that = (uk.gov.justice.core.courts.DefendantJudicialResult) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.judicialResult, that.judicialResult) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, judicialResult, masterDefendantId);}

  public static class Builder {
    private UUID defendantId;

    private JudicialResult judicialResult;

    private UUID masterDefendantId;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withJudicialResult(final JudicialResult judicialResult) {
      this.judicialResult = judicialResult;
      return this;
    }

    public Builder withMasterDefendantId(final UUID masterDefendantId) {
      this.masterDefendantId = masterDefendantId;
      return this;
    }

    public Builder withValuesFrom(final DefendantJudicialResult defendantJudicialResult) {
      this.defendantId = defendantJudicialResult.getDefendantId();
      this.judicialResult = defendantJudicialResult.getJudicialResult();
      this.masterDefendantId = defendantJudicialResult.getMasterDefendantId();
      return this;
    }

    public DefendantJudicialResult build() {
      return new uk.gov.justice.core.courts.DefendantJudicialResult(defendantId, judicialResult, masterDefendantId);
    }
  }
}

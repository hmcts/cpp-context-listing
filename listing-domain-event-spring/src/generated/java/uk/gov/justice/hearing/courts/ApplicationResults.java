package uk.gov.justice.hearing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.JudicialResult;

public class ApplicationResults implements Serializable {
  private static final long serialVersionUID = -8762589598591790959L;

  private final UUID applicationId;

  private final UUID defendantId;

  private final JudicialResult judicialResult;

  @JsonCreator
  public ApplicationResults(final UUID applicationId, final UUID defendantId, final JudicialResult judicialResult) {
    this.applicationId = applicationId;
    this.defendantId = defendantId;
    this.judicialResult = judicialResult;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public JudicialResult getJudicialResult() {
    return judicialResult;
  }

  public static Builder applicationResults() {
    return new uk.gov.justice.hearing.courts.ApplicationResults.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.hearing.courts.ApplicationResults that = (uk.gov.justice.hearing.courts.ApplicationResults) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.judicialResult, that.judicialResult);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, defendantId, judicialResult);}

  public static class Builder {
    private UUID applicationId;

    private UUID defendantId;

    private JudicialResult judicialResult;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withJudicialResult(final JudicialResult judicialResult) {
      this.judicialResult = judicialResult;
      return this;
    }

    public Builder withValuesFrom(final ApplicationResults applicationResults) {
      this.applicationId = applicationResults.getApplicationId();
      this.defendantId = applicationResults.getDefendantId();
      this.judicialResult = applicationResults.getJudicialResult();
      return this;
    }

    public ApplicationResults build() {
      return new uk.gov.justice.hearing.courts.ApplicationResults(applicationId, defendantId, judicialResult);
    }
  }
}

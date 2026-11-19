package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class DefendantCase implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final UUID caseId;

  private final String caseReference;

  private final UUID defendantId;

  @JsonCreator
  public DefendantCase(final UUID caseId, final String caseReference, final UUID defendantId) {
    this.caseId = caseId;
    this.caseReference = caseReference;
    this.defendantId = defendantId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public String getCaseReference() {
    return caseReference;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public static Builder defendantCase() {
    return new uk.gov.justice.core.courts.DefendantCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefendantCase that = (uk.gov.justice.core.courts.DefendantCase) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseReference, that.caseReference) &&
    java.util.Objects.equals(this.defendantId, that.defendantId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseReference, defendantId);}

  public static class Builder {
    private UUID caseId;

    private String caseReference;

    private UUID defendantId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCaseReference(final String caseReference) {
      this.caseReference = caseReference;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withValuesFrom(final DefendantCase defendantCase) {
      this.caseId = defendantCase.getCaseId();
      this.caseReference = defendantCase.getCaseReference();
      this.defendantId = defendantCase.getDefendantId();
      return this;
    }

    public DefendantCase build() {
      return new uk.gov.justice.core.courts.DefendantCase(caseId, caseReference, defendantId);
    }
  }
}

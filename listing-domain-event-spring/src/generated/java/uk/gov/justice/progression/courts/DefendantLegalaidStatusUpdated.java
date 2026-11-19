package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class DefendantLegalaidStatusUpdated implements Serializable {
  private static final long serialVersionUID = 7244867664397077359L;

  private final UUID caseId;

  private final UUID defendantId;

  private final String laaContractNumber;

  private final String legalAidStatus;

  @JsonCreator
  public DefendantLegalaidStatusUpdated(final UUID caseId, final UUID defendantId, final String laaContractNumber, final String legalAidStatus) {
    this.caseId = caseId;
    this.defendantId = defendantId;
    this.laaContractNumber = laaContractNumber;
    this.legalAidStatus = legalAidStatus;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public String getLaaContractNumber() {
    return laaContractNumber;
  }

  public String getLegalAidStatus() {
    return legalAidStatus;
  }

  public static Builder defendantLegalaidStatusUpdated() {
    return new uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated that = (uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.laaContractNumber, that.laaContractNumber) &&
    java.util.Objects.equals(this.legalAidStatus, that.legalAidStatus);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantId, laaContractNumber, legalAidStatus);}

  public static class Builder {
    private UUID caseId;

    private UUID defendantId;

    private String laaContractNumber;

    private String legalAidStatus;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withLaaContractNumber(final String laaContractNumber) {
      this.laaContractNumber = laaContractNumber;
      return this;
    }

    public Builder withLegalAidStatus(final String legalAidStatus) {
      this.legalAidStatus = legalAidStatus;
      return this;
    }

    public Builder withValuesFrom(final DefendantLegalaidStatusUpdated defendantLegalaidStatusUpdated) {
      this.caseId = defendantLegalaidStatusUpdated.getCaseId();
      this.defendantId = defendantLegalaidStatusUpdated.getDefendantId();
      this.laaContractNumber = defendantLegalaidStatusUpdated.getLaaContractNumber();
      this.legalAidStatus = defendantLegalaidStatusUpdated.getLegalAidStatus();
      return this;
    }

    public DefendantLegalaidStatusUpdated build() {
      return new uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated(caseId, defendantId, laaContractNumber, legalAidStatus);
    }
  }
}

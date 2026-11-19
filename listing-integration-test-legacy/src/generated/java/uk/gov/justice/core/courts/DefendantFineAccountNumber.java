package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class DefendantFineAccountNumber implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID defendantId;

  private final String fineAccountNumber;

  @JsonCreator
  public DefendantFineAccountNumber(final UUID defendantId, final String fineAccountNumber) {
    this.defendantId = defendantId;
    this.fineAccountNumber = fineAccountNumber;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public String getFineAccountNumber() {
    return fineAccountNumber;
  }

  public static Builder defendantFineAccountNumber() {
    return new uk.gov.justice.core.courts.DefendantFineAccountNumber.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefendantFineAccountNumber that = (uk.gov.justice.core.courts.DefendantFineAccountNumber) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.fineAccountNumber, that.fineAccountNumber);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, fineAccountNumber);}

  public static class Builder {
    private UUID defendantId;

    private String fineAccountNumber;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withFineAccountNumber(final String fineAccountNumber) {
      this.fineAccountNumber = fineAccountNumber;
      return this;
    }

    public Builder withValuesFrom(final DefendantFineAccountNumber defendantFineAccountNumber) {
      this.defendantId = defendantFineAccountNumber.getDefendantId();
      this.fineAccountNumber = defendantFineAccountNumber.getFineAccountNumber();
      return this;
    }

    public DefendantFineAccountNumber build() {
      return new uk.gov.justice.core.courts.DefendantFineAccountNumber(defendantId, fineAccountNumber);
    }
  }
}

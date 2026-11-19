package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CustodyTimeLimit implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Integer daysSpent;

  private final Boolean isCtlExtended;

  private final String timeLimit;

  @JsonCreator
  public CustodyTimeLimit(final Integer daysSpent, final Boolean isCtlExtended, final String timeLimit) {
    this.daysSpent = daysSpent;
    this.isCtlExtended = isCtlExtended;
    this.timeLimit = timeLimit;
  }

  public Integer getDaysSpent() {
    return daysSpent;
  }

  public Boolean getIsCtlExtended() {
    return isCtlExtended;
  }

  public String getTimeLimit() {
    return timeLimit;
  }

  public static Builder custodyTimeLimit() {
    return new uk.gov.justice.core.courts.CustodyTimeLimit.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CustodyTimeLimit that = (uk.gov.justice.core.courts.CustodyTimeLimit) obj;

    return java.util.Objects.equals(this.daysSpent, that.daysSpent) &&
    java.util.Objects.equals(this.isCtlExtended, that.isCtlExtended) &&
    java.util.Objects.equals(this.timeLimit, that.timeLimit);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(daysSpent, isCtlExtended, timeLimit);}

  public static class Builder {
    private Integer daysSpent;

    private Boolean isCtlExtended;

    private String timeLimit;

    public Builder withDaysSpent(final Integer daysSpent) {
      this.daysSpent = daysSpent;
      return this;
    }

    public Builder withIsCtlExtended(final Boolean isCtlExtended) {
      this.isCtlExtended = isCtlExtended;
      return this;
    }

    public Builder withTimeLimit(final String timeLimit) {
      this.timeLimit = timeLimit;
      return this;
    }

    public Builder withValuesFrom(final CustodyTimeLimit custodyTimeLimit) {
      this.daysSpent = custodyTimeLimit.getDaysSpent();
      this.isCtlExtended = custodyTimeLimit.getIsCtlExtended();
      this.timeLimit = custodyTimeLimit.getTimeLimit();
      return this;
    }

    public CustodyTimeLimit build() {
      return new uk.gov.justice.core.courts.CustodyTimeLimit(daysSpent, isCtlExtended, timeLimit);
    }
  }
}

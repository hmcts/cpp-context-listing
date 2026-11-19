package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CustodyTimeLimit implements Serializable {
  private static final long serialVersionUID = -2619644771746664968L;

  private final Integer daysSpent;

  private final String timeLimit;

  @JsonCreator
  public CustodyTimeLimit(final Integer daysSpent, final String timeLimit) {
    this.daysSpent = daysSpent;
    this.timeLimit = timeLimit;
  }

  public Integer getDaysSpent() {
    return daysSpent;
  }

  public String getTimeLimit() {
    return timeLimit;
  }

  public static Builder custodyTimeLimit() {
    return new uk.gov.justice.listing.commands.CustodyTimeLimit.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.CustodyTimeLimit that = (uk.gov.justice.listing.commands.CustodyTimeLimit) obj;

    return java.util.Objects.equals(this.daysSpent, that.daysSpent) &&
    java.util.Objects.equals(this.timeLimit, that.timeLimit);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(daysSpent, timeLimit);}

  public static class Builder {
    private Integer daysSpent;

    private String timeLimit;

    public Builder withDaysSpent(final Integer daysSpent) {
      this.daysSpent = daysSpent;
      return this;
    }

    public Builder withTimeLimit(final String timeLimit) {
      this.timeLimit = timeLimit;
      return this;
    }

    public Builder withValuesFrom(final CustodyTimeLimit custodyTimeLimit) {
      this.daysSpent = custodyTimeLimit.getDaysSpent();
      this.timeLimit = custodyTimeLimit.getTimeLimit();
      return this;
    }

    public CustodyTimeLimit build() {
      return new uk.gov.justice.listing.commands.CustodyTimeLimit(daysSpent, timeLimit);
    }
  }
}

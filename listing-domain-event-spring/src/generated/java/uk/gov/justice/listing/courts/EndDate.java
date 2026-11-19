package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class EndDate implements Serializable {
  private static final long serialVersionUID = -2582361536581928800L;

  private final String endDate;

  @JsonCreator
  public EndDate(final String endDate) {
    this.endDate = endDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public static Builder endDate() {
    return new uk.gov.justice.listing.courts.EndDate.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.EndDate that = (uk.gov.justice.listing.courts.EndDate) obj;

    return java.util.Objects.equals(this.endDate, that.endDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(endDate);}

  public static class Builder {
    private String endDate;

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withValuesFrom(final EndDate endDate) {
      this.endDate = endDate.getEndDate();
      return this;
    }

    public EndDate build() {
      return new uk.gov.justice.listing.courts.EndDate(endDate);
    }
  }
}

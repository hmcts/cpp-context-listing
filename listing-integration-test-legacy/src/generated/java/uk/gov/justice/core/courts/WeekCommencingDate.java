package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class WeekCommencingDate implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Integer duration;

  private final String startDate;

  @JsonCreator
  public WeekCommencingDate(final Integer duration, final String startDate) {
    this.duration = duration;
    this.startDate = startDate;
  }

  public Integer getDuration() {
    return duration;
  }

  public String getStartDate() {
    return startDate;
  }

  public static Builder weekCommencingDate() {
    return new uk.gov.justice.core.courts.WeekCommencingDate.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.WeekCommencingDate that = (uk.gov.justice.core.courts.WeekCommencingDate) obj;

    return java.util.Objects.equals(this.duration, that.duration) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(duration, startDate);}

  public static class Builder {
    private Integer duration;

    private String startDate;

    public Builder withDuration(final Integer duration) {
      this.duration = duration;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final WeekCommencingDate weekCommencingDate) {
      this.duration = weekCommencingDate.getDuration();
      this.startDate = weekCommencingDate.getStartDate();
      return this;
    }

    public WeekCommencingDate build() {
      return new uk.gov.justice.core.courts.WeekCommencingDate(duration, startDate);
    }
  }
}

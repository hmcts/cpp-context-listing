package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.List;

public class FutureSummonsHearing implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final CourtCentre courtCentre;

  private final ZonedDateTime earliestStartDateTime;

  private final Integer estimatedMinutes;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final WeekCommencingDate weekCommencingDate;

  @JsonCreator
  public FutureSummonsHearing(final CourtCentre courtCentre, final ZonedDateTime earliestStartDateTime, final Integer estimatedMinutes, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final WeekCommencingDate weekCommencingDate) {
    this.courtCentre = courtCentre;
    this.earliestStartDateTime = earliestStartDateTime;
    this.estimatedMinutes = estimatedMinutes;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.weekCommencingDate = weekCommencingDate;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public ZonedDateTime getEarliestStartDateTime() {
    return earliestStartDateTime;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public WeekCommencingDate getWeekCommencingDate() {
    return weekCommencingDate;
  }

  public static Builder futureSummonsHearing() {
    return new uk.gov.justice.core.courts.FutureSummonsHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.FutureSummonsHearing that = (uk.gov.justice.core.courts.FutureSummonsHearing) obj;

    return java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.earliestStartDateTime, that.earliestStartDateTime) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.weekCommencingDate, that.weekCommencingDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentre, earliestStartDateTime, estimatedMinutes, judiciary, jurisdictionType, weekCommencingDate);}

  public static class Builder {
    private CourtCentre courtCentre;

    private ZonedDateTime earliestStartDateTime;

    private Integer estimatedMinutes;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private WeekCommencingDate weekCommencingDate;

    public Builder withCourtCentre(final CourtCentre courtCentre) {
      this.courtCentre = courtCentre;
      return this;
    }

    public Builder withEarliestStartDateTime(final ZonedDateTime earliestStartDateTime) {
      this.earliestStartDateTime = earliestStartDateTime;
      return this;
    }

    public Builder withEstimatedMinutes(final Integer estimatedMinutes) {
      this.estimatedMinutes = estimatedMinutes;
      return this;
    }

    public Builder withJudiciary(final List<JudicialRole> judiciary) {
      this.judiciary = judiciary;
      return this;
    }

    public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
      this.jurisdictionType = jurisdictionType;
      return this;
    }

    public Builder withWeekCommencingDate(final WeekCommencingDate weekCommencingDate) {
      this.weekCommencingDate = weekCommencingDate;
      return this;
    }

    public Builder withValuesFrom(final FutureSummonsHearing futureSummonsHearing) {
      this.courtCentre = futureSummonsHearing.getCourtCentre();
      this.earliestStartDateTime = futureSummonsHearing.getEarliestStartDateTime();
      this.estimatedMinutes = futureSummonsHearing.getEstimatedMinutes();
      this.judiciary = futureSummonsHearing.getJudiciary();
      this.jurisdictionType = futureSummonsHearing.getJurisdictionType();
      this.weekCommencingDate = futureSummonsHearing.getWeekCommencingDate();
      return this;
    }

    public FutureSummonsHearing build() {
      return new uk.gov.justice.core.courts.FutureSummonsHearing(courtCentre, earliestStartDateTime, estimatedMinutes, judiciary, jurisdictionType, weekCommencingDate);
    }
  }
}

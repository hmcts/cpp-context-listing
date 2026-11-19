package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class WeekCommencingDateChangedForHearing implements Serializable {
  private static final long serialVersionUID = -4734674097672592584L;

  private final UUID hearingId;

  private final Integer weekCommencingDurationInWeeks;

  private final String weekCommencingEndDate;

  private final String weekCommencingStartDate;

  @JsonCreator
  public WeekCommencingDateChangedForHearing(final UUID hearingId, final Integer weekCommencingDurationInWeeks, final String weekCommencingEndDate, final String weekCommencingStartDate) {
    this.hearingId = hearingId;
    this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;
    this.weekCommencingEndDate = weekCommencingEndDate;
    this.weekCommencingStartDate = weekCommencingStartDate;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Integer getWeekCommencingDurationInWeeks() {
    return weekCommencingDurationInWeeks;
  }

  public String getWeekCommencingEndDate() {
    return weekCommencingEndDate;
  }

  public String getWeekCommencingStartDate() {
    return weekCommencingStartDate;
  }

  public static Builder weekCommencingDateChangedForHearing() {
    return new uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing that = (uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.weekCommencingDurationInWeeks, that.weekCommencingDurationInWeeks) &&
    java.util.Objects.equals(this.weekCommencingEndDate, that.weekCommencingEndDate) &&
    java.util.Objects.equals(this.weekCommencingStartDate, that.weekCommencingStartDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);}

  public static class Builder {
    private UUID hearingId;

    private Integer weekCommencingDurationInWeeks;

    private String weekCommencingEndDate;

    private String weekCommencingStartDate;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withWeekCommencingDurationInWeeks(final Integer weekCommencingDurationInWeeks) {
      this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;
      return this;
    }

    public Builder withWeekCommencingEndDate(final String weekCommencingEndDate) {
      this.weekCommencingEndDate = weekCommencingEndDate;
      return this;
    }

    public Builder withWeekCommencingStartDate(final String weekCommencingStartDate) {
      this.weekCommencingStartDate = weekCommencingStartDate;
      return this;
    }

    public Builder withValuesFrom(final WeekCommencingDateChangedForHearing weekCommencingDateChangedForHearing) {
      this.hearingId = weekCommencingDateChangedForHearing.getHearingId();
      this.weekCommencingDurationInWeeks = weekCommencingDateChangedForHearing.getWeekCommencingDurationInWeeks();
      this.weekCommencingEndDate = weekCommencingDateChangedForHearing.getWeekCommencingEndDate();
      this.weekCommencingStartDate = weekCommencingDateChangedForHearing.getWeekCommencingStartDate();
      return this;
    }

    public WeekCommencingDateChangedForHearing build() {
      return new uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing(hearingId, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);
    }
  }
}

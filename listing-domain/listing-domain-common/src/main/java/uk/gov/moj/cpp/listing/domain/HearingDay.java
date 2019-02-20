package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067"})
public class HearingDay implements Serializable {
  private static final long serialVersionUID = 1L;

  private final Integer durationMinutes;

  private final ZonedDateTime endTime;

  private final LocalDate hearingDate;

  private final Integer sequence;

  private final ZonedDateTime startTime;

  public HearingDay(final Integer durationMinutes, final ZonedDateTime endTime, final LocalDate hearingDate, final Integer sequence, final ZonedDateTime startTime) {
    this.durationMinutes = durationMinutes;
    this.endTime = endTime;
    this.hearingDate = hearingDate;
    this.sequence = sequence;
    this.startTime = startTime;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public ZonedDateTime getEndTime() {
    return endTime;
  }

  public LocalDate getHearingDate() {
    return hearingDate;
  }

  public Integer getSequence() {
    return sequence;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public static Builder hearingDay() {
    return new HearingDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final HearingDay that = (HearingDay) obj;

    return java.util.Objects.equals(this.durationMinutes, that.durationMinutes) &&
            java.util.Objects.equals(this.endTime, that.endTime) &&
            java.util.Objects.equals(this.hearingDate, that.hearingDate) &&
            java.util.Objects.equals(this.sequence, that.sequence) &&
            java.util.Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(durationMinutes, endTime, hearingDate, sequence, startTime);}

  @Override
  public String toString() {
    return "HearingDay{" +
            "durationMinutes='" + durationMinutes + "'," +
            "endTime='" + endTime + "'," +
            "hearingDate='" + hearingDate + "'," +
            "sequence='" + sequence + "'," +
            "startTime='" + startTime + "'" +
            "}";
  }

  public static class Builder {
    private Integer durationMinutes;

    private ZonedDateTime endTime;

    private LocalDate hearingDate;

    private Integer sequence;

    private ZonedDateTime startTime;

    public Builder withDurationMinutes(final Integer durationMinutes) {
      this.durationMinutes = durationMinutes;
      return this;
    }

    public Builder withEndTime(final ZonedDateTime endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder withHearingDate(final LocalDate hearingDate) {
      this.hearingDate = hearingDate;
      return this;
    }

    public Builder withSequence(final Integer sequence) {
      this.sequence = sequence;
      return this;
    }

    public Builder withStartTime(final ZonedDateTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public HearingDay build() {
      return new HearingDay(durationMinutes, endTime, hearingDate, sequence, startTime);
    }
  }
}

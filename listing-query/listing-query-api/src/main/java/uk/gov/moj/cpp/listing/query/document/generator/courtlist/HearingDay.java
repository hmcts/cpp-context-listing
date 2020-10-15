package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.Objects;

public class HearingDay {

    private String hearingDate;
    private String endTime;
    private String startTime;
    private int durationMinutes;

    public String getHearingDate() {
        return hearingDate;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public static HearingDay.Builder hearingDay() {
        return new HearingDay.Builder();
    }

    public static final class Builder {

        private String hearingDate;
        private String endTime;
        private String startTime;
        private int durationMinutes;

        private Builder() {
        }


        public Builder withHearingDate(final String hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withEndTime(final String endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withStartTime(final String startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withDurationMinutes(final int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }


        public HearingDay build() {
            final HearingDay hearingDates = new HearingDay();
            hearingDates.hearingDate = this.hearingDate;
            hearingDates.durationMinutes = this.durationMinutes;
            hearingDates.startTime = this.startTime;
            hearingDates.endTime = this.endTime;
            return hearingDates;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HearingDay)) {
            return false;
        }
        final HearingDay that = (HearingDay) o;
        return Objects.equals(hearingDate, that.hearingDate) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(durationMinutes, that.durationMinutes) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingDate, startTime, durationMinutes, endTime);
    }

    @Override
    public String toString() {
        return "HearingDate{" +
                "hearingDate='" + hearingDate + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", durationMinutes=" + durationMinutes +
                '}';
    }
}

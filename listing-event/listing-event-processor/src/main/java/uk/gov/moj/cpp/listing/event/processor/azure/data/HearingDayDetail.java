package uk.gov.moj.cpp.listing.event.processor.azure.data;

import java.util.Objects;
import java.util.Optional;

public class HearingDayDetail {
    private String date;
    private String time;
    private int duration;
    private Optional<String> courtScheduleId;

    public HearingDayDetail(final String date, final String time, final int duration, final Optional<String> courtScheduleId) {
        this.date = date;
        this.time = time;
        this.duration = duration;
        this.courtScheduleId = courtScheduleId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(final String time) {
        this.time = time;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(final int duration) {
        this.duration = duration;
    }

    public Optional<String> getCourtScheduleId() {
        return courtScheduleId;
    }

    public void setCourtScheduleId(final Optional<String> courtScheduleId) {
        this.courtScheduleId = courtScheduleId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HearingDayDetail that = (HearingDayDetail) o;
        return duration == that.duration &&
                Objects.equals(date, that.date) &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, time, duration);
    }
}

package uk.gov.moj.cpp.listing.domain;

import java.util.Objects;

public class ListUpdateHearing {
    private String hearingId;
    private String courtScheduleId;
    private String hearingStartTime;
    private Integer duration;

    public String getHearingId() {
        return hearingId;
    }

    public void setHearingId(String hearingId) {
        this.hearingId = hearingId;
    }

    public String getCourtScheduleId() {
        return courtScheduleId;
    }

    public void setCourtScheduleId(String courtScheduleId) {
        this.courtScheduleId = courtScheduleId;
    }

    public String getHearingStartTime() {
        return hearingStartTime;
    }

    public void setHearingStartTime(String hearingStartTime) {
        this.hearingStartTime = hearingStartTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListUpdateHearing that)) return false;
        return Objects.equals(hearingId, that.hearingId) &&
                Objects.equals(courtScheduleId, that.courtScheduleId) &&
                Objects.equals(hearingStartTime, that.hearingStartTime) &&
                Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingId, courtScheduleId, hearingStartTime, duration);
    }

    @Override
    public String toString() {
        return "HearingSlot{" +
                "hearingId='" + hearingId + '\'' +
                ", courtScheduleId='" + courtScheduleId + '\'' +
                ", hearingStartTime='" + hearingStartTime + '\'' +
                ", duration=" + duration +
                '}';
    }
}

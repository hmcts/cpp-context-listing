package uk.gov.moj.cpp.listing.query.view.hearing;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

@SuppressWarnings({"squid:S1067"})
public class HearingDay {

    private LocalDate hearingDate;

    private ZonedDateTime startTime;

    private ZonedDateTime endTime;

    private String courtCentreId;

    private String courtRoomId;

    private String courtScheduleId;

    private boolean matchedWithQuery;

    public HearingDay(final LocalDate hearingDate, final ZonedDateTime startTime, final ZonedDateTime endTime, final String courtCentreId, final String courtRoomId, final String courtScheduleId, final boolean matchedWithQuery) {
        this.hearingDate = hearingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.courtScheduleId = courtScheduleId;
        this.matchedWithQuery = matchedWithQuery;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public String getCourtScheduleId() {
        return courtScheduleId;
    }

    public boolean isMatchedWithQuery() {
        return matchedWithQuery;
    }

    public void setMatchedWithQuery(final boolean matchedWithQuery) {
        this.matchedWithQuery = matchedWithQuery;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HearingDay that = (HearingDay) o;
        return Objects.equals(hearingDate, that.hearingDate) &&
               Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime) &&
               Objects.equals(courtCentreId, that.courtCentreId) &&
               Objects.equals(courtRoomId, that.courtRoomId) &&
               Objects.equals(courtScheduleId, that.courtScheduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingDate, startTime, endTime, courtCentreId, courtRoomId, courtScheduleId);
    }

    public static class Builder {

        private LocalDate hearingDate;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private String courtCentreId;
        private String courtRoomId;
        private String courtScheduleId;
        private boolean matchedWithQuery;

        public HearingDay.Builder withHearingDate(final LocalDate hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public HearingDay.Builder withStartTime(final ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public HearingDay.Builder withEndTime(final ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public HearingDay.Builder withCourtCentreId(final String courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public HearingDay.Builder withCourtRoomId(final String courtRoomId) {
            this.courtRoomId = courtRoomId;
            return this;
        }

        public HearingDay.Builder withCourtScheduleId(final String courtScheduleId) {
            this.courtScheduleId = courtScheduleId;
            return this;
        }

        public HearingDay.Builder withMatchedWithQuery(final boolean matchedWithQuery) {
            this.matchedWithQuery = matchedWithQuery;
            return this;
        }

        public HearingDay build() {
            return new HearingDay(hearingDate, startTime, endTime, courtCentreId, courtRoomId, courtScheduleId, matchedWithQuery);
        }
    }

}

package uk.gov.moj.cpp.listing.domain;

import java.time.ZonedDateTime;
import java.util.List;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067", "squid:S2065", "pmd:BeanMembersShouldSerialize"})
public class HmiSession {

    private final String courtHouseId;

    private final String courtHouseName;

    private final String courtRoomId;

    private final String courtRoomName;

    private final Integer courtRoomNumber;

    private final String courtScheduleId;

    private final String courtSession;

    private final Integer duration;

    private final String hearingType;

    private final List<HmiJudiciary> judiciaries;

    private final String listingUrl;

    private final String ouCode;

    private final Integer remainingSlot;

    private final Integer remainingTime;

    private final String sessionDate;

    private final ZonedDateTime sessionStartTime;

    private final String sessionType;

    private final String venueId;

    @SuppressWarnings("squid:S00107")
    public HmiSession(final String courtHouseId, final String courtHouseName, final String courtRoomId, final String courtRoomName, final Integer courtRoomNumber, final String courtScheduleId, final String courtSession, final Integer duration, final String hearingType, final List<HmiJudiciary> judiciaries, final String listingUrl, final String ouCode, final Integer remainingSlot, final Integer remainingTime, final String sessionDate, final ZonedDateTime sessionStartTime, final String sessionType, final String venueId) {
        this.courtHouseId = courtHouseId;
        this.courtHouseName = courtHouseName;
        this.courtRoomId = courtRoomId;
        this.courtRoomName = courtRoomName;
        this.courtRoomNumber = courtRoomNumber;
        this.courtScheduleId = courtScheduleId;
        this.courtSession = courtSession;
        this.duration = duration;
        this.hearingType = hearingType;
        this.judiciaries = judiciaries;
        this.listingUrl = listingUrl;
        this.ouCode = ouCode;
        this.remainingSlot = remainingSlot;
        this.remainingTime = remainingTime;
        this.sessionDate = sessionDate;
        this.sessionStartTime = sessionStartTime;
        this.sessionType = sessionType;
        this.venueId = venueId;
    }

    public String getCourtHouseId() {
        return courtHouseId;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public Integer getCourtRoomNumber() {
        return courtRoomNumber;
    }

    public String getCourtScheduleId() {
        return courtScheduleId;
    }

    public String getCourtSession() {
        return courtSession;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getHearingType() {
        return hearingType;
    }

    public List<HmiJudiciary> getJudiciaries() {
        return judiciaries;
    }

    public String getListingUrl() {
        return listingUrl;
    }

    public String getOuCode() {
        return ouCode;
    }

    public Integer getRemainingSlot() {
        return remainingSlot;
    }

    public Integer getRemainingTime() {
        return remainingTime;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public ZonedDateTime getSessionStartTime() {
        return sessionStartTime;
    }

    public String getSessionType() {
        return sessionType;
    }

    public String getVenueId() {
        return venueId;
    }

    public static Builder hmiSession() {
        return new uk.gov.moj.cpp.listing.domain.HmiSession.Builder();
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final uk.gov.moj.cpp.listing.domain.HmiSession that = (uk.gov.moj.cpp.listing.domain.HmiSession) obj;

        return java.util.Objects.equals(this.courtHouseId, that.courtHouseId) &&
                java.util.Objects.equals(this.courtHouseName, that.courtHouseName) &&
                java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
                java.util.Objects.equals(this.courtRoomName, that.courtRoomName) &&
                java.util.Objects.equals(this.courtRoomNumber, that.courtRoomNumber) &&
                java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
                java.util.Objects.equals(this.courtSession, that.courtSession) &&
                java.util.Objects.equals(this.duration, that.duration) &&
                java.util.Objects.equals(this.hearingType, that.hearingType) &&
                java.util.Objects.equals(this.judiciaries, that.judiciaries) &&
                java.util.Objects.equals(this.listingUrl, that.listingUrl) &&
                java.util.Objects.equals(this.ouCode, that.ouCode) &&
                java.util.Objects.equals(this.remainingSlot, that.remainingSlot) &&
                java.util.Objects.equals(this.remainingTime, that.remainingTime) &&
                java.util.Objects.equals(this.sessionDate, that.sessionDate) &&
                java.util.Objects.equals(this.sessionStartTime, that.sessionStartTime) &&
                java.util.Objects.equals(this.sessionType, that.sessionType) &&
                java.util.Objects.equals(this.venueId, that.venueId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(courtHouseId, courtHouseName, courtRoomId, courtRoomName, courtRoomNumber, courtScheduleId, courtSession, duration, hearingType, judiciaries, listingUrl, ouCode, remainingSlot, remainingTime, sessionDate, sessionStartTime, sessionType, venueId);
    }

    @Override
    public String toString() {
        return "HmiSession{" +
                "courtHouseId='" + courtHouseId + "'," +
                "courtHouseName='" + courtHouseName + "'," +
                "courtRoomId='" + courtRoomId + "'," +
                "courtRoomName='" + courtRoomName + "'," +
                "courtRoomNumber='" + courtRoomNumber + "'," +
                "courtScheduleId='" + courtScheduleId + "'," +
                "courtSession='" + courtSession + "'," +
                "duration='" + duration + "'," +
                "hearingType='" + hearingType + "'," +
                "judiciaries='" + judiciaries + "'," +
                "listingUrl='" + listingUrl + "'," +
                "ouCode='" + ouCode + "'," +
                "remainingSlot='" + remainingSlot + "'," +
                "remainingTime='" + remainingTime + "'," +
                "sessionDate='" + sessionDate + "'," +
                "sessionStartTime='" + sessionStartTime + "'," +
                "sessionType='" + sessionType + "'," +
                "venueId='" + venueId + "'" +
                "}";
    }

    public static class Builder {
        private String courtHouseId;

        private String courtHouseName;

        private String courtRoomId;

        private String courtRoomName;

        private Integer courtRoomNumber;

        private String courtScheduleId;

        private String courtSession;

        private Integer duration;

        private String hearingType;

        private List<HmiJudiciary> judiciaries;

        private String listingUrl;

        private String ouCode;

        private Integer remainingSlot;

        private Integer remainingTime;

        private String sessionDate;

        private ZonedDateTime sessionStartTime;

        private String sessionType;

        private String venueId;

        public Builder withCourtHouseId(final String courtHouseId) {
            this.courtHouseId = courtHouseId;
            return this;
        }

        public Builder withCourtHouseName(final String courtHouseName) {
            this.courtHouseName = courtHouseName;
            return this;
        }

        public Builder withCourtRoomId(final String courtRoomId) {
            this.courtRoomId = courtRoomId;
            return this;
        }

        public Builder withCourtRoomName(final String courtRoomName) {
            this.courtRoomName = courtRoomName;
            return this;
        }

        public Builder withCourtRoomNumber(final Integer courtRoomNumber) {
            this.courtRoomNumber = courtRoomNumber;
            return this;
        }

        public Builder withCourtScheduleId(final String courtScheduleId) {
            this.courtScheduleId = courtScheduleId;
            return this;
        }

        public Builder withCourtSession(final String courtSession) {
            this.courtSession = courtSession;
            return this;
        }

        public Builder withDuration(final Integer duration) {
            this.duration = duration;
            return this;
        }

        public Builder withHearingType(final String hearingType) {
            this.hearingType = hearingType;
            return this;
        }

        public Builder withJudiciaries(final List<HmiJudiciary> judiciaries) {
            this.judiciaries = judiciaries;
            return this;
        }

        public Builder withListingUrl(final String listingUrl) {
            this.listingUrl = listingUrl;
            return this;
        }

        public Builder withOuCode(final String ouCode) {
            this.ouCode = ouCode;
            return this;
        }

        public Builder withRemainingSlot(final Integer remainingSlot) {
            this.remainingSlot = remainingSlot;
            return this;
        }

        public Builder withRemainingTime(final Integer remainingTime) {
            this.remainingTime = remainingTime;
            return this;
        }

        public Builder withSessionDate(final String sessionDate) {
            this.sessionDate = sessionDate;
            return this;
        }

        public Builder withSessionStartTime(final ZonedDateTime sessionStartTime) {
            this.sessionStartTime = sessionStartTime;
            return this;
        }

        public Builder withSessionType(final String sessionType) {
            this.sessionType = sessionType;
            return this;
        }

        public Builder withVenueId(final String venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder withValuesFrom(final HmiSession hmiSession) {
            this.courtHouseId = hmiSession.getCourtHouseId();
            this.courtHouseName = hmiSession.getCourtHouseName();
            this.courtRoomId = hmiSession.getCourtRoomId();
            this.courtRoomName = hmiSession.getCourtRoomName();
            this.courtRoomNumber = hmiSession.getCourtRoomNumber();
            this.courtScheduleId = hmiSession.getCourtScheduleId();
            this.courtSession = hmiSession.getCourtSession();
            this.duration = hmiSession.getDuration();
            this.hearingType = hmiSession.getHearingType();
            this.judiciaries = hmiSession.getJudiciaries();
            this.listingUrl = hmiSession.getListingUrl();
            this.ouCode = hmiSession.getOuCode();
            this.remainingSlot = hmiSession.getRemainingSlot();
            this.remainingTime = hmiSession.getRemainingTime();
            this.sessionDate = hmiSession.getSessionDate();
            this.sessionStartTime = hmiSession.getSessionStartTime();
            this.sessionType = hmiSession.getSessionType();
            this.venueId = hmiSession.getVenueId();
            return this;
        }

        public HmiSession build() {
            return new uk.gov.moj.cpp.listing.domain.HmiSession(courtHouseId, courtHouseName, courtRoomId, courtRoomName, courtRoomNumber, courtScheduleId, courtSession, duration, hearingType, judiciaries, listingUrl, ouCode, remainingSlot, remainingTime, sessionDate, sessionStartTime, sessionType, venueId);
        }
    }
}

package uk.gov.moj.cpp.listing.event.processor.azure.data;

import java.util.Objects;

@SuppressWarnings({"squid:S1067"})
public class SlotDetail {

    private String ouCode;
    private String businessType;
    private int courtRoomId;
    private String sessionDate;
    private String session;
    private String hearingId;
    private int duration;
    private String courtScheduleId;
    private String bookingId;


    public SlotDetail(final String ouCode,
                      final String businessType,
                      final int courtRoomId,
                      final String sessionDate,
                      final String session,
                      final String hearingId,
                      final int duration,
                      final String courtScheduleId,
                      final String bookingId) {
        this.ouCode = ouCode;
        this.businessType = businessType;
        this.courtRoomId = courtRoomId;
        this.sessionDate = sessionDate;
        this.session = session;
        this.hearingId = hearingId;
        this.duration = duration;
        this.courtScheduleId = courtScheduleId;
        this.bookingId = bookingId;
    }

    public String getOuCode() {
        return ouCode;
    }

    public void setOuCode(final String ouCode) {
        this.ouCode = ouCode;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(final String businessType) {
        this.businessType = businessType;
    }

    public int getCourtRoomId() {
        return courtRoomId;
    }

    public void setCourtRoomId(final int courtRoomId) {
        this.courtRoomId = courtRoomId;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(final String sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getSession() {
        return session;
    }

    public void setSession(final String session) {
        this.session = session;
    }

    public String getHearingId() {
        return hearingId;
    }

    public void setHearingId(final String hearingId) {
        this.hearingId = hearingId;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(final int duration) {
        this.duration = duration;
    }

    public String getCourtScheduleId() {
        return courtScheduleId;
    }

    public void setCourtScheduleId(final String courtScheduleId) {
        this.courtScheduleId = courtScheduleId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(final String bookingId) {
        this.bookingId = bookingId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SlotDetail that = (SlotDetail) o;
        return duration == that.duration &&
                Objects.equals(ouCode, that.ouCode) &&
                Objects.equals(businessType, that.businessType) &&
                Objects.equals(courtRoomId, that.courtRoomId) &&
                Objects.equals(sessionDate, that.sessionDate) &&
                Objects.equals(session, that.session) &&
                Objects.equals(hearingId, that.hearingId) &&
                Objects.equals(courtScheduleId, that.courtScheduleId) &&
                Objects.equals(bookingId, that.bookingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ouCode, businessType, courtRoomId, sessionDate, session, hearingId, duration, courtScheduleId, bookingId);
    }
}

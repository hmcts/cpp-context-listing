package uk.gov.moj.cpp.listing.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@SuppressWarnings({"squid:S00107", "squid:S1210"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtSchedule implements Comparable<CourtSchedule> {

    private String bookingId;
    private String courtScheduleId;
    private String listingProfileId;
    private String ouCode;
    private String courtRoomId;
    private Integer courtRoomNumber;
    private String courtHouseName;
    private String courtRoomName;
    private String operationalUnit;
    private String businessType;
    private String panel;
    private String courtSession;
    private LocalDate sessionDate;
    private Integer maxSlots;
    private Integer maxDuration;
    private Integer availableSlots;
    private Integer availableDuration;
    private String hearingStartTime;
    private String courtHouseId;
    private final List<CourtScheduleJudiciary> judiciaries = new ArrayList();

    public CourtSchedule() {
    }

    public CourtSchedule(final String bookingId, final String courtScheduleId, final String listingProfileId, final String ouCode, final String courtRoomId, final Integer courtRoomNumber, final String courtHouseName, final String courtRoomName, final String operationalUnit, final String businessType, final String panel, final String courtSession, final LocalDate sessionDate, final Integer maxSlots, final Integer maxDuration, final Integer availableSlots, final Integer availableDuration, final String hearingStartTime, final String courtHouseId) {
        this.bookingId = bookingId;
        this.courtScheduleId = courtScheduleId;
        this.listingProfileId = listingProfileId;
        this.ouCode = ouCode;
        this.courtRoomId = courtRoomId;
        this.courtRoomNumber = courtRoomNumber;
        this.courtHouseName = courtHouseName;
        this.courtRoomName = courtRoomName;
        this.operationalUnit = operationalUnit;
        this.businessType = businessType;
        this.panel = panel;
        this.courtSession = courtSession;
        this.sessionDate = sessionDate;
        this.maxSlots = maxSlots;
        this.maxDuration = maxDuration;
        this.availableSlots = availableSlots;
        this.availableDuration = availableDuration;
        this.hearingStartTime = hearingStartTime;
        this.courtHouseId = courtHouseId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(final String bookingId) {
        this.bookingId = bookingId;
    }

    public String getCourtScheduleId() {
        return courtScheduleId;
    }

    public void setCourtScheduleId(final String courtScheduleId) {
        this.courtScheduleId = courtScheduleId;
    }

    public String getListingProfileId() {
        return listingProfileId;
    }

    public void setListingProfileId(final String listingProfileId) {
        this.listingProfileId = listingProfileId;
    }

    public String getOuCode() {
        return ouCode;
    }

    public void setOuCode(final String ouCode) {
        this.ouCode = ouCode;
    }

    public String getCourtRoomId() {
        return courtRoomId;
    }

    public void setCourtRoomId(final String courtRoomId) {
        this.courtRoomId = courtRoomId;
    }

    public Integer getCourtRoomNumber() {
        return courtRoomNumber;
    }

    public void setCourtRoomNumber(final Integer courtRoomNumber) {
        this.courtRoomNumber = courtRoomNumber;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public void setCourtHouseName(final String courtHouseName) {
        this.courtHouseName = courtHouseName;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public void setCourtRoomName(final String courtRoomName) {
        this.courtRoomName = courtRoomName;
    }

    public String getOperationalUnit() {
        return operationalUnit;
    }

    public void setOperationalUnit(final String operationalUnit) {
        this.operationalUnit = operationalUnit;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(final String businessType) {
        this.businessType = businessType;
    }

    public String getPanel() {
        return panel;
    }

    public void setPanel(final String panel) {
        this.panel = panel;
    }

    public String getCourtSession() {
        return courtSession;
    }

    public void setCourtSession(final String courtSession) {
        this.courtSession = courtSession;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(final LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Integer getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(final Integer maxSlots) {
        this.maxSlots = maxSlots;
    }

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(final Integer maxDuration) {
        this.maxDuration = maxDuration;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(final Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Integer getAvailableDuration() {
        return availableDuration;
    }

    public void setAvailableDuration(final Integer availableDuration) {
        this.availableDuration = availableDuration;
    }

    public List<CourtScheduleJudiciary> getJudiciaries() {
        return judiciaries;
    }

    public String getHearingStartTime() {
        return hearingStartTime;
    }

    public void setHearingStartTime(final String hearingStartTime) {
        this.hearingStartTime = hearingStartTime;
    }

    public String getCourtHouseId() {
        return courtHouseId;
    }

    public void setCourtHouseId(final String courtHouseId) {
        this.courtHouseId = courtHouseId;
    }

    @Override
    public int compareTo(final CourtSchedule o) {
        if (getSessionDate() == null || o.getSessionDate() == null) {
            return 0;
        }
        return getSessionDate().compareTo(o.getSessionDate());
    }
}

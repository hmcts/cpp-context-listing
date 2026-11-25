package uk.gov.moj.cpp.listing.persistence.entity.dashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * One hearing day (or one undated unallocated hearing) for the listing dashboard query.
 * Holds only the fields the weekly view needs - no defendant, offence or judiciary data.
 */
public class ListingDashboardRow {
    private String hearingId;
    private LocalDate hearingDate;
    private LocalDate weekCommencingStartDate;
    private LocalDate weekCommencingEndDate;
    private String courtRoomId;
    private String courtRoomName;
    private String startTime;
    private Integer durationMinutes;
    private String hearingTypeId;
    private String hearingType;
    private Integer hearingDayNumber;
    private Integer totalHearingDays;
    private List<String> caseUrns;
    private List<String> applicationReferences;
    private Boolean allocated;

    // Getters and Setters
    public String getHearingId() { return hearingId; }
    public void setHearingId(String hearingId) { this.hearingId = hearingId; }

    public LocalDate getHearingDate() { return hearingDate; }
    public void setHearingDate(LocalDate hearingDate) { this.hearingDate = hearingDate; }

    public LocalDate getWeekCommencingStartDate() { return weekCommencingStartDate; }
    public void setWeekCommencingStartDate(LocalDate weekCommencingStartDate) { this.weekCommencingStartDate = weekCommencingStartDate; }

    public LocalDate getWeekCommencingEndDate() { return weekCommencingEndDate; }
    public void setWeekCommencingEndDate(LocalDate weekCommencingEndDate) { this.weekCommencingEndDate = weekCommencingEndDate; }

    public String getCourtRoomId() { return courtRoomId; }
    public void setCourtRoomId(String courtRoomId) { this.courtRoomId = courtRoomId; }

    public String getCourtRoomName() { return courtRoomName; }
    public void setCourtRoomName(String courtRoomName) { this.courtRoomName = courtRoomName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getHearingTypeId() { return hearingTypeId; }
    public void setHearingTypeId(String hearingTypeId) { this.hearingTypeId = hearingTypeId; }

    public String getHearingType() { return hearingType; }
    public void setHearingType(String hearingType) { this.hearingType = hearingType; }

    public Integer getHearingDayNumber() { return hearingDayNumber; }
    public void setHearingDayNumber(Integer hearingDayNumber) { this.hearingDayNumber = hearingDayNumber; }

    public Integer getTotalHearingDays() { return totalHearingDays; }
    public void setTotalHearingDays(Integer totalHearingDays) { this.totalHearingDays = totalHearingDays; }

    public List<String> getCaseUrns() { return caseUrns; }
    public void setCaseUrns(List<String> caseUrns) { this.caseUrns = caseUrns; }

    public List<String> getApplicationReferences() { return applicationReferences; }
    public void setApplicationReferences(List<String> applicationReferences) { this.applicationReferences = applicationReferences; }

    public Boolean getAllocated() { return allocated; }
    public void setAllocated(Boolean allocated) { this.allocated = allocated; }
}

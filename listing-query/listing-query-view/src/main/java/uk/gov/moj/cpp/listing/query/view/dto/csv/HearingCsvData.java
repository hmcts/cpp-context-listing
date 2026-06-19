package uk.gov.moj.cpp.listing.query.view.dto.csv;

import java.time.LocalDate;
import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;

/**
 * POJO representing hearing data for CSV export.
 * This replaces the Object[] approach for better type safety and maintainability.
 */
public class HearingCsvData {
    private LocalDate hearingDate;
    private String weekCommencing;
    private String courtroom;
    private String judiciary;
    private String startTime;
    private String hearingType;
    private String duration;
    private String day;
    private String caseUrns;
    private String caseIds;
    private String defendantNames;
    private String defendantFlag;
    private String offences;
    private String publicListNote;
    private String language;
    private String videoHearing;
    private String custodyStatus;
    private String multiDayHearingDetails;
    private String pinnedNotes;
    private String unpinnedNotes;
    private String markers;
    private String reportingRestriction;
    private CsvRecordType recordType;

    // Default constructor
    public HearingCsvData() {}

    // Constructor with all fields
    public HearingCsvData(LocalDate hearingDate, String weekCommencing, String courtroom,
                          String judiciary, String startTime, String hearingType, String duration,
                          String day, String caseUrns, String caseIds, String defendantNames, String defendantFlag,
                          String offences, String publicListNote, String language, String videoHearing,
                          String custodyStatus, String multiDayHearingDetails, String pinnedNotes,
                          String unpinnedNotes, String markers, String reportingRestriction, CsvRecordType recordType) {
        this.hearingDate = hearingDate;
        this.weekCommencing = weekCommencing;
        this.courtroom = courtroom;
        this.judiciary = judiciary;
        this.startTime = startTime;
        this.hearingType = hearingType;
        this.duration = duration;
        this.day = day;
        this.caseUrns = caseUrns;
        this.caseIds = caseIds;
        this.defendantNames = defendantNames;
        this.defendantFlag = defendantFlag;
        this.offences = offences;
        this.publicListNote = publicListNote;
        this.language = language;
        this.videoHearing = videoHearing;
        this.custodyStatus = custodyStatus;
        this.multiDayHearingDetails = multiDayHearingDetails;
        this.pinnedNotes = pinnedNotes;
        this.unpinnedNotes = unpinnedNotes;
        this.markers = markers;
        this.reportingRestriction = reportingRestriction;
        this.recordType = recordType;
    }

    // Getters and Setters
    public LocalDate getHearingDate() { return hearingDate; }
    public void setHearingDate(LocalDate hearingDate) { this.hearingDate = hearingDate; }

    public String getWeekCommencing() { return weekCommencing; }
    public void setWeekCommencing(String weekCommencing) { this.weekCommencing = weekCommencing; }

    public String getCourtroom() { return courtroom; }
    public void setCourtroom(String courtroom) { this.courtroom = courtroom; }

    public String getJudiciary() { return judiciary; }
    public void setJudiciary(String judiciary) { this.judiciary = judiciary; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getHearingType() { return hearingType; }
    public void setHearingType(String hearingType) { this.hearingType = hearingType; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getCaseUrns() { return caseUrns; }
    public void setCaseUrns(String caseUrns) { this.caseUrns = caseUrns; }

    public String getCaseIds() { return caseIds; }
    public void setCaseIds(String caseIds) { this.caseIds = caseIds; }

    public String getDefendantNames() { return defendantNames; }
    public void setDefendantNames(String defendantNames) { this.defendantNames = defendantNames; }

    public String getDefendantFlag() { return defendantFlag; }
    public void setDefendantFlag(String defendantFlag) { this.defendantFlag = defendantFlag; }

    public String getOffences() { return offences; }
    public void setOffences(String offences) { this.offences = offences; }

    public String getPublicListNote() { return publicListNote; }
    public void setPublicListNote(String publicListNote) { this.publicListNote = publicListNote; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getVideoHearing() { return videoHearing; }
    public void setVideoHearing(String videoHearing) { this.videoHearing = videoHearing; }

    public String getCustodyStatus() { return custodyStatus; }
    public void setCustodyStatus(String custodyStatus) { this.custodyStatus = custodyStatus; }

    public String getMultiDayHearingDetails() { return multiDayHearingDetails; }
    public void setMultiDayHearingDetails(String multiDayHearingDetails) { this.multiDayHearingDetails = multiDayHearingDetails; }

    public String getPinnedNotes() { return pinnedNotes; }
    public void setPinnedNotes(String pinnedNotes) { this.pinnedNotes = pinnedNotes; }

    public String getUnpinnedNotes() { return unpinnedNotes; }
    public void setUnpinnedNotes(String unpinnedNotes) { this.unpinnedNotes = unpinnedNotes; }

    public String getMarkers() { return markers; }
    public void setMarkers(String markers) { this.markers = markers; }

    public String getReportingRestriction() { return reportingRestriction; }
    public void setReportingRestriction(String reportingRestriction) { this.reportingRestriction = reportingRestriction; }

    public CsvRecordType getRecordType() { return recordType; }
    public void setRecordType(CsvRecordType recordType) { this.recordType = recordType; }

    /**
     * Converts this POJO to an Object array for backward compatibility with CSV generation.
     * This maintains the existing CSV generation logic while providing a cleaner data model.
     */
    public Object[] toObjectArray() {
        return new Object[]{
            hearingDate,
            weekCommencing,
            courtroom,
            judiciary,
            startTime,
            hearingType,
            duration,
            day,
            caseUrns,
            defendantNames,
            defendantFlag,
            offences,
            publicListNote,
            language,
            videoHearing,
            custodyStatus,
            multiDayHearingDetails,
            pinnedNotes,
            unpinnedNotes,
            markers,
            reportingRestriction,
            recordType != null ? recordType.getValue() : null
        };
    }

    @Override
    public String toString() {
        return "CsvHearingData{" +
                "hearingDate=" + hearingDate +
                ", weekCommencing='" + weekCommencing + '\'' +
                ", courtroom='" + courtroom + '\'' +
                ", judiciary='" + judiciary + '\'' +
                ", startTime='" + startTime + '\'' +
                ", hearingType='" + hearingType + '\'' +
                ", duration='" + duration + '\'' +
                ", day='" + day + '\'' +
                ", caseUrns='" + caseUrns + '\'' +
                ", defendantNames='" + defendantNames + '\'' +
                ", defendantFlag='" + defendantFlag + '\'' +
                ", offences='" + offences + '\'' +
                ", publicListNote='" + publicListNote + '\'' +
                ", language='" + language + '\'' +
                ", videoHearing='" + videoHearing + '\'' +
                ", custodyStatus='" + custodyStatus + '\'' +
                ", multiDayHearingDetails='" + multiDayHearingDetails + '\'' +
                ", pinnedNotes='" + pinnedNotes + '\'' +
                ", unpinnedNotes='" + unpinnedNotes + '\'' +
                ", markers='" + markers + '\'' +
                ", reportingRestriction='" + reportingRestriction + '\'' +
                ", recordType=" + recordType +
                '}';
    }
}

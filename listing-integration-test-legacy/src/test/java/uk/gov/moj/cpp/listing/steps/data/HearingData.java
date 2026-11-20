package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.RotaSlot;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class HearingData {

    private final UUID id;
    private final UUID courtCentreId;
    private  String name;
    private final HearingTypeData hearingTypeData;
    private final int hearingEstimateMinutes;
    private final UUID courtRoomId;
    private final List<ListedCaseData> listedCases;
    private final List<JudicialRoleData> judiciary;
    private final String reportingRestrictionReason;
    private final String jurisdictionType;
    private  List<CourtApplicationData> courtApplications;
    private final List<CourtApplicationPartyListingNeeds> courtApplicationPartyNeeds;
    private final LocalDate hearingStartDate;
    private final LocalDate hearingEndDate;
    private final ZonedDateTime hearingStartTime;
    private LocalDate weekCommencingStartDate;
    private LocalDate weekCommencingEndDate;
    private int weekCommencingDuration;
    private Boolean hasVideoLink;
    private String publicListNote;
    private String adjournmentDate;
    private List<RotaSlot> bookedSlots;
    private String estimatedDuration;

    public HearingData(final UUID id, final UUID courtCentreId, final HearingTypeData hearingTypeData,
                       final LocalDate hearingStartDate, final LocalDate hearingEndDate,
                       final int hearingEstimateMinutes, String estimatedDuration, final UUID courtRoomId,
                       final ZonedDateTime hearingStartTime, final List<ListedCaseData> listedCases,
                       final List<JudicialRoleData> judiciary, final String jurisdictionType,
                       final String reportingRestrictionReason,
                       final List<CourtApplicationData> courtApplications,
                       final List<CourtApplicationPartyListingNeeds> courtApplicationPartyNeeds, final String name) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.name = name;
        this.hearingEstimateMinutes = hearingEstimateMinutes;
        this.estimatedDuration = estimatedDuration;
        this.hearingStartDate = hearingStartDate;
        this.hearingEndDate = hearingEndDate;
        this.hearingTypeData = hearingTypeData;
        this.courtRoomId = courtRoomId;
        this.hearingStartTime = hearingStartTime;
        this.listedCases = listedCases;
        this.judiciary = judiciary;
        this.jurisdictionType = jurisdictionType;
        this.reportingRestrictionReason = reportingRestrictionReason;
        this.courtApplications = courtApplications;
        this.courtApplicationPartyNeeds = courtApplicationPartyNeeds;
    }


    public HearingData(final UUID id, final UUID courtCentreId, final HearingTypeData hearingTypeData,
                       final LocalDate hearingStartDate, final LocalDate hearingEndDate,
                       final int hearingEstimateMinutes, final UUID courtRoomId,
                       final ZonedDateTime hearingStartTime, final List<ListedCaseData> listedCases,
                       final List<JudicialRoleData> judiciary, final String jurisdictionType,
                       final String reportingRestrictionReason,
                       final List<CourtApplicationData> courtApplications,
                       final List<CourtApplicationPartyListingNeeds> courtApplicationPartyNeeds, final String name, final Boolean hasVideoLink, final String publicListNote) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.name = name;
        this.hearingEstimateMinutes = hearingEstimateMinutes;
        this.hearingStartDate = hearingStartDate;
        this.hearingEndDate = hearingEndDate;
        this.hearingTypeData = hearingTypeData;
        this.courtRoomId = courtRoomId;
        this.hearingStartTime = hearingStartTime;
        this.listedCases = listedCases;
        this.judiciary = judiciary;
        this.jurisdictionType = jurisdictionType;
        this.reportingRestrictionReason = reportingRestrictionReason;
        this.courtApplications = courtApplications;
        this.courtApplicationPartyNeeds = courtApplicationPartyNeeds;
        this.hasVideoLink = hasVideoLink;
        this.publicListNote = publicListNote;
    }

    public HearingData(final UUID id, final UUID courtCentreId, final String name, final HearingTypeData hearingTypeData,
                       final LocalDate hearingStartDate, final LocalDate hearingEndDate,
                       final int hearingEstimateMinutes, final UUID courtRoomId,
                       final ZonedDateTime hearingStartTime, final List<ListedCaseData> listedCases,
                       final List<JudicialRoleData> judiciary, final String jurisdictionType,
                       final String reportingRestrictionReason,
                       final List<CourtApplicationData> courtApplications,
                       final List<CourtApplicationPartyListingNeeds> courtApplicationPartyNeeds,
                       final LocalDate weekCommencingStartDate, final LocalDate weekCommencingEndDate, final int weekCommencingDuration) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.name = name;
        this.hearingEstimateMinutes = hearingEstimateMinutes;
        this.hearingStartDate = hearingStartDate;
        this.hearingEndDate = hearingEndDate;
        this.hearingTypeData = hearingTypeData;
        this.courtRoomId = courtRoomId;
        this.hearingStartTime = hearingStartTime;
        this.listedCases = listedCases;
        this.judiciary = judiciary;
        this.jurisdictionType = jurisdictionType;
        this.reportingRestrictionReason = reportingRestrictionReason;
        this.courtApplications = courtApplications;
        this.courtApplicationPartyNeeds = courtApplicationPartyNeeds;
        this.weekCommencingStartDate = weekCommencingStartDate;
        this.weekCommencingEndDate = weekCommencingEndDate;
        this.weekCommencingDuration = weekCommencingDuration;
    }

    public HearingData(final UUID id, final UUID courtCentreId, final String name, final HearingTypeData hearingTypeData,
                       final LocalDate hearingStartDate, final LocalDate hearingEndDate,
                       final int hearingEstimateMinutes, final UUID courtRoomId,
                       final ZonedDateTime hearingStartTime, final List<ListedCaseData> listedCases,
                       final List<JudicialRoleData> judiciary, final String jurisdictionType,
                       final String reportingRestrictionReason,
                       final List<CourtApplicationData> courtApplications,
                       final List<CourtApplicationPartyListingNeeds> courtApplicationPartyNeeds,
                       final List<RotaSlot> bookedSlots) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.name = name;
        this.hearingEstimateMinutes = hearingEstimateMinutes;
        this.hearingStartDate = hearingStartDate;
        this.hearingEndDate = hearingEndDate;
        this.hearingTypeData = hearingTypeData;
        this.courtRoomId = courtRoomId;
        this.hearingStartTime = hearingStartTime;
        this.listedCases = listedCases;
        this.judiciary = judiciary;
        this.jurisdictionType = jurisdictionType;
        this.reportingRestrictionReason = reportingRestrictionReason;
        this.courtApplications = courtApplications;
        this.courtApplicationPartyNeeds = courtApplicationPartyNeeds;
        this.bookedSlots = bookedSlots;
    }

    public HearingData(final UUID id, final UUID courtCentreId, final HearingTypeData hearingTypeData,
                       final LocalDate hearingStartDate, final LocalDate hearingEndDate,
                       final int hearingEstimateMinutes, final UUID courtRoomId,
                       final ZonedDateTime hearingStartTime, final List<ListedCaseData> listedCases,
                       final List<JudicialRoleData> judiciary, final String jurisdictionType,
                       final String reportingRestrictionReason,
                       final List<CourtApplicationData> courtApplications,
                       final List<CourtApplicationPartyListingNeeds> courtApplicationPartyNeeds,
                       final String name,
                       final String adjournmentDate) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.hearingEstimateMinutes = hearingEstimateMinutes;
        this.hearingStartDate = hearingStartDate;
        this.hearingEndDate = hearingEndDate;
        this.hearingTypeData = hearingTypeData;
        this.courtRoomId = courtRoomId;
        this.hearingStartTime = hearingStartTime;
        this.listedCases = listedCases;
        this.judiciary = judiciary;
        this.jurisdictionType = jurisdictionType;
        this.reportingRestrictionReason = reportingRestrictionReason;
        this.courtApplications = courtApplications;
        this.courtApplicationPartyNeeds = courtApplicationPartyNeeds;
        this.name = name;
        this.adjournmentDate = adjournmentDate;
    }

    public UUID getId() { return id; }

    public String getName() {
        return name;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public HearingTypeData getHearingTypeData() {
        return hearingTypeData;
    }

    public LocalDate getHearingStartDate() {
        return hearingStartDate;
    }

    public int getHearingEstimateMinutes() {
        return hearingEstimateMinutes;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public LocalDate getHearingEndDate() {
        return hearingEndDate;
    }

    public ZonedDateTime getHearingStartTime() {
        return hearingStartTime;
    }

    public List<ListedCaseData> getListedCases() {
        return listedCases;
    }

    public List<JudicialRoleData> getJudiciary() {
        return judiciary;
    }

    public String getJurisdictionType() {
        return jurisdictionType;
    }

    public String getReportingRestrictionReason() {
        return reportingRestrictionReason;
    }

    public List<CourtApplicationData> getCourtApplications() {
        return courtApplications;
    }

    public  void setCourtApplications(List<CourtApplicationData> courtApplicationData) {
         courtApplications = courtApplicationData;
    }

    public List<CourtApplicationPartyListingNeeds> getCourtApplicationPartyNeeds() {
        return courtApplicationPartyNeeds;
    }

    public LocalDate getWeekCommencingStartDate() {
        return weekCommencingStartDate;
    }

    public LocalDate getWeekCommencingEndDate() {
        return weekCommencingEndDate;
    }

    public int getWeekCommencingDuration() {
        return weekCommencingDuration;
    }

    public String getAdjournmentDate() {
        return adjournmentDate;
    }

    public List<RotaSlot> getBookedSlots() {
        return bookedSlots;
    }

    public Boolean getHasVideoLink() {
        return hasVideoLink;
    }

    public String getPublicListNote() {
        return publicListNote;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setName(final String name) {
        this.name = name;
    }
}

package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class NextHearing implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String adjournmentReason;

  private final String applicationTypeCode;

  private final UUID bookingReference;

  private final CourtCentre courtCentre;

  private final String courtScheduleId;

  private final Boolean dateToBeFixed;

  private final String endDate;

  private final String estimatedDuration;

  private final Integer estimatedMinutes;

  private final UUID existingHearingId;

  private final HearingLanguage hearingLanguage;

  private final List<RotaSlot> hmiSlots;

  private final Boolean isFirstReviewHearing;

  private final Boolean isGroupProceedings;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final ZonedDateTime listedStartDateTime;

  private final List<UUID> nextHearingCourtApplicationId;

  private final List<NextHearingProsecutionCase> nextHearingProsecutionCases;

  private final Integer numberOfGroupCases;

  private final String orderName;

  private final String probationTeamAddress;

  private final String probationTeamName;

  private final String reportingRestrictionReason;

  private final Boolean reservedJudiciary;

  private final String reviewFrequency;

  private final String suspendedPeriod;

  private final String totalCustodialPeriod;

  private final HearingType type;

  private final String weekCommencingDate;

  @JsonCreator
  public NextHearing(final String adjournmentReason, final String applicationTypeCode, final UUID bookingReference, final CourtCentre courtCentre, final String courtScheduleId, final Boolean dateToBeFixed, final String endDate, final String estimatedDuration, final Integer estimatedMinutes, final UUID existingHearingId, final HearingLanguage hearingLanguage, final List<RotaSlot> hmiSlots, final Boolean isFirstReviewHearing, final Boolean isGroupProceedings, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final ZonedDateTime listedStartDateTime, final List<UUID> nextHearingCourtApplicationId, final List<NextHearingProsecutionCase> nextHearingProsecutionCases, final Integer numberOfGroupCases, final String orderName, final String probationTeamAddress, final String probationTeamName, final String reportingRestrictionReason, final Boolean reservedJudiciary, final String reviewFrequency, final String suspendedPeriod, final String totalCustodialPeriod, final HearingType type, final String weekCommencingDate) {
    this.adjournmentReason = adjournmentReason;
    this.applicationTypeCode = applicationTypeCode;
    this.bookingReference = bookingReference;
    this.courtCentre = courtCentre;
    this.courtScheduleId = courtScheduleId;
    this.dateToBeFixed = dateToBeFixed;
    this.endDate = endDate;
    this.estimatedDuration = estimatedDuration;
    this.estimatedMinutes = estimatedMinutes;
    this.existingHearingId = existingHearingId;
    this.hearingLanguage = hearingLanguage;
    this.hmiSlots = hmiSlots;
    this.isFirstReviewHearing = isFirstReviewHearing;
    this.isGroupProceedings = isGroupProceedings;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.listedStartDateTime = listedStartDateTime;
    this.nextHearingCourtApplicationId = nextHearingCourtApplicationId;
    this.nextHearingProsecutionCases = nextHearingProsecutionCases;
    this.numberOfGroupCases = numberOfGroupCases;
    this.orderName = orderName;
    this.probationTeamAddress = probationTeamAddress;
    this.probationTeamName = probationTeamName;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.reservedJudiciary = reservedJudiciary;
    this.reviewFrequency = reviewFrequency;
    this.suspendedPeriod = suspendedPeriod;
    this.totalCustodialPeriod = totalCustodialPeriod;
    this.type = type;
    this.weekCommencingDate = weekCommencingDate;
  }

  public String getAdjournmentReason() {
    return adjournmentReason;
  }

  public String getApplicationTypeCode() {
    return applicationTypeCode;
  }

  public UUID getBookingReference() {
    return bookingReference;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public String getCourtScheduleId() {
    return courtScheduleId;
  }

  public Boolean getDateToBeFixed() {
    return dateToBeFixed;
  }

  public String getEndDate() {
    return endDate;
  }

  public String getEstimatedDuration() {
    return estimatedDuration;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public UUID getExistingHearingId() {
    return existingHearingId;
  }

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
  }

  public List<RotaSlot> getHmiSlots() {
    return hmiSlots;
  }

  public Boolean getIsFirstReviewHearing() {
    return isFirstReviewHearing;
  }

  public Boolean getIsGroupProceedings() {
    return isGroupProceedings;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public ZonedDateTime getListedStartDateTime() {
    return listedStartDateTime;
  }

  public List<UUID> getNextHearingCourtApplicationId() {
    return nextHearingCourtApplicationId;
  }

  public List<NextHearingProsecutionCase> getNextHearingProsecutionCases() {
    return nextHearingProsecutionCases;
  }

  public Integer getNumberOfGroupCases() {
    return numberOfGroupCases;
  }

  public String getOrderName() {
    return orderName;
  }

  public String getProbationTeamAddress() {
    return probationTeamAddress;
  }

  public String getProbationTeamName() {
    return probationTeamName;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public Boolean getReservedJudiciary() {
    return reservedJudiciary;
  }

  public String getReviewFrequency() {
    return reviewFrequency;
  }

  public String getSuspendedPeriod() {
    return suspendedPeriod;
  }

  public String getTotalCustodialPeriod() {
    return totalCustodialPeriod;
  }

  public HearingType getType() {
    return type;
  }

  public String getWeekCommencingDate() {
    return weekCommencingDate;
  }

  public static Builder nextHearing() {
    return new uk.gov.justice.core.courts.NextHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.NextHearing that = (uk.gov.justice.core.courts.NextHearing) obj;

    return java.util.Objects.equals(this.adjournmentReason, that.adjournmentReason) &&
    java.util.Objects.equals(this.applicationTypeCode, that.applicationTypeCode) &&
    java.util.Objects.equals(this.bookingReference, that.bookingReference) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.dateToBeFixed, that.dateToBeFixed) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.existingHearingId, that.existingHearingId) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.hmiSlots, that.hmiSlots) &&
    java.util.Objects.equals(this.isFirstReviewHearing, that.isFirstReviewHearing) &&
    java.util.Objects.equals(this.isGroupProceedings, that.isGroupProceedings) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.listedStartDateTime, that.listedStartDateTime) &&
    java.util.Objects.equals(this.nextHearingCourtApplicationId, that.nextHearingCourtApplicationId) &&
    java.util.Objects.equals(this.nextHearingProsecutionCases, that.nextHearingProsecutionCases) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.orderName, that.orderName) &&
    java.util.Objects.equals(this.probationTeamAddress, that.probationTeamAddress) &&
    java.util.Objects.equals(this.probationTeamName, that.probationTeamName) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.reservedJudiciary, that.reservedJudiciary) &&
    java.util.Objects.equals(this.reviewFrequency, that.reviewFrequency) &&
    java.util.Objects.equals(this.suspendedPeriod, that.suspendedPeriod) &&
    java.util.Objects.equals(this.totalCustodialPeriod, that.totalCustodialPeriod) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.weekCommencingDate, that.weekCommencingDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournmentReason, applicationTypeCode, bookingReference, courtCentre, courtScheduleId, dateToBeFixed, endDate, estimatedDuration, estimatedMinutes, existingHearingId, hearingLanguage, hmiSlots, isFirstReviewHearing, isGroupProceedings, judiciary, jurisdictionType, listedStartDateTime, nextHearingCourtApplicationId, nextHearingProsecutionCases, numberOfGroupCases, orderName, probationTeamAddress, probationTeamName, reportingRestrictionReason, reservedJudiciary, reviewFrequency, suspendedPeriod, totalCustodialPeriod, type, weekCommencingDate);}

  public static class Builder {
    private String adjournmentReason;

    private String applicationTypeCode;

    private UUID bookingReference;

    private CourtCentre courtCentre;

    private String courtScheduleId;

    private Boolean dateToBeFixed;

    private String endDate;

    private String estimatedDuration;

    private Integer estimatedMinutes;

    private UUID existingHearingId;

    private HearingLanguage hearingLanguage;

    private List<RotaSlot> hmiSlots;

    private Boolean isFirstReviewHearing;

    private Boolean isGroupProceedings;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private ZonedDateTime listedStartDateTime;

    private List<UUID> nextHearingCourtApplicationId;

    private List<NextHearingProsecutionCase> nextHearingProsecutionCases;

    private Integer numberOfGroupCases;

    private String orderName;

    private String probationTeamAddress;

    private String probationTeamName;

    private String reportingRestrictionReason;

    private Boolean reservedJudiciary;

    private String reviewFrequency;

    private String suspendedPeriod;

    private String totalCustodialPeriod;

    private HearingType type;

    private String weekCommencingDate;

    public Builder withAdjournmentReason(final String adjournmentReason) {
      this.adjournmentReason = adjournmentReason;
      return this;
    }

    public Builder withApplicationTypeCode(final String applicationTypeCode) {
      this.applicationTypeCode = applicationTypeCode;
      return this;
    }

    public Builder withBookingReference(final UUID bookingReference) {
      this.bookingReference = bookingReference;
      return this;
    }

    public Builder withCourtCentre(final CourtCentre courtCentre) {
      this.courtCentre = courtCentre;
      return this;
    }

    public Builder withCourtScheduleId(final String courtScheduleId) {
      this.courtScheduleId = courtScheduleId;
      return this;
    }

    public Builder withDateToBeFixed(final Boolean dateToBeFixed) {
      this.dateToBeFixed = dateToBeFixed;
      return this;
    }

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withEstimatedDuration(final String estimatedDuration) {
      this.estimatedDuration = estimatedDuration;
      return this;
    }

    public Builder withEstimatedMinutes(final Integer estimatedMinutes) {
      this.estimatedMinutes = estimatedMinutes;
      return this;
    }

    public Builder withExistingHearingId(final UUID existingHearingId) {
      this.existingHearingId = existingHearingId;
      return this;
    }

    public Builder withHearingLanguage(final HearingLanguage hearingLanguage) {
      this.hearingLanguage = hearingLanguage;
      return this;
    }

    public Builder withHmiSlots(final List<RotaSlot> hmiSlots) {
      this.hmiSlots = hmiSlots;
      return this;
    }

    public Builder withIsFirstReviewHearing(final Boolean isFirstReviewHearing) {
      this.isFirstReviewHearing = isFirstReviewHearing;
      return this;
    }

    public Builder withIsGroupProceedings(final Boolean isGroupProceedings) {
      this.isGroupProceedings = isGroupProceedings;
      return this;
    }

    public Builder withJudiciary(final List<JudicialRole> judiciary) {
      this.judiciary = judiciary;
      return this;
    }

    public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
      this.jurisdictionType = jurisdictionType;
      return this;
    }

    public Builder withListedStartDateTime(final ZonedDateTime listedStartDateTime) {
      this.listedStartDateTime = listedStartDateTime;
      return this;
    }

    public Builder withNextHearingCourtApplicationId(final List<UUID> nextHearingCourtApplicationId) {
      this.nextHearingCourtApplicationId = nextHearingCourtApplicationId;
      return this;
    }

    public Builder withNextHearingProsecutionCases(final List<NextHearingProsecutionCase> nextHearingProsecutionCases) {
      this.nextHearingProsecutionCases = nextHearingProsecutionCases;
      return this;
    }

    public Builder withNumberOfGroupCases(final Integer numberOfGroupCases) {
      this.numberOfGroupCases = numberOfGroupCases;
      return this;
    }

    public Builder withOrderName(final String orderName) {
      this.orderName = orderName;
      return this;
    }

    public Builder withProbationTeamAddress(final String probationTeamAddress) {
      this.probationTeamAddress = probationTeamAddress;
      return this;
    }

    public Builder withProbationTeamName(final String probationTeamName) {
      this.probationTeamName = probationTeamName;
      return this;
    }

    public Builder withReportingRestrictionReason(final String reportingRestrictionReason) {
      this.reportingRestrictionReason = reportingRestrictionReason;
      return this;
    }

    public Builder withReservedJudiciary(final Boolean reservedJudiciary) {
      this.reservedJudiciary = reservedJudiciary;
      return this;
    }

    public Builder withReviewFrequency(final String reviewFrequency) {
      this.reviewFrequency = reviewFrequency;
      return this;
    }

    public Builder withSuspendedPeriod(final String suspendedPeriod) {
      this.suspendedPeriod = suspendedPeriod;
      return this;
    }

    public Builder withTotalCustodialPeriod(final String totalCustodialPeriod) {
      this.totalCustodialPeriod = totalCustodialPeriod;
      return this;
    }

    public Builder withType(final HearingType type) {
      this.type = type;
      return this;
    }

    public Builder withWeekCommencingDate(final String weekCommencingDate) {
      this.weekCommencingDate = weekCommencingDate;
      return this;
    }

    public Builder withValuesFrom(final NextHearing nextHearing) {
      this.adjournmentReason = nextHearing.getAdjournmentReason();
      this.applicationTypeCode = nextHearing.getApplicationTypeCode();
      this.bookingReference = nextHearing.getBookingReference();
      this.courtCentre = nextHearing.getCourtCentre();
      this.courtScheduleId = nextHearing.getCourtScheduleId();
      this.dateToBeFixed = nextHearing.getDateToBeFixed();
      this.endDate = nextHearing.getEndDate();
      this.estimatedDuration = nextHearing.getEstimatedDuration();
      this.estimatedMinutes = nextHearing.getEstimatedMinutes();
      this.existingHearingId = nextHearing.getExistingHearingId();
      this.hearingLanguage = nextHearing.getHearingLanguage();
      this.hmiSlots = nextHearing.getHmiSlots();
      this.isFirstReviewHearing = nextHearing.getIsFirstReviewHearing();
      this.isGroupProceedings = nextHearing.getIsGroupProceedings();
      this.judiciary = nextHearing.getJudiciary();
      this.jurisdictionType = nextHearing.getJurisdictionType();
      this.listedStartDateTime = nextHearing.getListedStartDateTime();
      this.nextHearingCourtApplicationId = nextHearing.getNextHearingCourtApplicationId();
      this.nextHearingProsecutionCases = nextHearing.getNextHearingProsecutionCases();
      this.numberOfGroupCases = nextHearing.getNumberOfGroupCases();
      this.orderName = nextHearing.getOrderName();
      this.probationTeamAddress = nextHearing.getProbationTeamAddress();
      this.probationTeamName = nextHearing.getProbationTeamName();
      this.reportingRestrictionReason = nextHearing.getReportingRestrictionReason();
      this.reservedJudiciary = nextHearing.getReservedJudiciary();
      this.reviewFrequency = nextHearing.getReviewFrequency();
      this.suspendedPeriod = nextHearing.getSuspendedPeriod();
      this.totalCustodialPeriod = nextHearing.getTotalCustodialPeriod();
      this.type = nextHearing.getType();
      this.weekCommencingDate = nextHearing.getWeekCommencingDate();
      return this;
    }

    public NextHearing build() {
      return new uk.gov.justice.core.courts.NextHearing(adjournmentReason, applicationTypeCode, bookingReference, courtCentre, courtScheduleId, dateToBeFixed, endDate, estimatedDuration, estimatedMinutes, existingHearingId, hearingLanguage, hmiSlots, isFirstReviewHearing, isGroupProceedings, judiciary, jurisdictionType, listedStartDateTime, nextHearingCourtApplicationId, nextHearingProsecutionCases, numberOfGroupCases, orderName, probationTeamAddress, probationTeamName, reportingRestrictionReason, reservedJudiciary, reviewFrequency, suspendedPeriod, totalCustodialPeriod, type, weekCommencingDate);
    }
  }
}

package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.List;

public class ListHearingRequest implements Serializable {
  private static final long serialVersionUID = 1923407196461966438L;

  private final List<RotaSlot> bookedSlots;

  private final String bookingType;

  private final CourtCentre courtCentre;

  private final String courtScheduleId;

  private final ZonedDateTime earliestStartDateTime;

  private final Integer estimateMinutes;

  private final String estimatedDuration;

  private final HearingType hearingType;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final List<ListDefendantRequest> listDefendantRequests;

  private final ZonedDateTime listedEndDateTime;

  private final ZonedDateTime listedStartDateTime;

  private final String listingDirections;

  private final List<NonDefaultDay> nonDefaultDays;

  private final String priority;

  private final String prosecutorDatesToAvoid;

  private final String reportingRestrictionReason;

  private final List<String> specialRequirements;

  private final WeekCommencingDate weekCommencingDate;

  @JsonCreator
  public ListHearingRequest(final List<RotaSlot> bookedSlots, final String bookingType, final CourtCentre courtCentre, final String courtScheduleId, final ZonedDateTime earliestStartDateTime, final Integer estimateMinutes, final String estimatedDuration, final HearingType hearingType, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final List<ListDefendantRequest> listDefendantRequests, final ZonedDateTime listedEndDateTime, final ZonedDateTime listedStartDateTime, final String listingDirections, final List<NonDefaultDay> nonDefaultDays, final String priority, final String prosecutorDatesToAvoid, final String reportingRestrictionReason, final List<String> specialRequirements, final WeekCommencingDate weekCommencingDate) {
    this.bookedSlots = bookedSlots;
    this.bookingType = bookingType;
    this.courtCentre = courtCentre;
    this.courtScheduleId = courtScheduleId;
    this.earliestStartDateTime = earliestStartDateTime;
    this.estimateMinutes = estimateMinutes;
    this.estimatedDuration = estimatedDuration;
    this.hearingType = hearingType;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.listDefendantRequests = listDefendantRequests;
    this.listedEndDateTime = listedEndDateTime;
    this.listedStartDateTime = listedStartDateTime;
    this.listingDirections = listingDirections;
    this.nonDefaultDays = nonDefaultDays;
    this.priority = priority;
    this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.specialRequirements = specialRequirements;
    this.weekCommencingDate = weekCommencingDate;
  }

  public List<RotaSlot> getBookedSlots() {
    return bookedSlots;
  }

  public String getBookingType() {
    return bookingType;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public String getCourtScheduleId() {
    return courtScheduleId;
  }

  public ZonedDateTime getEarliestStartDateTime() {
    return earliestStartDateTime;
  }

  public Integer getEstimateMinutes() {
    return estimateMinutes;
  }

  public String getEstimatedDuration() {
    return estimatedDuration;
  }

  public HearingType getHearingType() {
    return hearingType;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public List<ListDefendantRequest> getListDefendantRequests() {
    return listDefendantRequests;
  }

  public ZonedDateTime getListedEndDateTime() {
    return listedEndDateTime;
  }

  public ZonedDateTime getListedStartDateTime() {
    return listedStartDateTime;
  }

  public String getListingDirections() {
    return listingDirections;
  }

  public List<NonDefaultDay> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public String getPriority() {
    return priority;
  }

  public String getProsecutorDatesToAvoid() {
    return prosecutorDatesToAvoid;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public List<String> getSpecialRequirements() {
    return specialRequirements;
  }

  public WeekCommencingDate getWeekCommencingDate() {
    return weekCommencingDate;
  }

  public static Builder listHearingRequest() {
    return new uk.gov.justice.core.courts.ListHearingRequest.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ListHearingRequest that = (uk.gov.justice.core.courts.ListHearingRequest) obj;

    return java.util.Objects.equals(this.bookedSlots, that.bookedSlots) &&
    java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
    java.util.Objects.equals(this.earliestStartDateTime, that.earliestStartDateTime) &&
    java.util.Objects.equals(this.estimateMinutes, that.estimateMinutes) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.hearingType, that.hearingType) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.listDefendantRequests, that.listDefendantRequests) &&
    java.util.Objects.equals(this.listedEndDateTime, that.listedEndDateTime) &&
    java.util.Objects.equals(this.listedStartDateTime, that.listedStartDateTime) &&
    java.util.Objects.equals(this.listingDirections, that.listingDirections) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.prosecutorDatesToAvoid, that.prosecutorDatesToAvoid) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.weekCommencingDate, that.weekCommencingDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookedSlots, bookingType, courtCentre, courtScheduleId, earliestStartDateTime, estimateMinutes, estimatedDuration, hearingType, judiciary, jurisdictionType, listDefendantRequests, listedEndDateTime, listedStartDateTime, listingDirections, nonDefaultDays, priority, prosecutorDatesToAvoid, reportingRestrictionReason, specialRequirements, weekCommencingDate);}

  public static class Builder {
    private List<RotaSlot> bookedSlots;

    private String bookingType;

    private CourtCentre courtCentre;

    private String courtScheduleId;

    private ZonedDateTime earliestStartDateTime;

    private Integer estimateMinutes;

    private String estimatedDuration;

    private HearingType hearingType;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private List<ListDefendantRequest> listDefendantRequests;

    private ZonedDateTime listedEndDateTime;

    private ZonedDateTime listedStartDateTime;

    private String listingDirections;

    private List<NonDefaultDay> nonDefaultDays;

    private String priority;

    private String prosecutorDatesToAvoid;

    private String reportingRestrictionReason;

    private List<String> specialRequirements;

    private WeekCommencingDate weekCommencingDate;

    public Builder withBookedSlots(final List<RotaSlot> bookedSlots) {
      this.bookedSlots = bookedSlots;
      return this;
    }

    public Builder withBookingType(final String bookingType) {
      this.bookingType = bookingType;
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

    public Builder withEarliestStartDateTime(final ZonedDateTime earliestStartDateTime) {
      this.earliestStartDateTime = earliestStartDateTime;
      return this;
    }

    public Builder withEstimateMinutes(final Integer estimateMinutes) {
      this.estimateMinutes = estimateMinutes;
      return this;
    }

    public Builder withEstimatedDuration(final String estimatedDuration) {
      this.estimatedDuration = estimatedDuration;
      return this;
    }

    public Builder withHearingType(final HearingType hearingType) {
      this.hearingType = hearingType;
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

    public Builder withListDefendantRequests(final List<ListDefendantRequest> listDefendantRequests) {
      this.listDefendantRequests = listDefendantRequests;
      return this;
    }

    public Builder withListedEndDateTime(final ZonedDateTime listedEndDateTime) {
      this.listedEndDateTime = listedEndDateTime;
      return this;
    }

    public Builder withListedStartDateTime(final ZonedDateTime listedStartDateTime) {
      this.listedStartDateTime = listedStartDateTime;
      return this;
    }

    public Builder withListingDirections(final String listingDirections) {
      this.listingDirections = listingDirections;
      return this;
    }

    public Builder withNonDefaultDays(final List<NonDefaultDay> nonDefaultDays) {
      this.nonDefaultDays = nonDefaultDays;
      return this;
    }

    public Builder withPriority(final String priority) {
      this.priority = priority;
      return this;
    }

    public Builder withProsecutorDatesToAvoid(final String prosecutorDatesToAvoid) {
      this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
      return this;
    }

    public Builder withReportingRestrictionReason(final String reportingRestrictionReason) {
      this.reportingRestrictionReason = reportingRestrictionReason;
      return this;
    }

    public Builder withSpecialRequirements(final List<String> specialRequirements) {
      this.specialRequirements = specialRequirements;
      return this;
    }

    public Builder withWeekCommencingDate(final WeekCommencingDate weekCommencingDate) {
      this.weekCommencingDate = weekCommencingDate;
      return this;
    }

    public Builder withValuesFrom(final ListHearingRequest listHearingRequest) {
      this.bookedSlots = listHearingRequest.getBookedSlots();
      this.bookingType = listHearingRequest.getBookingType();
      this.courtCentre = listHearingRequest.getCourtCentre();
      this.courtScheduleId = listHearingRequest.getCourtScheduleId();
      this.earliestStartDateTime = listHearingRequest.getEarliestStartDateTime();
      this.estimateMinutes = listHearingRequest.getEstimateMinutes();
      this.estimatedDuration = listHearingRequest.getEstimatedDuration();
      this.hearingType = listHearingRequest.getHearingType();
      this.judiciary = listHearingRequest.getJudiciary();
      this.jurisdictionType = listHearingRequest.getJurisdictionType();
      this.listDefendantRequests = listHearingRequest.getListDefendantRequests();
      this.listedEndDateTime = listHearingRequest.getListedEndDateTime();
      this.listedStartDateTime = listHearingRequest.getListedStartDateTime();
      this.listingDirections = listHearingRequest.getListingDirections();
      this.nonDefaultDays = listHearingRequest.getNonDefaultDays();
      this.priority = listHearingRequest.getPriority();
      this.prosecutorDatesToAvoid = listHearingRequest.getProsecutorDatesToAvoid();
      this.reportingRestrictionReason = listHearingRequest.getReportingRestrictionReason();
      this.specialRequirements = listHearingRequest.getSpecialRequirements();
      this.weekCommencingDate = listHearingRequest.getWeekCommencingDate();
      return this;
    }

    public ListHearingRequest build() {
      return new uk.gov.justice.core.courts.ListHearingRequest(bookedSlots, bookingType, courtCentre, courtScheduleId, earliestStartDateTime, estimateMinutes, estimatedDuration, hearingType, judiciary, jurisdictionType, listDefendantRequests, listedEndDateTime, listedStartDateTime, listingDirections, nonDefaultDays, priority, prosecutorDatesToAvoid, reportingRestrictionReason, specialRequirements, weekCommencingDate);
    }
  }
}

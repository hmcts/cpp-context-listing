package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class CourtHearingRequest implements Serializable {
  private static final long serialVersionUID = 4799816782246862590L;

  private final List<RotaSlot> bookedSlots;

  private final String bookingType;

  private final CourtCentre courtCentre;

  private final ZonedDateTime earliestStartDateTime;

  private final String endDate;

  private final String estimatedDuration;

  private final Integer estimatedMinutes;

  private final HearingType hearingType;

  private final UUID id;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final List<ListDefendantRequest> listDefendantRequests;

  private final ZonedDateTime listedStartDateTime;

  private final String listingDirections;

  private final List<NonDefaultDay> nonDefaultDays;

  private final String priority;

  private final List<String> specialRequirements;

  private final WeekCommencingDate weekCommencingDate;

  @JsonCreator
  public CourtHearingRequest(final List<RotaSlot> bookedSlots, final String bookingType, final CourtCentre courtCentre, final ZonedDateTime earliestStartDateTime, final String endDate, final String estimatedDuration, final Integer estimatedMinutes, final HearingType hearingType, final UUID id, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final List<ListDefendantRequest> listDefendantRequests, final ZonedDateTime listedStartDateTime, final String listingDirections, final List<NonDefaultDay> nonDefaultDays, final String priority, final List<String> specialRequirements, final WeekCommencingDate weekCommencingDate) {
    this.bookedSlots = bookedSlots;
    this.bookingType = bookingType;
    this.courtCentre = courtCentre;
    this.earliestStartDateTime = earliestStartDateTime;
    this.endDate = endDate;
    this.estimatedDuration = estimatedDuration;
    this.estimatedMinutes = estimatedMinutes;
    this.hearingType = hearingType;
    this.id = id;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.listDefendantRequests = listDefendantRequests;
    this.listedStartDateTime = listedStartDateTime;
    this.listingDirections = listingDirections;
    this.nonDefaultDays = nonDefaultDays;
    this.priority = priority;
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

  public ZonedDateTime getEarliestStartDateTime() {
    return earliestStartDateTime;
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

  public HearingType getHearingType() {
    return hearingType;
  }

  public UUID getId() {
    return id;
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

  public List<String> getSpecialRequirements() {
    return specialRequirements;
  }

  public WeekCommencingDate getWeekCommencingDate() {
    return weekCommencingDate;
  }

  public static Builder courtHearingRequest() {
    return new uk.gov.justice.core.courts.CourtHearingRequest.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtHearingRequest that = (uk.gov.justice.core.courts.CourtHearingRequest) obj;

    return java.util.Objects.equals(this.bookedSlots, that.bookedSlots) &&
    java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.earliestStartDateTime, that.earliestStartDateTime) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.hearingType, that.hearingType) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.listDefendantRequests, that.listDefendantRequests) &&
    java.util.Objects.equals(this.listedStartDateTime, that.listedStartDateTime) &&
    java.util.Objects.equals(this.listingDirections, that.listingDirections) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.weekCommencingDate, that.weekCommencingDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookedSlots, bookingType, courtCentre, earliestStartDateTime, endDate, estimatedDuration, estimatedMinutes, hearingType, id, judiciary, jurisdictionType, listDefendantRequests, listedStartDateTime, listingDirections, nonDefaultDays, priority, specialRequirements, weekCommencingDate);}

  public static class Builder {
    private List<RotaSlot> bookedSlots;

    private String bookingType;

    private CourtCentre courtCentre;

    private ZonedDateTime earliestStartDateTime;

    private String endDate;

    private String estimatedDuration;

    private Integer estimatedMinutes;

    private HearingType hearingType;

    private UUID id;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private List<ListDefendantRequest> listDefendantRequests;

    private ZonedDateTime listedStartDateTime;

    private String listingDirections;

    private List<NonDefaultDay> nonDefaultDays;

    private String priority;

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

    public Builder withEarliestStartDateTime(final ZonedDateTime earliestStartDateTime) {
      this.earliestStartDateTime = earliestStartDateTime;
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

    public Builder withHearingType(final HearingType hearingType) {
      this.hearingType = hearingType;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
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

    public Builder withSpecialRequirements(final List<String> specialRequirements) {
      this.specialRequirements = specialRequirements;
      return this;
    }

    public Builder withWeekCommencingDate(final WeekCommencingDate weekCommencingDate) {
      this.weekCommencingDate = weekCommencingDate;
      return this;
    }

    public Builder withValuesFrom(final CourtHearingRequest courtHearingRequest) {
      this.bookedSlots = courtHearingRequest.getBookedSlots();
      this.bookingType = courtHearingRequest.getBookingType();
      this.courtCentre = courtHearingRequest.getCourtCentre();
      this.earliestStartDateTime = courtHearingRequest.getEarliestStartDateTime();
      this.endDate = courtHearingRequest.getEndDate();
      this.estimatedDuration = courtHearingRequest.getEstimatedDuration();
      this.estimatedMinutes = courtHearingRequest.getEstimatedMinutes();
      this.hearingType = courtHearingRequest.getHearingType();
      this.id = courtHearingRequest.getId();
      this.judiciary = courtHearingRequest.getJudiciary();
      this.jurisdictionType = courtHearingRequest.getJurisdictionType();
      this.listDefendantRequests = courtHearingRequest.getListDefendantRequests();
      this.listedStartDateTime = courtHearingRequest.getListedStartDateTime();
      this.listingDirections = courtHearingRequest.getListingDirections();
      this.nonDefaultDays = courtHearingRequest.getNonDefaultDays();
      this.priority = courtHearingRequest.getPriority();
      this.specialRequirements = courtHearingRequest.getSpecialRequirements();
      this.weekCommencingDate = courtHearingRequest.getWeekCommencingDate();
      return this;
    }

    public CourtHearingRequest build() {
      return new uk.gov.justice.core.courts.CourtHearingRequest(bookedSlots, bookingType, courtCentre, earliestStartDateTime, endDate, estimatedDuration, estimatedMinutes, hearingType, id, judiciary, jurisdictionType, listDefendantRequests, listedStartDateTime, listingDirections, nonDefaultDays, priority, specialRequirements, weekCommencingDate);
    }
  }
}

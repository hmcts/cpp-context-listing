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
import uk.gov.justice.listing.courts.TypeOfList;

public class HearingUnscheduledListingNeeds implements Serializable {
  private static final long serialVersionUID = -8073545902110665383L;

  private final UUID bookingReference;

  private final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds;

  private final List<CourtApplication> courtApplications;

  private final CourtCentre courtCentre;

  private final List<DefendantListingNeeds> defendantListingNeeds;

  private final ZonedDateTime earliestStartDateTime;

  private final String endDate;

  private final String estimatedDuration;

  private final Integer estimatedMinutes;

  private final UUID id;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final ZonedDateTime listedStartDateTime;

  private final String listingDirections;

  private final List<ProsecutionCase> prosecutionCases;

  private final String prosecutorDatesToAvoid;

  private final String reportingRestrictionReason;

  private final HearingType type;

  private final TypeOfList typeOfList;

  private final WeekCommencingDate weekCommencingDate;

  @JsonCreator
  public HearingUnscheduledListingNeeds(final UUID bookingReference, final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds, final List<CourtApplication> courtApplications, final CourtCentre courtCentre, final List<DefendantListingNeeds> defendantListingNeeds, final ZonedDateTime earliestStartDateTime, final String endDate, final String estimatedDuration, final Integer estimatedMinutes, final UUID id, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final ZonedDateTime listedStartDateTime, final String listingDirections, final List<ProsecutionCase> prosecutionCases, final String prosecutorDatesToAvoid, final String reportingRestrictionReason, final HearingType type, final TypeOfList typeOfList, final WeekCommencingDate weekCommencingDate) {
    this.bookingReference = bookingReference;
    this.courtApplicationPartyListingNeeds = courtApplicationPartyListingNeeds;
    this.courtApplications = courtApplications;
    this.courtCentre = courtCentre;
    this.defendantListingNeeds = defendantListingNeeds;
    this.earliestStartDateTime = earliestStartDateTime;
    this.endDate = endDate;
    this.estimatedDuration = estimatedDuration;
    this.estimatedMinutes = estimatedMinutes;
    this.id = id;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.listedStartDateTime = listedStartDateTime;
    this.listingDirections = listingDirections;
    this.prosecutionCases = prosecutionCases;
    this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.type = type;
    this.typeOfList = typeOfList;
    this.weekCommencingDate = weekCommencingDate;
  }

  public UUID getBookingReference() {
    return bookingReference;
  }

  public List<CourtApplicationPartyListingNeeds> getCourtApplicationPartyListingNeeds() {
    return courtApplicationPartyListingNeeds;
  }

  public List<CourtApplication> getCourtApplications() {
    return courtApplications;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public List<DefendantListingNeeds> getDefendantListingNeeds() {
    return defendantListingNeeds;
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

  public UUID getId() {
    return id;
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

  public String getListingDirections() {
    return listingDirections;
  }

  public List<ProsecutionCase> getProsecutionCases() {
    return prosecutionCases;
  }

  public String getProsecutorDatesToAvoid() {
    return prosecutorDatesToAvoid;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public HearingType getType() {
    return type;
  }

  public TypeOfList getTypeOfList() {
    return typeOfList;
  }

  public WeekCommencingDate getWeekCommencingDate() {
    return weekCommencingDate;
  }

  public static Builder hearingUnscheduledListingNeeds() {
    return new uk.gov.justice.core.courts.HearingUnscheduledListingNeeds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.HearingUnscheduledListingNeeds that = (uk.gov.justice.core.courts.HearingUnscheduledListingNeeds) obj;

    return java.util.Objects.equals(this.bookingReference, that.bookingReference) &&
    java.util.Objects.equals(this.courtApplicationPartyListingNeeds, that.courtApplicationPartyListingNeeds) &&
    java.util.Objects.equals(this.courtApplications, that.courtApplications) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.defendantListingNeeds, that.defendantListingNeeds) &&
    java.util.Objects.equals(this.earliestStartDateTime, that.earliestStartDateTime) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.listedStartDateTime, that.listedStartDateTime) &&
    java.util.Objects.equals(this.listingDirections, that.listingDirections) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.prosecutorDatesToAvoid, that.prosecutorDatesToAvoid) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.typeOfList, that.typeOfList) &&
    java.util.Objects.equals(this.weekCommencingDate, that.weekCommencingDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookingReference, courtApplicationPartyListingNeeds, courtApplications, courtCentre, defendantListingNeeds, earliestStartDateTime, endDate, estimatedDuration, estimatedMinutes, id, judiciary, jurisdictionType, listedStartDateTime, listingDirections, prosecutionCases, prosecutorDatesToAvoid, reportingRestrictionReason, type, typeOfList, weekCommencingDate);}

  public static class Builder {
    private UUID bookingReference;

    private List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds;

    private List<CourtApplication> courtApplications;

    private CourtCentre courtCentre;

    private List<DefendantListingNeeds> defendantListingNeeds;

    private ZonedDateTime earliestStartDateTime;

    private String endDate;

    private String estimatedDuration;

    private Integer estimatedMinutes;

    private UUID id;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private ZonedDateTime listedStartDateTime;

    private String listingDirections;

    private List<ProsecutionCase> prosecutionCases;

    private String prosecutorDatesToAvoid;

    private String reportingRestrictionReason;

    private HearingType type;

    private TypeOfList typeOfList;

    private WeekCommencingDate weekCommencingDate;

    public Builder withBookingReference(final UUID bookingReference) {
      this.bookingReference = bookingReference;
      return this;
    }

    public Builder withCourtApplicationPartyListingNeeds(final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds) {
      this.courtApplicationPartyListingNeeds = courtApplicationPartyListingNeeds;
      return this;
    }

    public Builder withCourtApplications(final List<CourtApplication> courtApplications) {
      this.courtApplications = courtApplications;
      return this;
    }

    public Builder withCourtCentre(final CourtCentre courtCentre) {
      this.courtCentre = courtCentre;
      return this;
    }

    public Builder withDefendantListingNeeds(final List<DefendantListingNeeds> defendantListingNeeds) {
      this.defendantListingNeeds = defendantListingNeeds;
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

    public Builder withListedStartDateTime(final ZonedDateTime listedStartDateTime) {
      this.listedStartDateTime = listedStartDateTime;
      return this;
    }

    public Builder withListingDirections(final String listingDirections) {
      this.listingDirections = listingDirections;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCase> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
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

    public Builder withType(final HearingType type) {
      this.type = type;
      return this;
    }

    public Builder withTypeOfList(final TypeOfList typeOfList) {
      this.typeOfList = typeOfList;
      return this;
    }

    public Builder withWeekCommencingDate(final WeekCommencingDate weekCommencingDate) {
      this.weekCommencingDate = weekCommencingDate;
      return this;
    }

    public Builder withValuesFrom(final HearingUnscheduledListingNeeds hearingUnscheduledListingNeeds) {
      this.bookingReference = hearingUnscheduledListingNeeds.getBookingReference();
      this.courtApplicationPartyListingNeeds = hearingUnscheduledListingNeeds.getCourtApplicationPartyListingNeeds();
      this.courtApplications = hearingUnscheduledListingNeeds.getCourtApplications();
      this.courtCentre = hearingUnscheduledListingNeeds.getCourtCentre();
      this.defendantListingNeeds = hearingUnscheduledListingNeeds.getDefendantListingNeeds();
      this.earliestStartDateTime = hearingUnscheduledListingNeeds.getEarliestStartDateTime();
      this.endDate = hearingUnscheduledListingNeeds.getEndDate();
      this.estimatedDuration = hearingUnscheduledListingNeeds.getEstimatedDuration();
      this.estimatedMinutes = hearingUnscheduledListingNeeds.getEstimatedMinutes();
      this.id = hearingUnscheduledListingNeeds.getId();
      this.judiciary = hearingUnscheduledListingNeeds.getJudiciary();
      this.jurisdictionType = hearingUnscheduledListingNeeds.getJurisdictionType();
      this.listedStartDateTime = hearingUnscheduledListingNeeds.getListedStartDateTime();
      this.listingDirections = hearingUnscheduledListingNeeds.getListingDirections();
      this.prosecutionCases = hearingUnscheduledListingNeeds.getProsecutionCases();
      this.prosecutorDatesToAvoid = hearingUnscheduledListingNeeds.getProsecutorDatesToAvoid();
      this.reportingRestrictionReason = hearingUnscheduledListingNeeds.getReportingRestrictionReason();
      this.type = hearingUnscheduledListingNeeds.getType();
      this.typeOfList = hearingUnscheduledListingNeeds.getTypeOfList();
      this.weekCommencingDate = hearingUnscheduledListingNeeds.getWeekCommencingDate();
      return this;
    }

    public HearingUnscheduledListingNeeds build() {
      return new uk.gov.justice.core.courts.HearingUnscheduledListingNeeds(bookingReference, courtApplicationPartyListingNeeds, courtApplications, courtCentre, defendantListingNeeds, earliestStartDateTime, endDate, estimatedDuration, estimatedMinutes, id, judiciary, jurisdictionType, listedStartDateTime, listingDirections, prosecutionCases, prosecutorDatesToAvoid, reportingRestrictionReason, type, typeOfList, weekCommencingDate);
    }
  }
}

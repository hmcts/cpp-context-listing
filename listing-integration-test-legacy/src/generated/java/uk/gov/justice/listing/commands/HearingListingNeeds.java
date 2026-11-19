package uk.gov.justice.listing.commands;

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
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NonDefaultDay;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.WeekCommencingDate;

public class HearingListingNeeds implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final List<RotaSlot> bookedSlots;

  private final UUID bookingReference;

  private final String bookingType;

  private final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds;

  private final List<CourtApplication> courtApplications;

  private final CourtCentre courtCentre;

  private final List<DefendantListingNeeds> defendantListingNeeds;

  private final ZonedDateTime earliestStartDateTime;

  private final String endDate;

  private final String estimatedDuration;

  private final Integer estimatedMinutes;

  private final List<HearingDay> hearingDays;

  private final UUID id;

  private final Boolean isGroupProceedings;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final String keyReason;

  private final String listType;

  private final ZonedDateTime listedStartDateTime;

  private final String listingDirections;

  private final List<NonDefaultDay> nonDefaultDays;

  private final List<String> nonSittingDays;

  private final Integer numberOfGroupCases;

  private final String priority;

  private final List<ProsecutionCase> prosecutionCases;

  private final String prosecutorDatesToAvoid;

  private final String reportingRestrictionReason;

  private final List<String> specialRequirements;

  private final String tier;

  private final HearingType type;

  private final WeekCommencingDate weekCommencingDate;

  @JsonCreator
  public HearingListingNeeds(final List<RotaSlot> bookedSlots, final UUID bookingReference, final String bookingType, final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds, final List<CourtApplication> courtApplications, final CourtCentre courtCentre, final List<DefendantListingNeeds> defendantListingNeeds, final ZonedDateTime earliestStartDateTime, final String endDate, final String estimatedDuration, final Integer estimatedMinutes, final List<HearingDay> hearingDays, final UUID id, final Boolean isGroupProceedings, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final String keyReason, final String listType, final ZonedDateTime listedStartDateTime, final String listingDirections, final List<NonDefaultDay> nonDefaultDays, final List<String> nonSittingDays, final Integer numberOfGroupCases, final String priority, final List<ProsecutionCase> prosecutionCases, final String prosecutorDatesToAvoid, final String reportingRestrictionReason, final List<String> specialRequirements, final String tier, final HearingType type, final WeekCommencingDate weekCommencingDate) {
    this.bookedSlots = bookedSlots;
    this.bookingReference = bookingReference;
    this.bookingType = bookingType;
    this.courtApplicationPartyListingNeeds = courtApplicationPartyListingNeeds;
    this.courtApplications = courtApplications;
    this.courtCentre = courtCentre;
    this.defendantListingNeeds = defendantListingNeeds;
    this.earliestStartDateTime = earliestStartDateTime;
    this.endDate = endDate;
    this.estimatedDuration = estimatedDuration;
    this.estimatedMinutes = estimatedMinutes;
    this.hearingDays = hearingDays;
    this.id = id;
    this.isGroupProceedings = isGroupProceedings;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.keyReason = keyReason;
    this.listType = listType;
    this.listedStartDateTime = listedStartDateTime;
    this.listingDirections = listingDirections;
    this.nonDefaultDays = nonDefaultDays;
    this.nonSittingDays = nonSittingDays;
    this.numberOfGroupCases = numberOfGroupCases;
    this.priority = priority;
    this.prosecutionCases = prosecutionCases;
    this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.specialRequirements = specialRequirements;
    this.tier = tier;
    this.type = type;
    this.weekCommencingDate = weekCommencingDate;
  }

  public List<RotaSlot> getBookedSlots() {
    return bookedSlots;
  }

  public UUID getBookingReference() {
    return bookingReference;
  }

  public String getBookingType() {
    return bookingType;
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

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getId() {
    return id;
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

  public String getKeyReason() {
    return keyReason;
  }

  public String getListType() {
    return listType;
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

  public List<String> getNonSittingDays() {
    return nonSittingDays;
  }

  public Integer getNumberOfGroupCases() {
    return numberOfGroupCases;
  }

  public String getPriority() {
    return priority;
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

  public List<String> getSpecialRequirements() {
    return specialRequirements;
  }

  public String getTier() {
    return tier;
  }

  public HearingType getType() {
    return type;
  }

  public WeekCommencingDate getWeekCommencingDate() {
    return weekCommencingDate;
  }

  public static Builder hearingListingNeeds() {
    return new uk.gov.justice.listing.commands.HearingListingNeeds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.HearingListingNeeds that = (uk.gov.justice.listing.commands.HearingListingNeeds) obj;

    return java.util.Objects.equals(this.bookedSlots, that.bookedSlots) &&
    java.util.Objects.equals(this.bookingReference, that.bookingReference) &&
    java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.courtApplicationPartyListingNeeds, that.courtApplicationPartyListingNeeds) &&
    java.util.Objects.equals(this.courtApplications, that.courtApplications) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.defendantListingNeeds, that.defendantListingNeeds) &&
    java.util.Objects.equals(this.earliestStartDateTime, that.earliestStartDateTime) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isGroupProceedings, that.isGroupProceedings) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.listedStartDateTime, that.listedStartDateTime) &&
    java.util.Objects.equals(this.listingDirections, that.listingDirections) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.nonSittingDays, that.nonSittingDays) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.prosecutorDatesToAvoid, that.prosecutorDatesToAvoid) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.tier, that.tier) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.weekCommencingDate, that.weekCommencingDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookedSlots, bookingReference, bookingType, courtApplicationPartyListingNeeds, courtApplications, courtCentre, defendantListingNeeds, earliestStartDateTime, endDate, estimatedDuration, estimatedMinutes, hearingDays, id, isGroupProceedings, judiciary, jurisdictionType, keyReason, listType, listedStartDateTime, listingDirections, nonDefaultDays, nonSittingDays, numberOfGroupCases, priority, prosecutionCases, prosecutorDatesToAvoid, reportingRestrictionReason, specialRequirements, tier, type, weekCommencingDate);}

  public static class Builder {
    private List<RotaSlot> bookedSlots;

    private UUID bookingReference;

    private String bookingType;

    private List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds;

    private List<CourtApplication> courtApplications;

    private CourtCentre courtCentre;

    private List<DefendantListingNeeds> defendantListingNeeds;

    private ZonedDateTime earliestStartDateTime;

    private String endDate;

    private String estimatedDuration;

    private Integer estimatedMinutes;

    private List<HearingDay> hearingDays;

    private UUID id;

    private Boolean isGroupProceedings;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private String keyReason;

    private String listType;

    private ZonedDateTime listedStartDateTime;

    private String listingDirections;

    private List<NonDefaultDay> nonDefaultDays;

    private List<String> nonSittingDays;

    private Integer numberOfGroupCases;

    private String priority;

    private List<ProsecutionCase> prosecutionCases;

    private String prosecutorDatesToAvoid;

    private String reportingRestrictionReason;

    private List<String> specialRequirements;

    private String tier;

    private HearingType type;

    private WeekCommencingDate weekCommencingDate;

    public Builder withBookedSlots(final List<RotaSlot> bookedSlots) {
      this.bookedSlots = bookedSlots;
      return this;
    }

    public Builder withBookingReference(final UUID bookingReference) {
      this.bookingReference = bookingReference;
      return this;
    }

    public Builder withBookingType(final String bookingType) {
      this.bookingType = bookingType;
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

    public Builder withHearingDays(final List<HearingDay> hearingDays) {
      this.hearingDays = hearingDays;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
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

    public Builder withKeyReason(final String keyReason) {
      this.keyReason = keyReason;
      return this;
    }

    public Builder withListType(final String listType) {
      this.listType = listType;
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

    public Builder withNonSittingDays(final List<String> nonSittingDays) {
      this.nonSittingDays = nonSittingDays;
      return this;
    }

    public Builder withNumberOfGroupCases(final Integer numberOfGroupCases) {
      this.numberOfGroupCases = numberOfGroupCases;
      return this;
    }

    public Builder withPriority(final String priority) {
      this.priority = priority;
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

    public Builder withSpecialRequirements(final List<String> specialRequirements) {
      this.specialRequirements = specialRequirements;
      return this;
    }

    public Builder withTier(final String tier) {
      this.tier = tier;
      return this;
    }

    public Builder withType(final HearingType type) {
      this.type = type;
      return this;
    }

    public Builder withWeekCommencingDate(final WeekCommencingDate weekCommencingDate) {
      this.weekCommencingDate = weekCommencingDate;
      return this;
    }

    public Builder withValuesFrom(final HearingListingNeeds hearingListingNeeds) {
      this.bookedSlots = hearingListingNeeds.getBookedSlots();
      this.bookingReference = hearingListingNeeds.getBookingReference();
      this.bookingType = hearingListingNeeds.getBookingType();
      this.courtApplicationPartyListingNeeds = hearingListingNeeds.getCourtApplicationPartyListingNeeds();
      this.courtApplications = hearingListingNeeds.getCourtApplications();
      this.courtCentre = hearingListingNeeds.getCourtCentre();
      this.defendantListingNeeds = hearingListingNeeds.getDefendantListingNeeds();
      this.earliestStartDateTime = hearingListingNeeds.getEarliestStartDateTime();
      this.endDate = hearingListingNeeds.getEndDate();
      this.estimatedDuration = hearingListingNeeds.getEstimatedDuration();
      this.estimatedMinutes = hearingListingNeeds.getEstimatedMinutes();
      this.hearingDays = hearingListingNeeds.getHearingDays();
      this.id = hearingListingNeeds.getId();
      this.isGroupProceedings = hearingListingNeeds.getIsGroupProceedings();
      this.judiciary = hearingListingNeeds.getJudiciary();
      this.jurisdictionType = hearingListingNeeds.getJurisdictionType();
      this.keyReason = hearingListingNeeds.getKeyReason();
      this.listType = hearingListingNeeds.getListType();
      this.listedStartDateTime = hearingListingNeeds.getListedStartDateTime();
      this.listingDirections = hearingListingNeeds.getListingDirections();
      this.nonDefaultDays = hearingListingNeeds.getNonDefaultDays();
      this.nonSittingDays = hearingListingNeeds.getNonSittingDays();
      this.numberOfGroupCases = hearingListingNeeds.getNumberOfGroupCases();
      this.priority = hearingListingNeeds.getPriority();
      this.prosecutionCases = hearingListingNeeds.getProsecutionCases();
      this.prosecutorDatesToAvoid = hearingListingNeeds.getProsecutorDatesToAvoid();
      this.reportingRestrictionReason = hearingListingNeeds.getReportingRestrictionReason();
      this.specialRequirements = hearingListingNeeds.getSpecialRequirements();
      this.tier = hearingListingNeeds.getTier();
      this.type = hearingListingNeeds.getType();
      this.weekCommencingDate = hearingListingNeeds.getWeekCommencingDate();
      return this;
    }

    public HearingListingNeeds build() {
      return new uk.gov.justice.listing.commands.HearingListingNeeds(bookedSlots, bookingReference, bookingType, courtApplicationPartyListingNeeds, courtApplications, courtCentre, defendantListingNeeds, earliestStartDateTime, endDate, estimatedDuration, estimatedMinutes, hearingDays, id, isGroupProceedings, judiciary, jurisdictionType, keyReason, listType, listedStartDateTime, listingDirections, nonDefaultDays, nonSittingDays, numberOfGroupCases, priority, prosecutionCases, prosecutorDatesToAvoid, reportingRestrictionReason, specialRequirements, tier, type, weekCommencingDate);
    }
  }
}

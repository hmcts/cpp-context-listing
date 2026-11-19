package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCounsel;

public class Hearing implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final String adjournedFromDate;

  private final Boolean allocated;

  private final String bookingType;

  private final List<CourtApplication> courtApplications;

  private final CourtCentreDetails courtCentreDetails;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final List<DefenceCounsel> defenceCounsels;

  private final LocalDate endDate;

  private final String estimatedDuration;

  private final Integer estimatedMinutes;

  private final Boolean hasVideoLink;

  private final List<HearingDay> hearingDays;

  private final HearingLanguage hearingLanguage;

  private final UUID id;

  private final Boolean isGroupProceedings;

  private final Boolean isPossibleDisqualification;

  private final Boolean isSlotsBooked;

  private final Boolean isVacated;

  private final List<JudicialRole> judiciary;

  private final JudiciaryAssignmentSource judiciaryAssignmentSource;

  private final JurisdictionType jurisdictionType;

  private final String keyReason;

  private final String listType;

  private final List<ListedCase> listedCases;

  private final String listingDirections;

  private final List<NonDefaultDay> nonDefaultDays;

  private final List<LocalDate> nonSittingDays;

  private final Integer numberOfGroupCases;

  private final String priority;

  private final List<ProsecutionCounsel> prosecutionCounsels;

  private final String prosecutorDatesToAvoid;

  private final String publicListNote;

  private final String reportingRestrictionReason;

  private final Integer sequence;

  private final List<String> specialRequirements;

  private final LocalDate startDate;

  private final String tier;

  private final Type type;

  private final TypeOfList typeOfList;

  private final Boolean unscheduled;

  private final String vacatedTrialReasonId;

  private final Integer weekCommencingDurationInWeeks;

  private final LocalDate weekCommencingEndDate;

  private final LocalDate weekCommencingStartDate;

  @JsonCreator
  public Hearing(final String adjournedFromDate, final Boolean allocated, final String bookingType, final List<CourtApplication> courtApplications, final CourtCentreDetails courtCentreDetails, final UUID courtCentreId, final UUID courtRoomId, final List<DefenceCounsel> defenceCounsels, final LocalDate endDate, final String estimatedDuration, final Integer estimatedMinutes, final Boolean hasVideoLink, final List<HearingDay> hearingDays, final HearingLanguage hearingLanguage, final UUID id, final Boolean isGroupProceedings, final Boolean isPossibleDisqualification, final Boolean isSlotsBooked, final Boolean isVacated, final List<JudicialRole> judiciary, final JudiciaryAssignmentSource judiciaryAssignmentSource, final JurisdictionType jurisdictionType, final String keyReason, final String listType, final List<ListedCase> listedCases, final String listingDirections, final List<NonDefaultDay> nonDefaultDays, final List<LocalDate> nonSittingDays, final Integer numberOfGroupCases, final String priority, final List<ProsecutionCounsel> prosecutionCounsels, final String prosecutorDatesToAvoid, final String publicListNote, final String reportingRestrictionReason, final Integer sequence, final List<String> specialRequirements, final LocalDate startDate, final String tier, final Type type, final TypeOfList typeOfList, final Boolean unscheduled, final String vacatedTrialReasonId, final Integer weekCommencingDurationInWeeks, final LocalDate weekCommencingEndDate, final LocalDate weekCommencingStartDate) {
    this.adjournedFromDate = adjournedFromDate;
    this.allocated = allocated;
    this.bookingType = bookingType;
    this.courtApplications = courtApplications;
    this.courtCentreDetails = courtCentreDetails;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.defenceCounsels = defenceCounsels;
    this.endDate = endDate;
    this.estimatedDuration = estimatedDuration;
    this.estimatedMinutes = estimatedMinutes;
    this.hasVideoLink = hasVideoLink;
    this.hearingDays = hearingDays;
    this.hearingLanguage = hearingLanguage;
    this.id = id;
    this.isGroupProceedings = isGroupProceedings;
    this.isPossibleDisqualification = isPossibleDisqualification;
    this.isSlotsBooked = isSlotsBooked;
    this.isVacated = isVacated;
    this.judiciary = judiciary;
    this.judiciaryAssignmentSource = judiciaryAssignmentSource;
    this.jurisdictionType = jurisdictionType;
    this.keyReason = keyReason;
    this.listType = listType;
    this.listedCases = listedCases;
    this.listingDirections = listingDirections;
    this.nonDefaultDays = nonDefaultDays;
    this.nonSittingDays = nonSittingDays;
    this.numberOfGroupCases = numberOfGroupCases;
    this.priority = priority;
    this.prosecutionCounsels = prosecutionCounsels;
    this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
    this.publicListNote = publicListNote;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.sequence = sequence;
    this.specialRequirements = specialRequirements;
    this.startDate = startDate;
    this.tier = tier;
    this.type = type;
    this.typeOfList = typeOfList;
    this.unscheduled = unscheduled;
    this.vacatedTrialReasonId = vacatedTrialReasonId;
    this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;
    this.weekCommencingEndDate = weekCommencingEndDate;
    this.weekCommencingStartDate = weekCommencingStartDate;
  }

  public String getAdjournedFromDate() {
    return adjournedFromDate;
  }

  public Boolean getAllocated() {
    return allocated;
  }

  public String getBookingType() {
    return bookingType;
  }

  public List<CourtApplication> getCourtApplications() {
    return courtApplications;
  }

  public CourtCentreDetails getCourtCentreDetails() {
    return courtCentreDetails;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public List<DefenceCounsel> getDefenceCounsels() {
    return defenceCounsels;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getEstimatedDuration() {
    return estimatedDuration;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public Boolean getHasVideoLink() {
    return hasVideoLink;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsGroupProceedings() {
    return isGroupProceedings;
  }

  public Boolean getIsPossibleDisqualification() {
    return isPossibleDisqualification;
  }

  public Boolean getIsSlotsBooked() {
    return isSlotsBooked;
  }

  public Boolean getIsVacated() {
    return isVacated;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JudiciaryAssignmentSource getJudiciaryAssignmentSource() {
    return judiciaryAssignmentSource;
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

  public List<ListedCase> getListedCases() {
    return listedCases;
  }

  public String getListingDirections() {
    return listingDirections;
  }

  public List<NonDefaultDay> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public List<LocalDate> getNonSittingDays() {
    return nonSittingDays;
  }

  public Integer getNumberOfGroupCases() {
    return numberOfGroupCases;
  }

  public String getPriority() {
    return priority;
  }

  public List<ProsecutionCounsel> getProsecutionCounsels() {
    return prosecutionCounsels;
  }

  public String getProsecutorDatesToAvoid() {
    return prosecutorDatesToAvoid;
  }

  public String getPublicListNote() {
    return publicListNote;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public Integer getSequence() {
    return sequence;
  }

  public List<String> getSpecialRequirements() {
    return specialRequirements;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public String getTier() {
    return tier;
  }

  public Type getType() {
    return type;
  }

  public TypeOfList getTypeOfList() {
    return typeOfList;
  }

  public Boolean getUnscheduled() {
    return unscheduled;
  }

  public String getVacatedTrialReasonId() {
    return vacatedTrialReasonId;
  }

  public Integer getWeekCommencingDurationInWeeks() {
    return weekCommencingDurationInWeeks;
  }

  public LocalDate getWeekCommencingEndDate() {
    return weekCommencingEndDate;
  }

  public LocalDate getWeekCommencingStartDate() {
    return weekCommencingStartDate;
  }

  public static Builder hearing() {
    return new uk.gov.justice.listing.events.Hearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.Hearing that = (uk.gov.justice.listing.events.Hearing) obj;

    return java.util.Objects.equals(this.adjournedFromDate, that.adjournedFromDate) &&
    java.util.Objects.equals(this.allocated, that.allocated) &&
    java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.courtApplications, that.courtApplications) &&
    java.util.Objects.equals(this.courtCentreDetails, that.courtCentreDetails) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.defenceCounsels, that.defenceCounsels) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.hasVideoLink, that.hasVideoLink) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isGroupProceedings, that.isGroupProceedings) &&
    java.util.Objects.equals(this.isPossibleDisqualification, that.isPossibleDisqualification) &&
    java.util.Objects.equals(this.isSlotsBooked, that.isSlotsBooked) &&
    java.util.Objects.equals(this.isVacated, that.isVacated) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.judiciaryAssignmentSource, that.judiciaryAssignmentSource) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.listedCases, that.listedCases) &&
    java.util.Objects.equals(this.listingDirections, that.listingDirections) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.nonSittingDays, that.nonSittingDays) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.prosecutionCounsels, that.prosecutionCounsels) &&
    java.util.Objects.equals(this.prosecutorDatesToAvoid, that.prosecutorDatesToAvoid) &&
    java.util.Objects.equals(this.publicListNote, that.publicListNote) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.sequence, that.sequence) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.tier, that.tier) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.typeOfList, that.typeOfList) &&
    java.util.Objects.equals(this.unscheduled, that.unscheduled) &&
    java.util.Objects.equals(this.vacatedTrialReasonId, that.vacatedTrialReasonId) &&
    java.util.Objects.equals(this.weekCommencingDurationInWeeks, that.weekCommencingDurationInWeeks) &&
    java.util.Objects.equals(this.weekCommencingEndDate, that.weekCommencingEndDate) &&
    java.util.Objects.equals(this.weekCommencingStartDate, that.weekCommencingStartDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(adjournedFromDate, allocated, bookingType, courtApplications, courtCentreDetails, courtCentreId, courtRoomId, defenceCounsels, endDate, estimatedDuration, estimatedMinutes, hasVideoLink, hearingDays, hearingLanguage, id, isGroupProceedings, isPossibleDisqualification, isSlotsBooked, isVacated, judiciary, judiciaryAssignmentSource, jurisdictionType, keyReason, listType, listedCases, listingDirections, nonDefaultDays, nonSittingDays, numberOfGroupCases, priority, prosecutionCounsels, prosecutorDatesToAvoid, publicListNote, reportingRestrictionReason, sequence, specialRequirements, startDate, tier, type, typeOfList, unscheduled, vacatedTrialReasonId, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);}

  public static class Builder {
    private String adjournedFromDate;

    private Boolean allocated;

    private String bookingType;

    private List<CourtApplication> courtApplications;

    private CourtCentreDetails courtCentreDetails;

    private UUID courtCentreId;

    private UUID courtRoomId;

    private List<DefenceCounsel> defenceCounsels;

    private LocalDate endDate;

    private String estimatedDuration;

    private Integer estimatedMinutes;

    private Boolean hasVideoLink;

    private List<HearingDay> hearingDays;

    private HearingLanguage hearingLanguage;

    private UUID id;

    private Boolean isGroupProceedings;

    private Boolean isPossibleDisqualification;

    private Boolean isSlotsBooked;

    private Boolean isVacated;

    private List<JudicialRole> judiciary;

    private JudiciaryAssignmentSource judiciaryAssignmentSource;

    private JurisdictionType jurisdictionType;

    private String keyReason;

    private String listType;

    private List<ListedCase> listedCases;

    private String listingDirections;

    private List<NonDefaultDay> nonDefaultDays;

    private List<LocalDate> nonSittingDays;

    private Integer numberOfGroupCases;

    private String priority;

    private List<ProsecutionCounsel> prosecutionCounsels;

    private String prosecutorDatesToAvoid;

    private String publicListNote;

    private String reportingRestrictionReason;

    private Integer sequence;

    private List<String> specialRequirements;

    private LocalDate startDate;

    private String tier;

    private Type type;

    private TypeOfList typeOfList;

    private Boolean unscheduled;

    private String vacatedTrialReasonId;

    private Integer weekCommencingDurationInWeeks;

    private LocalDate weekCommencingEndDate;

    private LocalDate weekCommencingStartDate;

    public Builder withAdjournedFromDate(final String adjournedFromDate) {
      this.adjournedFromDate = adjournedFromDate;
      return this;
    }

    public Builder withAllocated(final Boolean allocated) {
      this.allocated = allocated;
      return this;
    }

    public Builder withBookingType(final String bookingType) {
      this.bookingType = bookingType;
      return this;
    }

    public Builder withCourtApplications(final List<CourtApplication> courtApplications) {
      this.courtApplications = courtApplications;
      return this;
    }

    public Builder withCourtCentreDetails(final CourtCentreDetails courtCentreDetails) {
      this.courtCentreDetails = courtCentreDetails;
      return this;
    }

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withDefenceCounsels(final List<DefenceCounsel> defenceCounsels) {
      this.defenceCounsels = defenceCounsels;
      return this;
    }

    public Builder withEndDate(final LocalDate endDate) {
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

    public Builder withHasVideoLink(final Boolean hasVideoLink) {
      this.hasVideoLink = hasVideoLink;
      return this;
    }

    public Builder withHearingDays(final List<HearingDay> hearingDays) {
      this.hearingDays = hearingDays;
      return this;
    }

    public Builder withHearingLanguage(final HearingLanguage hearingLanguage) {
      this.hearingLanguage = hearingLanguage;
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

    public Builder withIsPossibleDisqualification(final Boolean isPossibleDisqualification) {
      this.isPossibleDisqualification = isPossibleDisqualification;
      return this;
    }

    public Builder withIsSlotsBooked(final Boolean isSlotsBooked) {
      this.isSlotsBooked = isSlotsBooked;
      return this;
    }

    public Builder withIsVacated(final Boolean isVacated) {
      this.isVacated = isVacated;
      return this;
    }

    public Builder withJudiciary(final List<JudicialRole> judiciary) {
      this.judiciary = judiciary;
      return this;
    }

    public Builder withJudiciaryAssignmentSource(final JudiciaryAssignmentSource judiciaryAssignmentSource) {
      this.judiciaryAssignmentSource = judiciaryAssignmentSource;
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

    public Builder withListedCases(final List<ListedCase> listedCases) {
      this.listedCases = listedCases;
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

    public Builder withNonSittingDays(final List<LocalDate> nonSittingDays) {
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

    public Builder withProsecutionCounsels(final List<ProsecutionCounsel> prosecutionCounsels) {
      this.prosecutionCounsels = prosecutionCounsels;
      return this;
    }

    public Builder withProsecutorDatesToAvoid(final String prosecutorDatesToAvoid) {
      this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
      return this;
    }

    public Builder withPublicListNote(final String publicListNote) {
      this.publicListNote = publicListNote;
      return this;
    }

    public Builder withReportingRestrictionReason(final String reportingRestrictionReason) {
      this.reportingRestrictionReason = reportingRestrictionReason;
      return this;
    }

    public Builder withSequence(final Integer sequence) {
      this.sequence = sequence;
      return this;
    }

    public Builder withSpecialRequirements(final List<String> specialRequirements) {
      this.specialRequirements = specialRequirements;
      return this;
    }

    public Builder withStartDate(final LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withTier(final String tier) {
      this.tier = tier;
      return this;
    }

    public Builder withType(final Type type) {
      this.type = type;
      return this;
    }

    public Builder withTypeOfList(final TypeOfList typeOfList) {
      this.typeOfList = typeOfList;
      return this;
    }

    public Builder withUnscheduled(final Boolean unscheduled) {
      this.unscheduled = unscheduled;
      return this;
    }

    public Builder withVacatedTrialReasonId(final String vacatedTrialReasonId) {
      this.vacatedTrialReasonId = vacatedTrialReasonId;
      return this;
    }

    public Builder withWeekCommencingDurationInWeeks(final Integer weekCommencingDurationInWeeks) {
      this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;
      return this;
    }

    public Builder withWeekCommencingEndDate(final LocalDate weekCommencingEndDate) {
      this.weekCommencingEndDate = weekCommencingEndDate;
      return this;
    }

    public Builder withWeekCommencingStartDate(final LocalDate weekCommencingStartDate) {
      this.weekCommencingStartDate = weekCommencingStartDate;
      return this;
    }

    public Builder withValuesFrom(final Hearing hearing) {
      this.adjournedFromDate = hearing.getAdjournedFromDate();
      this.allocated = hearing.getAllocated();
      this.bookingType = hearing.getBookingType();
      this.courtApplications = hearing.getCourtApplications();
      this.courtCentreDetails = hearing.getCourtCentreDetails();
      this.courtCentreId = hearing.getCourtCentreId();
      this.courtRoomId = hearing.getCourtRoomId();
      this.defenceCounsels = hearing.getDefenceCounsels();
      this.endDate = hearing.getEndDate();
      this.estimatedDuration = hearing.getEstimatedDuration();
      this.estimatedMinutes = hearing.getEstimatedMinutes();
      this.hasVideoLink = hearing.getHasVideoLink();
      this.hearingDays = hearing.getHearingDays();
      this.hearingLanguage = hearing.getHearingLanguage();
      this.id = hearing.getId();
      this.isGroupProceedings = hearing.getIsGroupProceedings();
      this.isPossibleDisqualification = hearing.getIsPossibleDisqualification();
      this.isSlotsBooked = hearing.getIsSlotsBooked();
      this.isVacated = hearing.getIsVacated();
      this.judiciary = hearing.getJudiciary();
      this.judiciaryAssignmentSource = hearing.getJudiciaryAssignmentSource();
      this.jurisdictionType = hearing.getJurisdictionType();
      this.keyReason = hearing.getKeyReason();
      this.listType = hearing.getListType();
      this.listedCases = hearing.getListedCases();
      this.listingDirections = hearing.getListingDirections();
      this.nonDefaultDays = hearing.getNonDefaultDays();
      this.nonSittingDays = hearing.getNonSittingDays();
      this.numberOfGroupCases = hearing.getNumberOfGroupCases();
      this.priority = hearing.getPriority();
      this.prosecutionCounsels = hearing.getProsecutionCounsels();
      this.prosecutorDatesToAvoid = hearing.getProsecutorDatesToAvoid();
      this.publicListNote = hearing.getPublicListNote();
      this.reportingRestrictionReason = hearing.getReportingRestrictionReason();
      this.sequence = hearing.getSequence();
      this.specialRequirements = hearing.getSpecialRequirements();
      this.startDate = hearing.getStartDate();
      this.tier = hearing.getTier();
      this.type = hearing.getType();
      this.typeOfList = hearing.getTypeOfList();
      this.unscheduled = hearing.getUnscheduled();
      this.vacatedTrialReasonId = hearing.getVacatedTrialReasonId();
      this.weekCommencingDurationInWeeks = hearing.getWeekCommencingDurationInWeeks();
      this.weekCommencingEndDate = hearing.getWeekCommencingEndDate();
      this.weekCommencingStartDate = hearing.getWeekCommencingStartDate();
      return this;
    }

    public Hearing build() {
      return new uk.gov.justice.listing.events.Hearing(adjournedFromDate, allocated, bookingType, courtApplications, courtCentreDetails, courtCentreId, courtRoomId, defenceCounsels, endDate, estimatedDuration, estimatedMinutes, hasVideoLink, hearingDays, hearingLanguage, id, isGroupProceedings, isPossibleDisqualification, isSlotsBooked, isVacated, judiciary, judiciaryAssignmentSource, jurisdictionType, keyReason, listType, listedCases, listingDirections, nonDefaultDays, nonSittingDays, numberOfGroupCases, priority, prosecutionCounsels, prosecutorDatesToAvoid, publicListNote, reportingRestrictionReason, sequence, specialRequirements, startDate, tier, type, typeOfList, unscheduled, vacatedTrialReasonId, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);
    }
  }
}

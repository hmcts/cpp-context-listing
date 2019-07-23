package uk.gov.moj.cpp.listing.domain;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067" })
public class Hearing {
  private final Boolean allocated;

  private final UUID courtCentreId;

  private final Optional<UUID> courtRoomId;

  private final Optional<LocalDate> endDate;

  private final Integer estimatedMinutes;

  private final List<HearingDay> hearingDays;

  private final Optional<HearingLanguage> hearingLanguage;

  private final UUID id;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final List<ListedCase> listedCases;

  private final Optional<String> listingDirections;

  private final List<NonDefaultDay> nonDefaultDays;

  private final List<LocalDate> nonSittingDays;

  private final Optional<String> prosecutorDatesToAvoid;

  private final Optional<String> reportingRestrictionReason;

  private final Optional<Integer> sequence;

  private final ZonedDateTime startDateTime;

  private final Type type;

  private final List<CourtApplication> courtApplications;

  private final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds;

  public Hearing(final Boolean allocated, final UUID courtCentreId, final Optional<UUID> courtRoomId, final Optional<LocalDate> endDate, final Integer estimatedMinutes, final List<HearingDay> hearingDays, final Optional<HearingLanguage> hearingLanguage, final UUID id, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final List<ListedCase> listedCases, final Optional<String> listingDirections, final List<NonDefaultDay> nonDefaultDays, final List<LocalDate> nonSittingDays, final Optional<String> prosecutorDatesToAvoid, final Optional<String> reportingRestrictionReason, final Optional<Integer> sequence, final ZonedDateTime startDateTime,
                 final Type type, final List<CourtApplication> courtApplications, final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds) {
    this.allocated = allocated;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.endDate = endDate;
    this.estimatedMinutes = estimatedMinutes;
    this.hearingDays = hearingDays;
    this.hearingLanguage = hearingLanguage;
    this.id = id;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.listedCases = listedCases;
    this.listingDirections = listingDirections;
    this.nonDefaultDays = nonDefaultDays;
    this.nonSittingDays = nonSittingDays;
    this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.sequence = sequence;
    this.startDateTime = startDateTime;
    this.type = type;
    this.courtApplications = courtApplications;
    this.courtApplicationPartyListingNeeds = courtApplicationPartyListingNeeds;
  }

  public Boolean getAllocated() {
    return allocated;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public Optional<UUID> getCourtRoomId() {
    return courtRoomId;
  }

  public Optional<LocalDate> getEndDate() {
    return endDate;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public Optional<HearingLanguage> getHearingLanguage() {
    return hearingLanguage;
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

  public List<ListedCase> getListedCases() {
    return listedCases;
  }

  public Optional<String> getListingDirections() {
    return listingDirections;
  }

  public List<NonDefaultDay> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public List<LocalDate> getNonSittingDays() {
    return nonSittingDays;
  }

  public Optional<String> getProsecutorDatesToAvoid() {
    return prosecutorDatesToAvoid;
  }

  public Optional<String> getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public Optional<Integer> getSequence() {
    return sequence;
  }

  public ZonedDateTime getStartDateTime() {
    return startDateTime;
  }

  public Type getType() {
    return type;
  }

  public List<CourtApplication> getCourtApplications() { return courtApplications; }

  public List<CourtApplicationPartyListingNeeds> getCourtApplicationPartyListingNeeds() {
    return courtApplicationPartyListingNeeds;
  }

  public static Builder hearing() {
    return new Hearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Hearing that = (Hearing) obj;

    return java.util.Objects.equals(this.allocated, that.allocated) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.listedCases, that.listedCases) &&
    java.util.Objects.equals(this.listingDirections, that.listingDirections) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.nonSittingDays, that.nonSittingDays) &&
    java.util.Objects.equals(this.prosecutorDatesToAvoid, that.prosecutorDatesToAvoid) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.sequence, that.sequence) &&
    java.util.Objects.equals(this.startDateTime, that.startDateTime) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.courtApplications, that.courtApplications) &&
    java.util.Objects.equals(this.courtApplicationPartyListingNeeds, that.courtApplicationPartyListingNeeds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocated, courtCentreId, courtRoomId, endDate, estimatedMinutes, hearingDays, hearingLanguage, id, judiciary, jurisdictionType, listedCases, listingDirections, nonDefaultDays, nonSittingDays, prosecutorDatesToAvoid, reportingRestrictionReason, sequence, startDateTime, type, courtApplications, courtApplicationPartyListingNeeds);}

  @Override
  public String toString() {
    return "Hearing{" +
    	"allocated='" + allocated + "'," +
    	"courtCentreId='" + courtCentreId + "'," +
    	"courtRoomId='" + courtRoomId + "'," +
    	"endDate='" + endDate + "'," +
    	"estimatedMinutes='" + estimatedMinutes + "'," +
    	"hearingDays='" + hearingDays + "'," +
    	"hearingLanguage='" + hearingLanguage + "'," +
    	"id='" + id + "'," +
    	"judiciary='" + judiciary + "'," +
    	"jurisdictionType='" + jurisdictionType + "'," +
    	"listedCases='" + listedCases + "'," +
    	"listingDirections='" + listingDirections + "'," +
    	"nonDefaultDays='" + nonDefaultDays + "'," +
    	"nonSittingDays='" + nonSittingDays + "'," +
    	"prosecutorDatesToAvoid='" + prosecutorDatesToAvoid + "'," +
    	"reportingRestrictionReason='" + reportingRestrictionReason + "'," +
    	"sequence='" + sequence + "'," +
    	"startDateTime='" + startDateTime + "'," +
    	"type='" + type + "'," +
        "courtApplications='" + courtApplications + "'" +
            "courtApplicationPartyNeeds='" + courtApplicationPartyListingNeeds + "'" +
    "}";
  }

  public static class Builder {
    private Boolean allocated;

    private UUID courtCentreId;

    private Optional<UUID> courtRoomId;

    private Optional<LocalDate> endDate;

    private Integer estimatedMinutes;

    private List<HearingDay> hearingDays;

    private Optional<HearingLanguage> hearingLanguage;

    private UUID id;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private List<ListedCase> listedCases;

    private Optional<String> listingDirections;

    private List<NonDefaultDay> nonDefaultDays;

    private List<LocalDate> nonSittingDays;

    private Optional<String> prosecutorDatesToAvoid;

    private Optional<String> reportingRestrictionReason;

    private Optional<Integer> sequence;

    private ZonedDateTime startDateTime;

    private Type type;

    private List<CourtApplication> courtApplications;

    private List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds;

    public Builder withAllocated(final Boolean allocated) {
      this.allocated = allocated;
      return this;
    }

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final Optional<UUID> courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withEndDate(final Optional<LocalDate> endDate) {
      this.endDate = endDate;
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

    public Builder withHearingLanguage(final Optional<HearingLanguage> hearingLanguage) {
      this.hearingLanguage = hearingLanguage;
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

    public Builder withListedCases(final List<ListedCase> listedCases) {
      this.listedCases = listedCases;
      return this;
    }

    public Builder withListingDirections(final Optional<String> listingDirections) {
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

    public Builder withProsecutorDatesToAvoid(final Optional<String> prosecutorDatesToAvoid) {
      this.prosecutorDatesToAvoid = prosecutorDatesToAvoid;
      return this;
    }

    public Builder withReportingRestrictionReason(final Optional<String> reportingRestrictionReason) {
      this.reportingRestrictionReason = reportingRestrictionReason;
      return this;
    }

    public Builder withSequence(final Optional<Integer> sequence) {
      this.sequence = sequence;
      return this;
    }

    public Builder withStartDateTime(final ZonedDateTime startDateTime) {
      this.startDateTime = startDateTime;
      return this;
    }

    public Builder withType(final Type type) {
      this.type = type;
      return this;
    }

    public Builder withCourtApplication(final List<CourtApplication> courtApplications) {
      this.courtApplications = courtApplications;
      return this;
    }
    public Builder withCourtApplicationPartyNeeds(final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds) {
      this.courtApplicationPartyListingNeeds = courtApplicationPartyListingNeeds;
      return this;
    }
    public Hearing build() {
      return new Hearing(allocated, courtCentreId, courtRoomId, endDate, estimatedMinutes, hearingDays, hearingLanguage, id, judiciary, jurisdictionType, listedCases, listingDirections, nonDefaultDays, nonSittingDays, prosecutorDatesToAvoid, reportingRestrictionReason, sequence, startDateTime, type, courtApplications, this.courtApplicationPartyListingNeeds);
    }
  }
}

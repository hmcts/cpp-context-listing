package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.allocated-hearing-extended-for-listing-v2")
public class AllocatedHearingExtendedForListingV2 implements Serializable {
  private static final long serialVersionUID = -221456124008145363L;

  private final List<UUID> courtApplicationIds;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final Integer estimatedMinutes;

  private final UUID existingHearingId;

  private final Boolean fullExtension;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  private final HearingLanguage hearingLanguage;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantsOffenceIds;

  private final String reportingRestrictionReason;

  private final Boolean sendNotificationToParties;

  private final Type type;

  private final List<ListedCase> unAllocatedListedCases;

  @JsonCreator
  public AllocatedHearingExtendedForListingV2(final List<UUID> courtApplicationIds, final UUID courtCentreId, final UUID courtRoomId, final Integer estimatedMinutes, final UUID existingHearingId, final Boolean fullExtension, final List<HearingDay> hearingDays, final UUID hearingId, final HearingLanguage hearingLanguage, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantsOffenceIds, final String reportingRestrictionReason, final Boolean sendNotificationToParties, final Type type, final List<ListedCase> unAllocatedListedCases) {
    this.courtApplicationIds = courtApplicationIds;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.estimatedMinutes = estimatedMinutes;
    this.existingHearingId = existingHearingId;
    this.fullExtension = fullExtension;
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
    this.hearingLanguage = hearingLanguage;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.prosecutionCaseDefendantsOffenceIds = prosecutionCaseDefendantsOffenceIds;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.sendNotificationToParties = sendNotificationToParties;
    this.type = type;
    this.unAllocatedListedCases = unAllocatedListedCases;
  }

  public List<UUID> getCourtApplicationIds() {
    return courtApplicationIds;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public UUID getExistingHearingId() {
    return existingHearingId;
  }

  public Boolean getFullExtension() {
    return fullExtension;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public List<ProsecutionCaseDefendantOffenceIds> getProsecutionCaseDefendantsOffenceIds() {
    return prosecutionCaseDefendantsOffenceIds;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public Type getType() {
    return type;
  }

  public List<ListedCase> getUnAllocatedListedCases() {
    return unAllocatedListedCases;
  }

  public static Builder allocatedHearingExtendedForListingV2() {
    return new uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2 that = (uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2) obj;

    return java.util.Objects.equals(this.courtApplicationIds, that.courtApplicationIds) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.existingHearingId, that.existingHearingId) &&
    java.util.Objects.equals(this.fullExtension, that.fullExtension) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.prosecutionCaseDefendantsOffenceIds, that.prosecutionCaseDefendantsOffenceIds) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.unAllocatedListedCases, that.unAllocatedListedCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplicationIds, courtCentreId, courtRoomId, estimatedMinutes, existingHearingId, fullExtension, hearingDays, hearingId, hearingLanguage, judiciary, jurisdictionType, prosecutionCaseDefendantsOffenceIds, reportingRestrictionReason, sendNotificationToParties, type, unAllocatedListedCases);}

  public static class Builder {
    private List<UUID> courtApplicationIds;

    private UUID courtCentreId;

    private UUID courtRoomId;

    private Integer estimatedMinutes;

    private UUID existingHearingId;

    private Boolean fullExtension;

    private List<HearingDay> hearingDays;

    private UUID hearingId;

    private HearingLanguage hearingLanguage;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantsOffenceIds;

    private String reportingRestrictionReason;

    private Boolean sendNotificationToParties;

    private Type type;

    private List<ListedCase> unAllocatedListedCases;

    public Builder withCourtApplicationIds(final List<UUID> courtApplicationIds) {
      this.courtApplicationIds = courtApplicationIds;
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

    public Builder withEstimatedMinutes(final Integer estimatedMinutes) {
      this.estimatedMinutes = estimatedMinutes;
      return this;
    }

    public Builder withExistingHearingId(final UUID existingHearingId) {
      this.existingHearingId = existingHearingId;
      return this;
    }

    public Builder withFullExtension(final Boolean fullExtension) {
      this.fullExtension = fullExtension;
      return this;
    }

    public Builder withHearingDays(final List<HearingDay> hearingDays) {
      this.hearingDays = hearingDays;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withHearingLanguage(final HearingLanguage hearingLanguage) {
      this.hearingLanguage = hearingLanguage;
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

    public Builder withProsecutionCaseDefendantsOffenceIds(final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantsOffenceIds) {
      this.prosecutionCaseDefendantsOffenceIds = prosecutionCaseDefendantsOffenceIds;
      return this;
    }

    public Builder withReportingRestrictionReason(final String reportingRestrictionReason) {
      this.reportingRestrictionReason = reportingRestrictionReason;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withType(final Type type) {
      this.type = type;
      return this;
    }

    public Builder withUnAllocatedListedCases(final List<ListedCase> unAllocatedListedCases) {
      this.unAllocatedListedCases = unAllocatedListedCases;
      return this;
    }

    public Builder withValuesFrom(final AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListingV2) {
      this.courtApplicationIds = allocatedHearingExtendedForListingV2.getCourtApplicationIds();
      this.courtCentreId = allocatedHearingExtendedForListingV2.getCourtCentreId();
      this.courtRoomId = allocatedHearingExtendedForListingV2.getCourtRoomId();
      this.estimatedMinutes = allocatedHearingExtendedForListingV2.getEstimatedMinutes();
      this.existingHearingId = allocatedHearingExtendedForListingV2.getExistingHearingId();
      this.fullExtension = allocatedHearingExtendedForListingV2.getFullExtension();
      this.hearingDays = allocatedHearingExtendedForListingV2.getHearingDays();
      this.hearingId = allocatedHearingExtendedForListingV2.getHearingId();
      this.hearingLanguage = allocatedHearingExtendedForListingV2.getHearingLanguage();
      this.judiciary = allocatedHearingExtendedForListingV2.getJudiciary();
      this.jurisdictionType = allocatedHearingExtendedForListingV2.getJurisdictionType();
      this.prosecutionCaseDefendantsOffenceIds = allocatedHearingExtendedForListingV2.getProsecutionCaseDefendantsOffenceIds();
      this.reportingRestrictionReason = allocatedHearingExtendedForListingV2.getReportingRestrictionReason();
      this.sendNotificationToParties = allocatedHearingExtendedForListingV2.getSendNotificationToParties();
      this.type = allocatedHearingExtendedForListingV2.getType();
      this.unAllocatedListedCases = allocatedHearingExtendedForListingV2.getUnAllocatedListedCases();
      return this;
    }

    public AllocatedHearingExtendedForListingV2 build() {
      return new uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2(courtApplicationIds, courtCentreId, courtRoomId, estimatedMinutes, existingHearingId, fullExtension, hearingDays, hearingId, hearingLanguage, judiciary, jurisdictionType, prosecutionCaseDefendantsOffenceIds, reportingRestrictionReason, sendNotificationToParties, type, unAllocatedListedCases);
    }
  }
}

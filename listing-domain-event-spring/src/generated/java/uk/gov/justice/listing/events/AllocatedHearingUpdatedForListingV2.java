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

@Event("listing.events.allocated-hearing-updated-for-listing-v2")
public class AllocatedHearingUpdatedForListingV2 implements Serializable {
  private static final long serialVersionUID = -1565312484075012412L;

  private final List<UUID> courtApplicationIds;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final Integer estimatedMinutes;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  private final HearingLanguage hearingLanguage;

  private final Boolean isNotificationAllocationFieldUpdated;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final Integer numberOfGroupCases;

  private final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantsOffenceIds;

  private final String reportingRestrictionReason;

  private final Boolean sendNotificationToParties;

  private final String source;

  private final Type type;

  private final Boolean updateSlot;

  @JsonCreator
  public AllocatedHearingUpdatedForListingV2(final List<UUID> courtApplicationIds, final UUID courtCentreId, final UUID courtRoomId, final Integer estimatedMinutes, final List<HearingDay> hearingDays, final UUID hearingId, final HearingLanguage hearingLanguage, final Boolean isNotificationAllocationFieldUpdated, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final Integer numberOfGroupCases, final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantsOffenceIds, final String reportingRestrictionReason, final Boolean sendNotificationToParties, final String source, final Type type, final Boolean updateSlot) {
    this.courtApplicationIds = courtApplicationIds;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.estimatedMinutes = estimatedMinutes;
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
    this.hearingLanguage = hearingLanguage;
    this.isNotificationAllocationFieldUpdated = isNotificationAllocationFieldUpdated;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.numberOfGroupCases = numberOfGroupCases;
    this.prosecutionCaseDefendantsOffenceIds = prosecutionCaseDefendantsOffenceIds;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.sendNotificationToParties = sendNotificationToParties;
    this.source = source;
    this.type = type;
    this.updateSlot = updateSlot;
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

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
  }

  public Boolean getIsNotificationAllocationFieldUpdated() {
    return isNotificationAllocationFieldUpdated;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public Integer getNumberOfGroupCases() {
    return numberOfGroupCases;
  }

  public List<ProsecutionCaseDefendantOffenceIdsV2> getProsecutionCaseDefendantsOffenceIds() {
    return prosecutionCaseDefendantsOffenceIds;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public String getSource() {
    return source;
  }

  public Type getType() {
    return type;
  }

  public Boolean getUpdateSlot() {
    return updateSlot;
  }

  public static Builder allocatedHearingUpdatedForListingV2() {
    return new uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2 that = (uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2) obj;

    return java.util.Objects.equals(this.courtApplicationIds, that.courtApplicationIds) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.isNotificationAllocationFieldUpdated, that.isNotificationAllocationFieldUpdated) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.prosecutionCaseDefendantsOffenceIds, that.prosecutionCaseDefendantsOffenceIds) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.source, that.source) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.updateSlot, that.updateSlot);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplicationIds, courtCentreId, courtRoomId, estimatedMinutes, hearingDays, hearingId, hearingLanguage, isNotificationAllocationFieldUpdated, judiciary, jurisdictionType, numberOfGroupCases, prosecutionCaseDefendantsOffenceIds, reportingRestrictionReason, sendNotificationToParties, source, type, updateSlot);}

  public static class Builder {
    private List<UUID> courtApplicationIds;

    private UUID courtCentreId;

    private UUID courtRoomId;

    private Integer estimatedMinutes;

    private List<HearingDay> hearingDays;

    private UUID hearingId;

    private HearingLanguage hearingLanguage;

    private Boolean isNotificationAllocationFieldUpdated;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private Integer numberOfGroupCases;

    private List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantsOffenceIds;

    private String reportingRestrictionReason;

    private Boolean sendNotificationToParties;

    private String source;

    private Type type;

    private Boolean updateSlot;

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

    public Builder withIsNotificationAllocationFieldUpdated(final Boolean isNotificationAllocationFieldUpdated) {
      this.isNotificationAllocationFieldUpdated = isNotificationAllocationFieldUpdated;
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

    public Builder withNumberOfGroupCases(final Integer numberOfGroupCases) {
      this.numberOfGroupCases = numberOfGroupCases;
      return this;
    }

    public Builder withProsecutionCaseDefendantsOffenceIds(final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantsOffenceIds) {
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

    public Builder withSource(final String source) {
      this.source = source;
      return this;
    }

    public Builder withType(final Type type) {
      this.type = type;
      return this;
    }

    public Builder withUpdateSlot(final Boolean updateSlot) {
      this.updateSlot = updateSlot;
      return this;
    }

    public Builder withValuesFrom(final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListingV2) {
      this.courtApplicationIds = allocatedHearingUpdatedForListingV2.getCourtApplicationIds();
      this.courtCentreId = allocatedHearingUpdatedForListingV2.getCourtCentreId();
      this.courtRoomId = allocatedHearingUpdatedForListingV2.getCourtRoomId();
      this.estimatedMinutes = allocatedHearingUpdatedForListingV2.getEstimatedMinutes();
      this.hearingDays = allocatedHearingUpdatedForListingV2.getHearingDays();
      this.hearingId = allocatedHearingUpdatedForListingV2.getHearingId();
      this.hearingLanguage = allocatedHearingUpdatedForListingV2.getHearingLanguage();
      this.isNotificationAllocationFieldUpdated = allocatedHearingUpdatedForListingV2.getIsNotificationAllocationFieldUpdated();
      this.judiciary = allocatedHearingUpdatedForListingV2.getJudiciary();
      this.jurisdictionType = allocatedHearingUpdatedForListingV2.getJurisdictionType();
      this.numberOfGroupCases = allocatedHearingUpdatedForListingV2.getNumberOfGroupCases();
      this.prosecutionCaseDefendantsOffenceIds = allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds();
      this.reportingRestrictionReason = allocatedHearingUpdatedForListingV2.getReportingRestrictionReason();
      this.sendNotificationToParties = allocatedHearingUpdatedForListingV2.getSendNotificationToParties();
      this.source = allocatedHearingUpdatedForListingV2.getSource();
      this.type = allocatedHearingUpdatedForListingV2.getType();
      this.updateSlot = allocatedHearingUpdatedForListingV2.getUpdateSlot();
      return this;
    }

    public AllocatedHearingUpdatedForListingV2 build() {
      return new uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2(courtApplicationIds, courtCentreId, courtRoomId, estimatedMinutes, hearingDays, hearingId, hearingLanguage, isNotificationAllocationFieldUpdated, judiciary, jurisdictionType, numberOfGroupCases, prosecutionCaseDefendantsOffenceIds, reportingRestrictionReason, sendNotificationToParties, source, type, updateSlot);
    }
  }
}

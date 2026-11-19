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

@Event("listing.events.hearing-allocated-for-listing")
public class HearingAllocatedForListing implements Serializable {
  private static final long serialVersionUID = 481348104142252561L;

  private final UUID bookingId;

  private final List<UUID> courtApplicationIds;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final String estimatedDuration;

  private final Integer estimatedMinutes;

  private final Boolean hasAdjournmentDate;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  private final HearingLanguage hearingLanguage;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantsOffenceIds;

  private final String reportingRestrictionReason;

  private final String source;

  private final Type type;

  private final Boolean updateSlot;

  @JsonCreator
  public HearingAllocatedForListing(final UUID bookingId, final List<UUID> courtApplicationIds, final UUID courtCentreId, final UUID courtRoomId, final String estimatedDuration, final Integer estimatedMinutes, final Boolean hasAdjournmentDate, final List<HearingDay> hearingDays, final UUID hearingId, final HearingLanguage hearingLanguage, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantsOffenceIds, final String reportingRestrictionReason, final String source, final Type type, final Boolean updateSlot) {
    this.bookingId = bookingId;
    this.courtApplicationIds = courtApplicationIds;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.estimatedDuration = estimatedDuration;
    this.estimatedMinutes = estimatedMinutes;
    this.hasAdjournmentDate = hasAdjournmentDate;
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
    this.hearingLanguage = hearingLanguage;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.prosecutionCaseDefendantsOffenceIds = prosecutionCaseDefendantsOffenceIds;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.source = source;
    this.type = type;
    this.updateSlot = updateSlot;
  }

  public UUID getBookingId() {
    return bookingId;
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

  public String getEstimatedDuration() {
    return estimatedDuration;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public Boolean getHasAdjournmentDate() {
    return hasAdjournmentDate;
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

  public String getSource() {
    return source;
  }

  public Type getType() {
    return type;
  }

  public Boolean getUpdateSlot() {
    return updateSlot;
  }

  public static Builder hearingAllocatedForListing() {
    return new uk.gov.justice.listing.events.HearingAllocatedForListing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingAllocatedForListing that = (uk.gov.justice.listing.events.HearingAllocatedForListing) obj;

    return java.util.Objects.equals(this.bookingId, that.bookingId) &&
    java.util.Objects.equals(this.courtApplicationIds, that.courtApplicationIds) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.hasAdjournmentDate, that.hasAdjournmentDate) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.prosecutionCaseDefendantsOffenceIds, that.prosecutionCaseDefendantsOffenceIds) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.source, that.source) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.updateSlot, that.updateSlot);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookingId, courtApplicationIds, courtCentreId, courtRoomId, estimatedDuration, estimatedMinutes, hasAdjournmentDate, hearingDays, hearingId, hearingLanguage, judiciary, jurisdictionType, prosecutionCaseDefendantsOffenceIds, reportingRestrictionReason, source, type, updateSlot);}

  public static class Builder {
    private UUID bookingId;

    private List<UUID> courtApplicationIds;

    private UUID courtCentreId;

    private UUID courtRoomId;

    private String estimatedDuration;

    private Integer estimatedMinutes;

    private Boolean hasAdjournmentDate;

    private List<HearingDay> hearingDays;

    private UUID hearingId;

    private HearingLanguage hearingLanguage;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantsOffenceIds;

    private String reportingRestrictionReason;

    private String source;

    private Type type;

    private Boolean updateSlot;

    public Builder withBookingId(final UUID bookingId) {
      this.bookingId = bookingId;
      return this;
    }

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

    public Builder withEstimatedDuration(final String estimatedDuration) {
      this.estimatedDuration = estimatedDuration;
      return this;
    }

    public Builder withEstimatedMinutes(final Integer estimatedMinutes) {
      this.estimatedMinutes = estimatedMinutes;
      return this;
    }

    public Builder withHasAdjournmentDate(final Boolean hasAdjournmentDate) {
      this.hasAdjournmentDate = hasAdjournmentDate;
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

    public Builder withValuesFrom(final HearingAllocatedForListing hearingAllocatedForListing) {
      this.bookingId = hearingAllocatedForListing.getBookingId();
      this.courtApplicationIds = hearingAllocatedForListing.getCourtApplicationIds();
      this.courtCentreId = hearingAllocatedForListing.getCourtCentreId();
      this.courtRoomId = hearingAllocatedForListing.getCourtRoomId();
      this.estimatedDuration = hearingAllocatedForListing.getEstimatedDuration();
      this.estimatedMinutes = hearingAllocatedForListing.getEstimatedMinutes();
      this.hasAdjournmentDate = hearingAllocatedForListing.getHasAdjournmentDate();
      this.hearingDays = hearingAllocatedForListing.getHearingDays();
      this.hearingId = hearingAllocatedForListing.getHearingId();
      this.hearingLanguage = hearingAllocatedForListing.getHearingLanguage();
      this.judiciary = hearingAllocatedForListing.getJudiciary();
      this.jurisdictionType = hearingAllocatedForListing.getJurisdictionType();
      this.prosecutionCaseDefendantsOffenceIds = hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds();
      this.reportingRestrictionReason = hearingAllocatedForListing.getReportingRestrictionReason();
      this.source = hearingAllocatedForListing.getSource();
      this.type = hearingAllocatedForListing.getType();
      this.updateSlot = hearingAllocatedForListing.getUpdateSlot();
      return this;
    }

    public HearingAllocatedForListing build() {
      return new uk.gov.justice.listing.events.HearingAllocatedForListing(bookingId, courtApplicationIds, courtCentreId, courtRoomId, estimatedDuration, estimatedMinutes, hasAdjournmentDate, hearingDays, hearingId, hearingLanguage, judiciary, jurisdictionType, prosecutionCaseDefendantsOffenceIds, reportingRestrictionReason, source, type, updateSlot);
    }
  }
}

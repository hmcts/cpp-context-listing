package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class ConfirmedHearing implements Serializable {
  private static final long serialVersionUID = -4327865879152535554L;

  private final List<UUID> courtApplicationIds;

  private final CourtCentre courtCentre;

  private final String estimatedDuration;

  private final UUID existingHearingId;

  private final Boolean fullExtension;

  private final List<HearingDay> hearingDays;

  private final HearingLanguage hearingLanguage;

  private final UUID id;

  private final Boolean isGroupProceedings;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final Integer numberOfGroupCases;

  private final List<ConfirmedProsecutionCase> prosecutionCases;

  private final String reportingRestrictionReason;

  private final HearingType type;

  @JsonCreator
  public ConfirmedHearing(final List<UUID> courtApplicationIds, final CourtCentre courtCentre, final String estimatedDuration, final UUID existingHearingId, final Boolean fullExtension, final List<HearingDay> hearingDays, final HearingLanguage hearingLanguage, final UUID id, final Boolean isGroupProceedings, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final Integer numberOfGroupCases, final List<ConfirmedProsecutionCase> prosecutionCases, final String reportingRestrictionReason, final HearingType type) {
    this.courtApplicationIds = courtApplicationIds;
    this.courtCentre = courtCentre;
    this.estimatedDuration = estimatedDuration;
    this.existingHearingId = existingHearingId;
    this.fullExtension = fullExtension;
    this.hearingDays = hearingDays;
    this.hearingLanguage = hearingLanguage;
    this.id = id;
    this.isGroupProceedings = isGroupProceedings;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.numberOfGroupCases = numberOfGroupCases;
    this.prosecutionCases = prosecutionCases;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.type = type;
  }

  public List<UUID> getCourtApplicationIds() {
    return courtApplicationIds;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public String getEstimatedDuration() {
    return estimatedDuration;
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

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
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

  public Integer getNumberOfGroupCases() {
    return numberOfGroupCases;
  }

  public List<ConfirmedProsecutionCase> getProsecutionCases() {
    return prosecutionCases;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public HearingType getType() {
    return type;
  }

  public static Builder confirmedHearing() {
    return new uk.gov.justice.core.courts.ConfirmedHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ConfirmedHearing that = (uk.gov.justice.core.courts.ConfirmedHearing) obj;

    return java.util.Objects.equals(this.courtApplicationIds, that.courtApplicationIds) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.existingHearingId, that.existingHearingId) &&
    java.util.Objects.equals(this.fullExtension, that.fullExtension) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isGroupProceedings, that.isGroupProceedings) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.type, that.type);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplicationIds, courtCentre, estimatedDuration, existingHearingId, fullExtension, hearingDays, hearingLanguage, id, isGroupProceedings, judiciary, jurisdictionType, numberOfGroupCases, prosecutionCases, reportingRestrictionReason, type);}

  public static class Builder {
    private List<UUID> courtApplicationIds;

    private CourtCentre courtCentre;

    private String estimatedDuration;

    private UUID existingHearingId;

    private Boolean fullExtension;

    private List<HearingDay> hearingDays;

    private HearingLanguage hearingLanguage;

    private UUID id;

    private Boolean isGroupProceedings;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private Integer numberOfGroupCases;

    private List<ConfirmedProsecutionCase> prosecutionCases;

    private String reportingRestrictionReason;

    private HearingType type;

    public Builder withCourtApplicationIds(final List<UUID> courtApplicationIds) {
      this.courtApplicationIds = courtApplicationIds;
      return this;
    }

    public Builder withCourtCentre(final CourtCentre courtCentre) {
      this.courtCentre = courtCentre;
      return this;
    }

    public Builder withEstimatedDuration(final String estimatedDuration) {
      this.estimatedDuration = estimatedDuration;
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

    public Builder withProsecutionCases(final List<ConfirmedProsecutionCase> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
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

    public Builder withValuesFrom(final ConfirmedHearing confirmedHearing) {
      this.courtApplicationIds = confirmedHearing.getCourtApplicationIds();
      this.courtCentre = confirmedHearing.getCourtCentre();
      this.estimatedDuration = confirmedHearing.getEstimatedDuration();
      this.existingHearingId = confirmedHearing.getExistingHearingId();
      this.fullExtension = confirmedHearing.getFullExtension();
      this.hearingDays = confirmedHearing.getHearingDays();
      this.hearingLanguage = confirmedHearing.getHearingLanguage();
      this.id = confirmedHearing.getId();
      this.isGroupProceedings = confirmedHearing.getIsGroupProceedings();
      this.judiciary = confirmedHearing.getJudiciary();
      this.jurisdictionType = confirmedHearing.getJurisdictionType();
      this.numberOfGroupCases = confirmedHearing.getNumberOfGroupCases();
      this.prosecutionCases = confirmedHearing.getProsecutionCases();
      this.reportingRestrictionReason = confirmedHearing.getReportingRestrictionReason();
      this.type = confirmedHearing.getType();
      return this;
    }

    public ConfirmedHearing build() {
      return new uk.gov.justice.core.courts.ConfirmedHearing(courtApplicationIds, courtCentre, estimatedDuration, existingHearingId, fullExtension, hearingDays, hearingLanguage, id, isGroupProceedings, judiciary, jurisdictionType, numberOfGroupCases, prosecutionCases, reportingRestrictionReason, type);
    }
  }
}

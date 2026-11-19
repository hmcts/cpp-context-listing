package uk.gov.justice.core.courts;

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

public class Hearing implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<ApplicantCounsel> applicantCounsels;

  private final List<CourtApplicationPartyCounsel> applicationPartyCounsels;

  private final List<ApprovalRequest> approvalsRequested;

  private final String bookingType;

  private final List<CompanyRepresentative> companyRepresentatives;

  private final List<CourtApplicationPartyAttendance> courtApplicationPartyAttendance;

  private final List<CourtApplication> courtApplications;

  private final CourtCentre courtCentre;

  private final CrackedIneffectiveTrial crackedIneffectiveTrial;

  private final List<DefenceCounsel> defenceCounsels;

  private final List<DefendantAttendance> defendantAttendance;

  private final List<DefendantJudicialResult> defendantJudicialResults;

  private final List<ReferralReason> defendantReferralReasons;

  private final List<DefendantsWithWelshTranslation> defendantsWithWelshTranslationList;

  private final DeletedJudicialResults deletedJudicialResults;

  private final ZonedDateTime earliestNextHearingDate;

  private final String estimatedDuration;

  private final Boolean hasSharedResults;

  private final List<HearingCaseNote> hearingCaseNotes;

  private final List<HearingDay> hearingDays;

  private final HearingLanguage hearingLanguage;

  private final UUID id;

  private final List<InterpreterIntermediary> intermediaries;

  private final Boolean isBoxHearing;

  private final Boolean isEffectiveTrial;

  private final Boolean isGroupProceedings;

  private final Boolean isSJPHearing;

  private final Boolean isVacatedTrial;

  private final Boolean isVirtualBoxHearing;

  private final List<JudicialRole> judiciary;

  private final JurisdictionType jurisdictionType;

  private final Integer numberOfGroupCases;

  private final String panel;

  private final String priority;

  private final List<ProsecutionCase> prosecutionCases;

  private final List<ProsecutionCounsel> prosecutionCounsels;

  private final String reportingRestrictionReason;

  private final List<RespondentCounsel> respondentCounsels;

  private final SeedingHearing seedingHearing;

  private final List<UUID> shadowListedOffences;

  private final List<String> specialRequirements;

  private final List<String> splitMergeTags;

  private final HearingType type;

  private final YouthCourt youthCourt;

  private final List<UUID> youthCourtDefendantIds;

  @JsonCreator
  public Hearing(final List<ApplicantCounsel> applicantCounsels, final List<CourtApplicationPartyCounsel> applicationPartyCounsels, final List<ApprovalRequest> approvalsRequested, final String bookingType, final List<CompanyRepresentative> companyRepresentatives, final List<CourtApplicationPartyAttendance> courtApplicationPartyAttendance, final List<CourtApplication> courtApplications, final CourtCentre courtCentre, final CrackedIneffectiveTrial crackedIneffectiveTrial, final List<DefenceCounsel> defenceCounsels, final List<DefendantAttendance> defendantAttendance, final List<DefendantJudicialResult> defendantJudicialResults, final List<ReferralReason> defendantReferralReasons, final List<DefendantsWithWelshTranslation> defendantsWithWelshTranslationList, final DeletedJudicialResults deletedJudicialResults, final ZonedDateTime earliestNextHearingDate, final String estimatedDuration, final Boolean hasSharedResults, final List<HearingCaseNote> hearingCaseNotes, final List<HearingDay> hearingDays, final HearingLanguage hearingLanguage, final UUID id, final List<InterpreterIntermediary> intermediaries, final Boolean isBoxHearing, final Boolean isEffectiveTrial, final Boolean isGroupProceedings, final Boolean isSJPHearing, final Boolean isVacatedTrial, final Boolean isVirtualBoxHearing, final List<JudicialRole> judiciary, final JurisdictionType jurisdictionType, final Integer numberOfGroupCases, final String panel, final String priority, final List<ProsecutionCase> prosecutionCases, final List<ProsecutionCounsel> prosecutionCounsels, final String reportingRestrictionReason, final List<RespondentCounsel> respondentCounsels, final SeedingHearing seedingHearing, final List<UUID> shadowListedOffences, final List<String> specialRequirements, final List<String> splitMergeTags, final HearingType type, final YouthCourt youthCourt, final List<UUID> youthCourtDefendantIds) {
    this.applicantCounsels = applicantCounsels;
    this.applicationPartyCounsels = applicationPartyCounsels;
    this.approvalsRequested = approvalsRequested;
    this.bookingType = bookingType;
    this.companyRepresentatives = companyRepresentatives;
    this.courtApplicationPartyAttendance = courtApplicationPartyAttendance;
    this.courtApplications = courtApplications;
    this.courtCentre = courtCentre;
    this.crackedIneffectiveTrial = crackedIneffectiveTrial;
    this.defenceCounsels = defenceCounsels;
    this.defendantAttendance = defendantAttendance;
    this.defendantJudicialResults = defendantJudicialResults;
    this.defendantReferralReasons = defendantReferralReasons;
    this.defendantsWithWelshTranslationList = defendantsWithWelshTranslationList;
    this.deletedJudicialResults = deletedJudicialResults;
    this.earliestNextHearingDate = earliestNextHearingDate;
    this.estimatedDuration = estimatedDuration;
    this.hasSharedResults = hasSharedResults;
    this.hearingCaseNotes = hearingCaseNotes;
    this.hearingDays = hearingDays;
    this.hearingLanguage = hearingLanguage;
    this.id = id;
    this.intermediaries = intermediaries;
    this.isBoxHearing = isBoxHearing;
    this.isEffectiveTrial = isEffectiveTrial;
    this.isGroupProceedings = isGroupProceedings;
    this.isSJPHearing = isSJPHearing;
    this.isVacatedTrial = isVacatedTrial;
    this.isVirtualBoxHearing = isVirtualBoxHearing;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.numberOfGroupCases = numberOfGroupCases;
    this.panel = panel;
    this.priority = priority;
    this.prosecutionCases = prosecutionCases;
    this.prosecutionCounsels = prosecutionCounsels;
    this.reportingRestrictionReason = reportingRestrictionReason;
    this.respondentCounsels = respondentCounsels;
    this.seedingHearing = seedingHearing;
    this.shadowListedOffences = shadowListedOffences;
    this.specialRequirements = specialRequirements;
    this.splitMergeTags = splitMergeTags;
    this.type = type;
    this.youthCourt = youthCourt;
    this.youthCourtDefendantIds = youthCourtDefendantIds;
  }

  public List<ApplicantCounsel> getApplicantCounsels() {
    return applicantCounsels;
  }

  public List<CourtApplicationPartyCounsel> getApplicationPartyCounsels() {
    return applicationPartyCounsels;
  }

  public List<ApprovalRequest> getApprovalsRequested() {
    return approvalsRequested;
  }

  public String getBookingType() {
    return bookingType;
  }

  public List<CompanyRepresentative> getCompanyRepresentatives() {
    return companyRepresentatives;
  }

  public List<CourtApplicationPartyAttendance> getCourtApplicationPartyAttendance() {
    return courtApplicationPartyAttendance;
  }

  public List<CourtApplication> getCourtApplications() {
    return courtApplications;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public CrackedIneffectiveTrial getCrackedIneffectiveTrial() {
    return crackedIneffectiveTrial;
  }

  public List<DefenceCounsel> getDefenceCounsels() {
    return defenceCounsels;
  }

  public List<DefendantAttendance> getDefendantAttendance() {
    return defendantAttendance;
  }

  public List<DefendantJudicialResult> getDefendantJudicialResults() {
    return defendantJudicialResults;
  }

  public List<ReferralReason> getDefendantReferralReasons() {
    return defendantReferralReasons;
  }

  public List<DefendantsWithWelshTranslation> getDefendantsWithWelshTranslationList() {
    return defendantsWithWelshTranslationList;
  }

  public DeletedJudicialResults getDeletedJudicialResults() {
    return deletedJudicialResults;
  }

  public ZonedDateTime getEarliestNextHearingDate() {
    return earliestNextHearingDate;
  }

  public String getEstimatedDuration() {
    return estimatedDuration;
  }

  public Boolean getHasSharedResults() {
    return hasSharedResults;
  }

  public List<HearingCaseNote> getHearingCaseNotes() {
    return hearingCaseNotes;
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

  public List<InterpreterIntermediary> getIntermediaries() {
    return intermediaries;
  }

  public Boolean getIsBoxHearing() {
    return isBoxHearing;
  }

  public Boolean getIsEffectiveTrial() {
    return isEffectiveTrial;
  }

  public Boolean getIsGroupProceedings() {
    return isGroupProceedings;
  }

  public Boolean getIsSJPHearing() {
    return isSJPHearing;
  }

  public Boolean getIsVacatedTrial() {
    return isVacatedTrial;
  }

  public Boolean getIsVirtualBoxHearing() {
    return isVirtualBoxHearing;
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

  public String getPanel() {
    return panel;
  }

  public String getPriority() {
    return priority;
  }

  public List<ProsecutionCase> getProsecutionCases() {
    return prosecutionCases;
  }

  public List<ProsecutionCounsel> getProsecutionCounsels() {
    return prosecutionCounsels;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public List<RespondentCounsel> getRespondentCounsels() {
    return respondentCounsels;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public List<String> getSpecialRequirements() {
    return specialRequirements;
  }

  public List<String> getSplitMergeTags() {
    return splitMergeTags;
  }

  public HearingType getType() {
    return type;
  }

  public YouthCourt getYouthCourt() {
    return youthCourt;
  }

  public List<UUID> getYouthCourtDefendantIds() {
    return youthCourtDefendantIds;
  }

  public static Builder hearing() {
    return new uk.gov.justice.core.courts.Hearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Hearing that = (uk.gov.justice.core.courts.Hearing) obj;

    return java.util.Objects.equals(this.applicantCounsels, that.applicantCounsels) &&
    java.util.Objects.equals(this.applicationPartyCounsels, that.applicationPartyCounsels) &&
    java.util.Objects.equals(this.approvalsRequested, that.approvalsRequested) &&
    java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.companyRepresentatives, that.companyRepresentatives) &&
    java.util.Objects.equals(this.courtApplicationPartyAttendance, that.courtApplicationPartyAttendance) &&
    java.util.Objects.equals(this.courtApplications, that.courtApplications) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.crackedIneffectiveTrial, that.crackedIneffectiveTrial) &&
    java.util.Objects.equals(this.defenceCounsels, that.defenceCounsels) &&
    java.util.Objects.equals(this.defendantAttendance, that.defendantAttendance) &&
    java.util.Objects.equals(this.defendantJudicialResults, that.defendantJudicialResults) &&
    java.util.Objects.equals(this.defendantReferralReasons, that.defendantReferralReasons) &&
    java.util.Objects.equals(this.defendantsWithWelshTranslationList, that.defendantsWithWelshTranslationList) &&
    java.util.Objects.equals(this.deletedJudicialResults, that.deletedJudicialResults) &&
    java.util.Objects.equals(this.earliestNextHearingDate, that.earliestNextHearingDate) &&
    java.util.Objects.equals(this.estimatedDuration, that.estimatedDuration) &&
    java.util.Objects.equals(this.hasSharedResults, that.hasSharedResults) &&
    java.util.Objects.equals(this.hearingCaseNotes, that.hearingCaseNotes) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.intermediaries, that.intermediaries) &&
    java.util.Objects.equals(this.isBoxHearing, that.isBoxHearing) &&
    java.util.Objects.equals(this.isEffectiveTrial, that.isEffectiveTrial) &&
    java.util.Objects.equals(this.isGroupProceedings, that.isGroupProceedings) &&
    java.util.Objects.equals(this.isSJPHearing, that.isSJPHearing) &&
    java.util.Objects.equals(this.isVacatedTrial, that.isVacatedTrial) &&
    java.util.Objects.equals(this.isVirtualBoxHearing, that.isVirtualBoxHearing) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.numberOfGroupCases, that.numberOfGroupCases) &&
    java.util.Objects.equals(this.panel, that.panel) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.prosecutionCounsels, that.prosecutionCounsels) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason) &&
    java.util.Objects.equals(this.respondentCounsels, that.respondentCounsels) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.splitMergeTags, that.splitMergeTags) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.youthCourt, that.youthCourt) &&
    java.util.Objects.equals(this.youthCourtDefendantIds, that.youthCourtDefendantIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicantCounsels, applicationPartyCounsels, approvalsRequested, bookingType, companyRepresentatives, courtApplicationPartyAttendance, courtApplications, courtCentre, crackedIneffectiveTrial, defenceCounsels, defendantAttendance, defendantJudicialResults, defendantReferralReasons, defendantsWithWelshTranslationList, deletedJudicialResults, earliestNextHearingDate, estimatedDuration, hasSharedResults, hearingCaseNotes, hearingDays, hearingLanguage, id, intermediaries, isBoxHearing, isEffectiveTrial, isGroupProceedings, isSJPHearing, isVacatedTrial, isVirtualBoxHearing, judiciary, jurisdictionType, numberOfGroupCases, panel, priority, prosecutionCases, prosecutionCounsels, reportingRestrictionReason, respondentCounsels, seedingHearing, shadowListedOffences, specialRequirements, splitMergeTags, type, youthCourt, youthCourtDefendantIds);}

  public static class Builder {
    private List<ApplicantCounsel> applicantCounsels;

    private List<CourtApplicationPartyCounsel> applicationPartyCounsels;

    private List<ApprovalRequest> approvalsRequested;

    private String bookingType;

    private List<CompanyRepresentative> companyRepresentatives;

    private List<CourtApplicationPartyAttendance> courtApplicationPartyAttendance;

    private List<CourtApplication> courtApplications;

    private CourtCentre courtCentre;

    private CrackedIneffectiveTrial crackedIneffectiveTrial;

    private List<DefenceCounsel> defenceCounsels;

    private List<DefendantAttendance> defendantAttendance;

    private List<DefendantJudicialResult> defendantJudicialResults;

    private List<ReferralReason> defendantReferralReasons;

    private List<DefendantsWithWelshTranslation> defendantsWithWelshTranslationList;

    private DeletedJudicialResults deletedJudicialResults;

    private ZonedDateTime earliestNextHearingDate;

    private String estimatedDuration;

    private Boolean hasSharedResults;

    private List<HearingCaseNote> hearingCaseNotes;

    private List<HearingDay> hearingDays;

    private HearingLanguage hearingLanguage;

    private UUID id;

    private List<InterpreterIntermediary> intermediaries;

    private Boolean isBoxHearing;

    private Boolean isEffectiveTrial;

    private Boolean isGroupProceedings;

    private Boolean isSJPHearing;

    private Boolean isVacatedTrial;

    private Boolean isVirtualBoxHearing;

    private List<JudicialRole> judiciary;

    private JurisdictionType jurisdictionType;

    private Integer numberOfGroupCases;

    private String panel;

    private String priority;

    private List<ProsecutionCase> prosecutionCases;

    private List<ProsecutionCounsel> prosecutionCounsels;

    private String reportingRestrictionReason;

    private List<RespondentCounsel> respondentCounsels;

    private SeedingHearing seedingHearing;

    private List<UUID> shadowListedOffences;

    private List<String> specialRequirements;

    private List<String> splitMergeTags;

    private HearingType type;

    private YouthCourt youthCourt;

    private List<UUID> youthCourtDefendantIds;

    public Builder withApplicantCounsels(final List<ApplicantCounsel> applicantCounsels) {
      this.applicantCounsels = applicantCounsels;
      return this;
    }

    public Builder withApplicationPartyCounsels(final List<CourtApplicationPartyCounsel> applicationPartyCounsels) {
      this.applicationPartyCounsels = applicationPartyCounsels;
      return this;
    }

    public Builder withApprovalsRequested(final List<ApprovalRequest> approvalsRequested) {
      this.approvalsRequested = approvalsRequested;
      return this;
    }

    public Builder withBookingType(final String bookingType) {
      this.bookingType = bookingType;
      return this;
    }

    public Builder withCompanyRepresentatives(final List<CompanyRepresentative> companyRepresentatives) {
      this.companyRepresentatives = companyRepresentatives;
      return this;
    }

    public Builder withCourtApplicationPartyAttendance(final List<CourtApplicationPartyAttendance> courtApplicationPartyAttendance) {
      this.courtApplicationPartyAttendance = courtApplicationPartyAttendance;
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

    public Builder withCrackedIneffectiveTrial(final CrackedIneffectiveTrial crackedIneffectiveTrial) {
      this.crackedIneffectiveTrial = crackedIneffectiveTrial;
      return this;
    }

    public Builder withDefenceCounsels(final List<DefenceCounsel> defenceCounsels) {
      this.defenceCounsels = defenceCounsels;
      return this;
    }

    public Builder withDefendantAttendance(final List<DefendantAttendance> defendantAttendance) {
      this.defendantAttendance = defendantAttendance;
      return this;
    }

    public Builder withDefendantJudicialResults(final List<DefendantJudicialResult> defendantJudicialResults) {
      this.defendantJudicialResults = defendantJudicialResults;
      return this;
    }

    public Builder withDefendantReferralReasons(final List<ReferralReason> defendantReferralReasons) {
      this.defendantReferralReasons = defendantReferralReasons;
      return this;
    }

    public Builder withDefendantsWithWelshTranslationList(final List<DefendantsWithWelshTranslation> defendantsWithWelshTranslationList) {
      this.defendantsWithWelshTranslationList = defendantsWithWelshTranslationList;
      return this;
    }

    public Builder withDeletedJudicialResults(final DeletedJudicialResults deletedJudicialResults) {
      this.deletedJudicialResults = deletedJudicialResults;
      return this;
    }

    public Builder withEarliestNextHearingDate(final ZonedDateTime earliestNextHearingDate) {
      this.earliestNextHearingDate = earliestNextHearingDate;
      return this;
    }

    public Builder withEstimatedDuration(final String estimatedDuration) {
      this.estimatedDuration = estimatedDuration;
      return this;
    }

    public Builder withHasSharedResults(final Boolean hasSharedResults) {
      this.hasSharedResults = hasSharedResults;
      return this;
    }

    public Builder withHearingCaseNotes(final List<HearingCaseNote> hearingCaseNotes) {
      this.hearingCaseNotes = hearingCaseNotes;
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

    public Builder withIntermediaries(final List<InterpreterIntermediary> intermediaries) {
      this.intermediaries = intermediaries;
      return this;
    }

    public Builder withIsBoxHearing(final Boolean isBoxHearing) {
      this.isBoxHearing = isBoxHearing;
      return this;
    }

    public Builder withIsEffectiveTrial(final Boolean isEffectiveTrial) {
      this.isEffectiveTrial = isEffectiveTrial;
      return this;
    }

    public Builder withIsGroupProceedings(final Boolean isGroupProceedings) {
      this.isGroupProceedings = isGroupProceedings;
      return this;
    }

    public Builder withIsSJPHearing(final Boolean isSJPHearing) {
      this.isSJPHearing = isSJPHearing;
      return this;
    }

    public Builder withIsVacatedTrial(final Boolean isVacatedTrial) {
      this.isVacatedTrial = isVacatedTrial;
      return this;
    }

    public Builder withIsVirtualBoxHearing(final Boolean isVirtualBoxHearing) {
      this.isVirtualBoxHearing = isVirtualBoxHearing;
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

    public Builder withPanel(final String panel) {
      this.panel = panel;
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

    public Builder withProsecutionCounsels(final List<ProsecutionCounsel> prosecutionCounsels) {
      this.prosecutionCounsels = prosecutionCounsels;
      return this;
    }

    public Builder withReportingRestrictionReason(final String reportingRestrictionReason) {
      this.reportingRestrictionReason = reportingRestrictionReason;
      return this;
    }

    public Builder withRespondentCounsels(final List<RespondentCounsel> respondentCounsels) {
      this.respondentCounsels = respondentCounsels;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withSpecialRequirements(final List<String> specialRequirements) {
      this.specialRequirements = specialRequirements;
      return this;
    }

    public Builder withSplitMergeTags(final List<String> splitMergeTags) {
      this.splitMergeTags = splitMergeTags;
      return this;
    }

    public Builder withType(final HearingType type) {
      this.type = type;
      return this;
    }

    public Builder withYouthCourt(final YouthCourt youthCourt) {
      this.youthCourt = youthCourt;
      return this;
    }

    public Builder withYouthCourtDefendantIds(final List<UUID> youthCourtDefendantIds) {
      this.youthCourtDefendantIds = youthCourtDefendantIds;
      return this;
    }

    public Builder withValuesFrom(final Hearing hearing) {
      this.applicantCounsels = hearing.getApplicantCounsels();
      this.applicationPartyCounsels = hearing.getApplicationPartyCounsels();
      this.approvalsRequested = hearing.getApprovalsRequested();
      this.bookingType = hearing.getBookingType();
      this.companyRepresentatives = hearing.getCompanyRepresentatives();
      this.courtApplicationPartyAttendance = hearing.getCourtApplicationPartyAttendance();
      this.courtApplications = hearing.getCourtApplications();
      this.courtCentre = hearing.getCourtCentre();
      this.crackedIneffectiveTrial = hearing.getCrackedIneffectiveTrial();
      this.defenceCounsels = hearing.getDefenceCounsels();
      this.defendantAttendance = hearing.getDefendantAttendance();
      this.defendantJudicialResults = hearing.getDefendantJudicialResults();
      this.defendantReferralReasons = hearing.getDefendantReferralReasons();
      this.defendantsWithWelshTranslationList = hearing.getDefendantsWithWelshTranslationList();
      this.deletedJudicialResults = hearing.getDeletedJudicialResults();
      this.earliestNextHearingDate = hearing.getEarliestNextHearingDate();
      this.estimatedDuration = hearing.getEstimatedDuration();
      this.hasSharedResults = hearing.getHasSharedResults();
      this.hearingCaseNotes = hearing.getHearingCaseNotes();
      this.hearingDays = hearing.getHearingDays();
      this.hearingLanguage = hearing.getHearingLanguage();
      this.id = hearing.getId();
      this.intermediaries = hearing.getIntermediaries();
      this.isBoxHearing = hearing.getIsBoxHearing();
      this.isEffectiveTrial = hearing.getIsEffectiveTrial();
      this.isGroupProceedings = hearing.getIsGroupProceedings();
      this.isSJPHearing = hearing.getIsSJPHearing();
      this.isVacatedTrial = hearing.getIsVacatedTrial();
      this.isVirtualBoxHearing = hearing.getIsVirtualBoxHearing();
      this.judiciary = hearing.getJudiciary();
      this.jurisdictionType = hearing.getJurisdictionType();
      this.numberOfGroupCases = hearing.getNumberOfGroupCases();
      this.panel = hearing.getPanel();
      this.priority = hearing.getPriority();
      this.prosecutionCases = hearing.getProsecutionCases();
      this.prosecutionCounsels = hearing.getProsecutionCounsels();
      this.reportingRestrictionReason = hearing.getReportingRestrictionReason();
      this.respondentCounsels = hearing.getRespondentCounsels();
      this.seedingHearing = hearing.getSeedingHearing();
      this.shadowListedOffences = hearing.getShadowListedOffences();
      this.specialRequirements = hearing.getSpecialRequirements();
      this.splitMergeTags = hearing.getSplitMergeTags();
      this.type = hearing.getType();
      this.youthCourt = hearing.getYouthCourt();
      this.youthCourtDefendantIds = hearing.getYouthCourtDefendantIds();
      return this;
    }

    public Hearing build() {
      return new uk.gov.justice.core.courts.Hearing(applicantCounsels, applicationPartyCounsels, approvalsRequested, bookingType, companyRepresentatives, courtApplicationPartyAttendance, courtApplications, courtCentre, crackedIneffectiveTrial, defenceCounsels, defendantAttendance, defendantJudicialResults, defendantReferralReasons, defendantsWithWelshTranslationList, deletedJudicialResults, earliestNextHearingDate, estimatedDuration, hasSharedResults, hearingCaseNotes, hearingDays, hearingLanguage, id, intermediaries, isBoxHearing, isEffectiveTrial, isGroupProceedings, isSJPHearing, isVacatedTrial, isVirtualBoxHearing, judiciary, jurisdictionType, numberOfGroupCases, panel, priority, prosecutionCases, prosecutionCounsels, reportingRestrictionReason, respondentCounsels, seedingHearing, shadowListedOffences, specialRequirements, splitMergeTags, type, youthCourt, youthCourtDefendantIds);
    }
  }
}

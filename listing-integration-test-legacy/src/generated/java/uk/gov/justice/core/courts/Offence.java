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

public class Offence implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final AllocationDecision allocationDecision;

  private final String aquittalDate;

  private final String arrestDate;

  private final BailStatus bailStatus;

  private final String chargeDate;

  private final CivilOffence civilOffence;

  private final CommittingCourt committingCourt;

  private final CourtCentre convictingCourt;

  private final String convictionDate;

  private final Integer count;

  private final Boolean ctlClockStopped;

  private final CustodyTimeLimit custodyTimeLimit;

  private final String dateHeldInCustodySince;

  private final String dateOfInformation;

  private final String dvlaOffenceCode;

  private final String endDate;

  private final Boolean endorsableFlag;

  private final UUID id;

  private final IndicatedPlea indicatedPlea;

  private final String indictmentParticular;

  private final Boolean introducedAfterInitialProceedings;

  private final Boolean isDiscontinued;

  private final Boolean isDisposed;

  private final List<JudicialResult> judicialResults;

  private final LaaReference laaApplnReference;

  private final String laidDate;

  private final String lastAdjournDate;

  private final String lastAdjournedHearingType;

  private final Integer listingNumber;

  private final String maxPenalty;

  private final String modeOfTrial;

  private final NotifiedPlea notifiedPlea;

  private final String offenceClass;

  private final String offenceCode;

  private final Integer offenceDateCode;

  private final UUID offenceDefinitionId;

  private final OffenceFacts offenceFacts;

  private final String offenceLegislation;

  private final String offenceLegislationWelsh;

  private final String offenceTitle;

  private final String offenceTitleWelsh;

  private final Boolean onlinePleaReceived;

  private final Integer orderIndex;

  private final Plea plea;

  private final Integer previousDaysHeldInCustody;

  private final Boolean proceedingsConcluded;

  private final List<ReportingRestriction> reportingRestrictions;

  private final SeedingHearing seedingHearing;

  private final String startDate;

  private final String statementOfFacts;

  private final Verdict verdict;

  private final List<Person> victims;

  private final String wording;

  private final String wordingWelsh;

  @JsonCreator
  public Offence(final AllocationDecision allocationDecision, final String aquittalDate, final String arrestDate, final BailStatus bailStatus, final String chargeDate, final CivilOffence civilOffence, final CommittingCourt committingCourt, final CourtCentre convictingCourt, final String convictionDate, final Integer count, final Boolean ctlClockStopped, final CustodyTimeLimit custodyTimeLimit, final String dateHeldInCustodySince, final String dateOfInformation, final String dvlaOffenceCode, final String endDate, final Boolean endorsableFlag, final UUID id, final IndicatedPlea indicatedPlea, final String indictmentParticular, final Boolean introducedAfterInitialProceedings, final Boolean isDiscontinued, final Boolean isDisposed, final List<JudicialResult> judicialResults, final LaaReference laaApplnReference, final String laidDate, final String lastAdjournDate, final String lastAdjournedHearingType, final Integer listingNumber, final String maxPenalty, final String modeOfTrial, final NotifiedPlea notifiedPlea, final String offenceClass, final String offenceCode, final Integer offenceDateCode, final UUID offenceDefinitionId, final OffenceFacts offenceFacts, final String offenceLegislation, final String offenceLegislationWelsh, final String offenceTitle, final String offenceTitleWelsh, final Boolean onlinePleaReceived, final Integer orderIndex, final Plea plea, final Integer previousDaysHeldInCustody, final Boolean proceedingsConcluded, final List<ReportingRestriction> reportingRestrictions, final SeedingHearing seedingHearing, final String startDate, final String statementOfFacts, final Verdict verdict, final List<Person> victims, final String wording, final String wordingWelsh) {
    this.allocationDecision = allocationDecision;
    this.aquittalDate = aquittalDate;
    this.arrestDate = arrestDate;
    this.bailStatus = bailStatus;
    this.chargeDate = chargeDate;
    this.civilOffence = civilOffence;
    this.committingCourt = committingCourt;
    this.convictingCourt = convictingCourt;
    this.convictionDate = convictionDate;
    this.count = count;
    this.ctlClockStopped = ctlClockStopped;
    this.custodyTimeLimit = custodyTimeLimit;
    this.dateHeldInCustodySince = dateHeldInCustodySince;
    this.dateOfInformation = dateOfInformation;
    this.dvlaOffenceCode = dvlaOffenceCode;
    this.endDate = endDate;
    this.endorsableFlag = endorsableFlag;
    this.id = id;
    this.indicatedPlea = indicatedPlea;
    this.indictmentParticular = indictmentParticular;
    this.introducedAfterInitialProceedings = introducedAfterInitialProceedings;
    this.isDiscontinued = isDiscontinued;
    this.isDisposed = isDisposed;
    this.judicialResults = judicialResults;
    this.laaApplnReference = laaApplnReference;
    this.laidDate = laidDate;
    this.lastAdjournDate = lastAdjournDate;
    this.lastAdjournedHearingType = lastAdjournedHearingType;
    this.listingNumber = listingNumber;
    this.maxPenalty = maxPenalty;
    this.modeOfTrial = modeOfTrial;
    this.notifiedPlea = notifiedPlea;
    this.offenceClass = offenceClass;
    this.offenceCode = offenceCode;
    this.offenceDateCode = offenceDateCode;
    this.offenceDefinitionId = offenceDefinitionId;
    this.offenceFacts = offenceFacts;
    this.offenceLegislation = offenceLegislation;
    this.offenceLegislationWelsh = offenceLegislationWelsh;
    this.offenceTitle = offenceTitle;
    this.offenceTitleWelsh = offenceTitleWelsh;
    this.onlinePleaReceived = onlinePleaReceived;
    this.orderIndex = orderIndex;
    this.plea = plea;
    this.previousDaysHeldInCustody = previousDaysHeldInCustody;
    this.proceedingsConcluded = proceedingsConcluded;
    this.reportingRestrictions = reportingRestrictions;
    this.seedingHearing = seedingHearing;
    this.startDate = startDate;
    this.statementOfFacts = statementOfFacts;
    this.verdict = verdict;
    this.victims = victims;
    this.wording = wording;
    this.wordingWelsh = wordingWelsh;
  }

  public AllocationDecision getAllocationDecision() {
    return allocationDecision;
  }

  public String getAquittalDate() {
    return aquittalDate;
  }

  public String getArrestDate() {
    return arrestDate;
  }

  public BailStatus getBailStatus() {
    return bailStatus;
  }

  public String getChargeDate() {
    return chargeDate;
  }

  public CivilOffence getCivilOffence() {
    return civilOffence;
  }

  public CommittingCourt getCommittingCourt() {
    return committingCourt;
  }

  public CourtCentre getConvictingCourt() {
    return convictingCourt;
  }

  public String getConvictionDate() {
    return convictionDate;
  }

  public Integer getCount() {
    return count;
  }

  public Boolean getCtlClockStopped() {
    return ctlClockStopped;
  }

  public CustodyTimeLimit getCustodyTimeLimit() {
    return custodyTimeLimit;
  }

  public String getDateHeldInCustodySince() {
    return dateHeldInCustodySince;
  }

  public String getDateOfInformation() {
    return dateOfInformation;
  }

  public String getDvlaOffenceCode() {
    return dvlaOffenceCode;
  }

  public String getEndDate() {
    return endDate;
  }

  public Boolean getEndorsableFlag() {
    return endorsableFlag;
  }

  public UUID getId() {
    return id;
  }

  public IndicatedPlea getIndicatedPlea() {
    return indicatedPlea;
  }

  public String getIndictmentParticular() {
    return indictmentParticular;
  }

  public Boolean getIntroducedAfterInitialProceedings() {
    return introducedAfterInitialProceedings;
  }

  public Boolean getIsDiscontinued() {
    return isDiscontinued;
  }

  public Boolean getIsDisposed() {
    return isDisposed;
  }

  public List<JudicialResult> getJudicialResults() {
    return judicialResults;
  }

  public LaaReference getLaaApplnReference() {
    return laaApplnReference;
  }

  public String getLaidDate() {
    return laidDate;
  }

  public String getLastAdjournDate() {
    return lastAdjournDate;
  }

  public String getLastAdjournedHearingType() {
    return lastAdjournedHearingType;
  }

  public Integer getListingNumber() {
    return listingNumber;
  }

  public String getMaxPenalty() {
    return maxPenalty;
  }

  public String getModeOfTrial() {
    return modeOfTrial;
  }

  public NotifiedPlea getNotifiedPlea() {
    return notifiedPlea;
  }

  public String getOffenceClass() {
    return offenceClass;
  }

  public String getOffenceCode() {
    return offenceCode;
  }

  public Integer getOffenceDateCode() {
    return offenceDateCode;
  }

  public UUID getOffenceDefinitionId() {
    return offenceDefinitionId;
  }

  public OffenceFacts getOffenceFacts() {
    return offenceFacts;
  }

  public String getOffenceLegislation() {
    return offenceLegislation;
  }

  public String getOffenceLegislationWelsh() {
    return offenceLegislationWelsh;
  }

  public String getOffenceTitle() {
    return offenceTitle;
  }

  public String getOffenceTitleWelsh() {
    return offenceTitleWelsh;
  }

  public Boolean getOnlinePleaReceived() {
    return onlinePleaReceived;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public Plea getPlea() {
    return plea;
  }

  public Integer getPreviousDaysHeldInCustody() {
    return previousDaysHeldInCustody;
  }

  public Boolean getProceedingsConcluded() {
    return proceedingsConcluded;
  }

  public List<ReportingRestriction> getReportingRestrictions() {
    return reportingRestrictions;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public String getStartDate() {
    return startDate;
  }

  public String getStatementOfFacts() {
    return statementOfFacts;
  }

  public Verdict getVerdict() {
    return verdict;
  }

  public List<Person> getVictims() {
    return victims;
  }

  public String getWording() {
    return wording;
  }

  public String getWordingWelsh() {
    return wordingWelsh;
  }

  public static Builder offence() {
    return new uk.gov.justice.core.courts.Offence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Offence that = (uk.gov.justice.core.courts.Offence) obj;

    return java.util.Objects.equals(this.allocationDecision, that.allocationDecision) &&
    java.util.Objects.equals(this.aquittalDate, that.aquittalDate) &&
    java.util.Objects.equals(this.arrestDate, that.arrestDate) &&
    java.util.Objects.equals(this.bailStatus, that.bailStatus) &&
    java.util.Objects.equals(this.chargeDate, that.chargeDate) &&
    java.util.Objects.equals(this.civilOffence, that.civilOffence) &&
    java.util.Objects.equals(this.committingCourt, that.committingCourt) &&
    java.util.Objects.equals(this.convictingCourt, that.convictingCourt) &&
    java.util.Objects.equals(this.convictionDate, that.convictionDate) &&
    java.util.Objects.equals(this.count, that.count) &&
    java.util.Objects.equals(this.ctlClockStopped, that.ctlClockStopped) &&
    java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
    java.util.Objects.equals(this.dateHeldInCustodySince, that.dateHeldInCustodySince) &&
    java.util.Objects.equals(this.dateOfInformation, that.dateOfInformation) &&
    java.util.Objects.equals(this.dvlaOffenceCode, that.dvlaOffenceCode) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.endorsableFlag, that.endorsableFlag) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.indicatedPlea, that.indicatedPlea) &&
    java.util.Objects.equals(this.indictmentParticular, that.indictmentParticular) &&
    java.util.Objects.equals(this.introducedAfterInitialProceedings, that.introducedAfterInitialProceedings) &&
    java.util.Objects.equals(this.isDiscontinued, that.isDiscontinued) &&
    java.util.Objects.equals(this.isDisposed, that.isDisposed) &&
    java.util.Objects.equals(this.judicialResults, that.judicialResults) &&
    java.util.Objects.equals(this.laaApplnReference, that.laaApplnReference) &&
    java.util.Objects.equals(this.laidDate, that.laidDate) &&
    java.util.Objects.equals(this.lastAdjournDate, that.lastAdjournDate) &&
    java.util.Objects.equals(this.lastAdjournedHearingType, that.lastAdjournedHearingType) &&
    java.util.Objects.equals(this.listingNumber, that.listingNumber) &&
    java.util.Objects.equals(this.maxPenalty, that.maxPenalty) &&
    java.util.Objects.equals(this.modeOfTrial, that.modeOfTrial) &&
    java.util.Objects.equals(this.notifiedPlea, that.notifiedPlea) &&
    java.util.Objects.equals(this.offenceClass, that.offenceClass) &&
    java.util.Objects.equals(this.offenceCode, that.offenceCode) &&
    java.util.Objects.equals(this.offenceDateCode, that.offenceDateCode) &&
    java.util.Objects.equals(this.offenceDefinitionId, that.offenceDefinitionId) &&
    java.util.Objects.equals(this.offenceFacts, that.offenceFacts) &&
    java.util.Objects.equals(this.offenceLegislation, that.offenceLegislation) &&
    java.util.Objects.equals(this.offenceLegislationWelsh, that.offenceLegislationWelsh) &&
    java.util.Objects.equals(this.offenceTitle, that.offenceTitle) &&
    java.util.Objects.equals(this.offenceTitleWelsh, that.offenceTitleWelsh) &&
    java.util.Objects.equals(this.onlinePleaReceived, that.onlinePleaReceived) &&
    java.util.Objects.equals(this.orderIndex, that.orderIndex) &&
    java.util.Objects.equals(this.plea, that.plea) &&
    java.util.Objects.equals(this.previousDaysHeldInCustody, that.previousDaysHeldInCustody) &&
    java.util.Objects.equals(this.proceedingsConcluded, that.proceedingsConcluded) &&
    java.util.Objects.equals(this.reportingRestrictions, that.reportingRestrictions) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.statementOfFacts, that.statementOfFacts) &&
    java.util.Objects.equals(this.verdict, that.verdict) &&
    java.util.Objects.equals(this.victims, that.victims) &&
    java.util.Objects.equals(this.wording, that.wording) &&
    java.util.Objects.equals(this.wordingWelsh, that.wordingWelsh);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocationDecision, aquittalDate, arrestDate, bailStatus, chargeDate, civilOffence, committingCourt, convictingCourt, convictionDate, count, ctlClockStopped, custodyTimeLimit, dateHeldInCustodySince, dateOfInformation, dvlaOffenceCode, endDate, endorsableFlag, id, indicatedPlea, indictmentParticular, introducedAfterInitialProceedings, isDiscontinued, isDisposed, judicialResults, laaApplnReference, laidDate, lastAdjournDate, lastAdjournedHearingType, listingNumber, maxPenalty, modeOfTrial, notifiedPlea, offenceClass, offenceCode, offenceDateCode, offenceDefinitionId, offenceFacts, offenceLegislation, offenceLegislationWelsh, offenceTitle, offenceTitleWelsh, onlinePleaReceived, orderIndex, plea, previousDaysHeldInCustody, proceedingsConcluded, reportingRestrictions, seedingHearing, startDate, statementOfFacts, verdict, victims, wording, wordingWelsh);}

  public static class Builder {
    private AllocationDecision allocationDecision;

    private String aquittalDate;

    private String arrestDate;

    private BailStatus bailStatus;

    private String chargeDate;

    private CivilOffence civilOffence;

    private CommittingCourt committingCourt;

    private CourtCentre convictingCourt;

    private String convictionDate;

    private Integer count;

    private Boolean ctlClockStopped;

    private CustodyTimeLimit custodyTimeLimit;

    private String dateHeldInCustodySince;

    private String dateOfInformation;

    private String dvlaOffenceCode;

    private String endDate;

    private Boolean endorsableFlag;

    private UUID id;

    private IndicatedPlea indicatedPlea;

    private String indictmentParticular;

    private Boolean introducedAfterInitialProceedings;

    private Boolean isDiscontinued;

    private Boolean isDisposed;

    private List<JudicialResult> judicialResults;

    private LaaReference laaApplnReference;

    private String laidDate;

    private String lastAdjournDate;

    private String lastAdjournedHearingType;

    private Integer listingNumber;

    private String maxPenalty;

    private String modeOfTrial;

    private NotifiedPlea notifiedPlea;

    private String offenceClass;

    private String offenceCode;

    private Integer offenceDateCode;

    private UUID offenceDefinitionId;

    private OffenceFacts offenceFacts;

    private String offenceLegislation;

    private String offenceLegislationWelsh;

    private String offenceTitle;

    private String offenceTitleWelsh;

    private Boolean onlinePleaReceived;

    private Integer orderIndex;

    private Plea plea;

    private Integer previousDaysHeldInCustody;

    private Boolean proceedingsConcluded;

    private List<ReportingRestriction> reportingRestrictions;

    private SeedingHearing seedingHearing;

    private String startDate;

    private String statementOfFacts;

    private Verdict verdict;

    private List<Person> victims;

    private String wording;

    private String wordingWelsh;

    public Builder withAllocationDecision(final AllocationDecision allocationDecision) {
      this.allocationDecision = allocationDecision;
      return this;
    }

    public Builder withAquittalDate(final String aquittalDate) {
      this.aquittalDate = aquittalDate;
      return this;
    }

    public Builder withArrestDate(final String arrestDate) {
      this.arrestDate = arrestDate;
      return this;
    }

    public Builder withBailStatus(final BailStatus bailStatus) {
      this.bailStatus = bailStatus;
      return this;
    }

    public Builder withChargeDate(final String chargeDate) {
      this.chargeDate = chargeDate;
      return this;
    }

    public Builder withCivilOffence(final CivilOffence civilOffence) {
      this.civilOffence = civilOffence;
      return this;
    }

    public Builder withCommittingCourt(final CommittingCourt committingCourt) {
      this.committingCourt = committingCourt;
      return this;
    }

    public Builder withConvictingCourt(final CourtCentre convictingCourt) {
      this.convictingCourt = convictingCourt;
      return this;
    }

    public Builder withConvictionDate(final String convictionDate) {
      this.convictionDate = convictionDate;
      return this;
    }

    public Builder withCount(final Integer count) {
      this.count = count;
      return this;
    }

    public Builder withCtlClockStopped(final Boolean ctlClockStopped) {
      this.ctlClockStopped = ctlClockStopped;
      return this;
    }

    public Builder withCustodyTimeLimit(final CustodyTimeLimit custodyTimeLimit) {
      this.custodyTimeLimit = custodyTimeLimit;
      return this;
    }

    public Builder withDateHeldInCustodySince(final String dateHeldInCustodySince) {
      this.dateHeldInCustodySince = dateHeldInCustodySince;
      return this;
    }

    public Builder withDateOfInformation(final String dateOfInformation) {
      this.dateOfInformation = dateOfInformation;
      return this;
    }

    public Builder withDvlaOffenceCode(final String dvlaOffenceCode) {
      this.dvlaOffenceCode = dvlaOffenceCode;
      return this;
    }

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withEndorsableFlag(final Boolean endorsableFlag) {
      this.endorsableFlag = endorsableFlag;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIndicatedPlea(final IndicatedPlea indicatedPlea) {
      this.indicatedPlea = indicatedPlea;
      return this;
    }

    public Builder withIndictmentParticular(final String indictmentParticular) {
      this.indictmentParticular = indictmentParticular;
      return this;
    }

    public Builder withIntroducedAfterInitialProceedings(final Boolean introducedAfterInitialProceedings) {
      this.introducedAfterInitialProceedings = introducedAfterInitialProceedings;
      return this;
    }

    public Builder withIsDiscontinued(final Boolean isDiscontinued) {
      this.isDiscontinued = isDiscontinued;
      return this;
    }

    public Builder withIsDisposed(final Boolean isDisposed) {
      this.isDisposed = isDisposed;
      return this;
    }

    public Builder withJudicialResults(final List<JudicialResult> judicialResults) {
      this.judicialResults = judicialResults;
      return this;
    }

    public Builder withLaaApplnReference(final LaaReference laaApplnReference) {
      this.laaApplnReference = laaApplnReference;
      return this;
    }

    public Builder withLaidDate(final String laidDate) {
      this.laidDate = laidDate;
      return this;
    }

    public Builder withLastAdjournDate(final String lastAdjournDate) {
      this.lastAdjournDate = lastAdjournDate;
      return this;
    }

    public Builder withLastAdjournedHearingType(final String lastAdjournedHearingType) {
      this.lastAdjournedHearingType = lastAdjournedHearingType;
      return this;
    }

    public Builder withListingNumber(final Integer listingNumber) {
      this.listingNumber = listingNumber;
      return this;
    }

    public Builder withMaxPenalty(final String maxPenalty) {
      this.maxPenalty = maxPenalty;
      return this;
    }

    public Builder withModeOfTrial(final String modeOfTrial) {
      this.modeOfTrial = modeOfTrial;
      return this;
    }

    public Builder withNotifiedPlea(final NotifiedPlea notifiedPlea) {
      this.notifiedPlea = notifiedPlea;
      return this;
    }

    public Builder withOffenceClass(final String offenceClass) {
      this.offenceClass = offenceClass;
      return this;
    }

    public Builder withOffenceCode(final String offenceCode) {
      this.offenceCode = offenceCode;
      return this;
    }

    public Builder withOffenceDateCode(final Integer offenceDateCode) {
      this.offenceDateCode = offenceDateCode;
      return this;
    }

    public Builder withOffenceDefinitionId(final UUID offenceDefinitionId) {
      this.offenceDefinitionId = offenceDefinitionId;
      return this;
    }

    public Builder withOffenceFacts(final OffenceFacts offenceFacts) {
      this.offenceFacts = offenceFacts;
      return this;
    }

    public Builder withOffenceLegislation(final String offenceLegislation) {
      this.offenceLegislation = offenceLegislation;
      return this;
    }

    public Builder withOffenceLegislationWelsh(final String offenceLegislationWelsh) {
      this.offenceLegislationWelsh = offenceLegislationWelsh;
      return this;
    }

    public Builder withOffenceTitle(final String offenceTitle) {
      this.offenceTitle = offenceTitle;
      return this;
    }

    public Builder withOffenceTitleWelsh(final String offenceTitleWelsh) {
      this.offenceTitleWelsh = offenceTitleWelsh;
      return this;
    }

    public Builder withOnlinePleaReceived(final Boolean onlinePleaReceived) {
      this.onlinePleaReceived = onlinePleaReceived;
      return this;
    }

    public Builder withOrderIndex(final Integer orderIndex) {
      this.orderIndex = orderIndex;
      return this;
    }

    public Builder withPlea(final Plea plea) {
      this.plea = plea;
      return this;
    }

    public Builder withPreviousDaysHeldInCustody(final Integer previousDaysHeldInCustody) {
      this.previousDaysHeldInCustody = previousDaysHeldInCustody;
      return this;
    }

    public Builder withProceedingsConcluded(final Boolean proceedingsConcluded) {
      this.proceedingsConcluded = proceedingsConcluded;
      return this;
    }

    public Builder withReportingRestrictions(final List<ReportingRestriction> reportingRestrictions) {
      this.reportingRestrictions = reportingRestrictions;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withStatementOfFacts(final String statementOfFacts) {
      this.statementOfFacts = statementOfFacts;
      return this;
    }

    public Builder withVerdict(final Verdict verdict) {
      this.verdict = verdict;
      return this;
    }

    public Builder withVictims(final List<Person> victims) {
      this.victims = victims;
      return this;
    }

    public Builder withWording(final String wording) {
      this.wording = wording;
      return this;
    }

    public Builder withWordingWelsh(final String wordingWelsh) {
      this.wordingWelsh = wordingWelsh;
      return this;
    }

    public Builder withValuesFrom(final Offence offence) {
      this.allocationDecision = offence.getAllocationDecision();
      this.aquittalDate = offence.getAquittalDate();
      this.arrestDate = offence.getArrestDate();
      this.bailStatus = offence.getBailStatus();
      this.chargeDate = offence.getChargeDate();
      this.civilOffence = offence.getCivilOffence();
      this.committingCourt = offence.getCommittingCourt();
      this.convictingCourt = offence.getConvictingCourt();
      this.convictionDate = offence.getConvictionDate();
      this.count = offence.getCount();
      this.ctlClockStopped = offence.getCtlClockStopped();
      this.custodyTimeLimit = offence.getCustodyTimeLimit();
      this.dateHeldInCustodySince = offence.getDateHeldInCustodySince();
      this.dateOfInformation = offence.getDateOfInformation();
      this.dvlaOffenceCode = offence.getDvlaOffenceCode();
      this.endDate = offence.getEndDate();
      this.endorsableFlag = offence.getEndorsableFlag();
      this.id = offence.getId();
      this.indicatedPlea = offence.getIndicatedPlea();
      this.indictmentParticular = offence.getIndictmentParticular();
      this.introducedAfterInitialProceedings = offence.getIntroducedAfterInitialProceedings();
      this.isDiscontinued = offence.getIsDiscontinued();
      this.isDisposed = offence.getIsDisposed();
      this.judicialResults = offence.getJudicialResults();
      this.laaApplnReference = offence.getLaaApplnReference();
      this.laidDate = offence.getLaidDate();
      this.lastAdjournDate = offence.getLastAdjournDate();
      this.lastAdjournedHearingType = offence.getLastAdjournedHearingType();
      this.listingNumber = offence.getListingNumber();
      this.maxPenalty = offence.getMaxPenalty();
      this.modeOfTrial = offence.getModeOfTrial();
      this.notifiedPlea = offence.getNotifiedPlea();
      this.offenceClass = offence.getOffenceClass();
      this.offenceCode = offence.getOffenceCode();
      this.offenceDateCode = offence.getOffenceDateCode();
      this.offenceDefinitionId = offence.getOffenceDefinitionId();
      this.offenceFacts = offence.getOffenceFacts();
      this.offenceLegislation = offence.getOffenceLegislation();
      this.offenceLegislationWelsh = offence.getOffenceLegislationWelsh();
      this.offenceTitle = offence.getOffenceTitle();
      this.offenceTitleWelsh = offence.getOffenceTitleWelsh();
      this.onlinePleaReceived = offence.getOnlinePleaReceived();
      this.orderIndex = offence.getOrderIndex();
      this.plea = offence.getPlea();
      this.previousDaysHeldInCustody = offence.getPreviousDaysHeldInCustody();
      this.proceedingsConcluded = offence.getProceedingsConcluded();
      this.reportingRestrictions = offence.getReportingRestrictions();
      this.seedingHearing = offence.getSeedingHearing();
      this.startDate = offence.getStartDate();
      this.statementOfFacts = offence.getStatementOfFacts();
      this.verdict = offence.getVerdict();
      this.victims = offence.getVictims();
      this.wording = offence.getWording();
      this.wordingWelsh = offence.getWordingWelsh();
      return this;
    }

    public Offence build() {
      return new uk.gov.justice.core.courts.Offence(allocationDecision, aquittalDate, arrestDate, bailStatus, chargeDate, civilOffence, committingCourt, convictingCourt, convictionDate, count, ctlClockStopped, custodyTimeLimit, dateHeldInCustodySince, dateOfInformation, dvlaOffenceCode, endDate, endorsableFlag, id, indicatedPlea, indictmentParticular, introducedAfterInitialProceedings, isDiscontinued, isDisposed, judicialResults, laaApplnReference, laidDate, lastAdjournDate, lastAdjournedHearingType, listingNumber, maxPenalty, modeOfTrial, notifiedPlea, offenceClass, offenceCode, offenceDateCode, offenceDefinitionId, offenceFacts, offenceLegislation, offenceLegislationWelsh, offenceTitle, offenceTitleWelsh, onlinePleaReceived, orderIndex, plea, previousDaysHeldInCustody, proceedingsConcluded, reportingRestrictions, seedingHearing, startDate, statementOfFacts, verdict, victims, wording, wordingWelsh);
    }
  }
}

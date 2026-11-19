package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CourtApplication implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String allegationOrComplaintEndDate;

  private final String allegationOrComplaintStartDate;

  private final CourtApplicationParty applicant;

  private final String applicationDecisionSoughtByDate;

  private final ApplicationExternalCreatorType applicationExternalCreatorType;

  private final String applicationParticulars;

  private final String applicationReceivedDate;

  private final String applicationReference;

  private final String applicationResultCodeForLaa;

  private final ApplicationStatus applicationStatus;

  private final Boolean commissionerOfOath;

  private final String convictionDate;

  private final List<CourtApplicationCase> courtApplicationCases;

  private final CourtApplicationPayment courtApplicationPayment;

  private final CourtCivilApplication courtCivilApplication;

  private final CourtOrder courtOrder;

  private final String defendantASN;

  private final FutureSummonsHearing futureSummonsHearing;

  private final Boolean hasSummonsSupplied;

  private final UUID hearingIdToBeVacated;

  private final UUID id;

  private final Boolean isGroupCaseApplication;

  private final List<JudicialResult> judicialResults;

  private final LaaReference laaApplnReference;

  private final String outOfTimeReasons;

  private final UUID parentApplicationId;

  private final Plea plea;

  private final Boolean proceedingsConcluded;

  private final String removalReason;

  private final List<CourtApplicationParty> respondents;

  private final CourtApplicationParty subject;

  private final List<CourtApplicationParty> thirdParties;

  private final CourtApplicationType type;

  private final Verdict verdict;

  @JsonCreator
  public CourtApplication(final String allegationOrComplaintEndDate, final String allegationOrComplaintStartDate, final CourtApplicationParty applicant, final String applicationDecisionSoughtByDate, final ApplicationExternalCreatorType applicationExternalCreatorType, final String applicationParticulars, final String applicationReceivedDate, final String applicationReference, final String applicationResultCodeForLaa, final ApplicationStatus applicationStatus, final Boolean commissionerOfOath, final String convictionDate, final List<CourtApplicationCase> courtApplicationCases, final CourtApplicationPayment courtApplicationPayment, final CourtCivilApplication courtCivilApplication, final CourtOrder courtOrder, final String defendantASN, final FutureSummonsHearing futureSummonsHearing, final Boolean hasSummonsSupplied, final UUID hearingIdToBeVacated, final UUID id, final Boolean isGroupCaseApplication, final List<JudicialResult> judicialResults, final LaaReference laaApplnReference, final String outOfTimeReasons, final UUID parentApplicationId, final Plea plea, final Boolean proceedingsConcluded, final String removalReason, final List<CourtApplicationParty> respondents, final CourtApplicationParty subject, final List<CourtApplicationParty> thirdParties, final CourtApplicationType type, final Verdict verdict) {
    this.allegationOrComplaintEndDate = allegationOrComplaintEndDate;
    this.allegationOrComplaintStartDate = allegationOrComplaintStartDate;
    this.applicant = applicant;
    this.applicationDecisionSoughtByDate = applicationDecisionSoughtByDate;
    this.applicationExternalCreatorType = applicationExternalCreatorType;
    this.applicationParticulars = applicationParticulars;
    this.applicationReceivedDate = applicationReceivedDate;
    this.applicationReference = applicationReference;
    this.applicationResultCodeForLaa = applicationResultCodeForLaa;
    this.applicationStatus = applicationStatus;
    this.commissionerOfOath = commissionerOfOath;
    this.convictionDate = convictionDate;
    this.courtApplicationCases = courtApplicationCases;
    this.courtApplicationPayment = courtApplicationPayment;
    this.courtCivilApplication = courtCivilApplication;
    this.courtOrder = courtOrder;
    this.defendantASN = defendantASN;
    this.futureSummonsHearing = futureSummonsHearing;
    this.hasSummonsSupplied = hasSummonsSupplied;
    this.hearingIdToBeVacated = hearingIdToBeVacated;
    this.id = id;
    this.isGroupCaseApplication = isGroupCaseApplication;
    this.judicialResults = judicialResults;
    this.laaApplnReference = laaApplnReference;
    this.outOfTimeReasons = outOfTimeReasons;
    this.parentApplicationId = parentApplicationId;
    this.plea = plea;
    this.proceedingsConcluded = proceedingsConcluded;
    this.removalReason = removalReason;
    this.respondents = respondents;
    this.subject = subject;
    this.thirdParties = thirdParties;
    this.type = type;
    this.verdict = verdict;
  }

  public String getAllegationOrComplaintEndDate() {
    return allegationOrComplaintEndDate;
  }

  public String getAllegationOrComplaintStartDate() {
    return allegationOrComplaintStartDate;
  }

  public CourtApplicationParty getApplicant() {
    return applicant;
  }

  public String getApplicationDecisionSoughtByDate() {
    return applicationDecisionSoughtByDate;
  }

  public ApplicationExternalCreatorType getApplicationExternalCreatorType() {
    return applicationExternalCreatorType;
  }

  public String getApplicationParticulars() {
    return applicationParticulars;
  }

  public String getApplicationReceivedDate() {
    return applicationReceivedDate;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public String getApplicationResultCodeForLaa() {
    return applicationResultCodeForLaa;
  }

  public ApplicationStatus getApplicationStatus() {
    return applicationStatus;
  }

  public Boolean getCommissionerOfOath() {
    return commissionerOfOath;
  }

  public String getConvictionDate() {
    return convictionDate;
  }

  public List<CourtApplicationCase> getCourtApplicationCases() {
    return courtApplicationCases;
  }

  public CourtApplicationPayment getCourtApplicationPayment() {
    return courtApplicationPayment;
  }

  public CourtCivilApplication getCourtCivilApplication() {
    return courtCivilApplication;
  }

  public CourtOrder getCourtOrder() {
    return courtOrder;
  }

  public String getDefendantASN() {
    return defendantASN;
  }

  public FutureSummonsHearing getFutureSummonsHearing() {
    return futureSummonsHearing;
  }

  public Boolean getHasSummonsSupplied() {
    return hasSummonsSupplied;
  }

  public UUID getHearingIdToBeVacated() {
    return hearingIdToBeVacated;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsGroupCaseApplication() {
    return isGroupCaseApplication;
  }

  public List<JudicialResult> getJudicialResults() {
    return judicialResults;
  }

  public LaaReference getLaaApplnReference() {
    return laaApplnReference;
  }

  public String getOutOfTimeReasons() {
    return outOfTimeReasons;
  }

  public UUID getParentApplicationId() {
    return parentApplicationId;
  }

  public Plea getPlea() {
    return plea;
  }

  public Boolean getProceedingsConcluded() {
    return proceedingsConcluded;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public List<CourtApplicationParty> getRespondents() {
    return respondents;
  }

  public CourtApplicationParty getSubject() {
    return subject;
  }

  public List<CourtApplicationParty> getThirdParties() {
    return thirdParties;
  }

  public CourtApplicationType getType() {
    return type;
  }

  public Verdict getVerdict() {
    return verdict;
  }

  public static Builder courtApplication() {
    return new uk.gov.justice.core.courts.CourtApplication.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplication that = (uk.gov.justice.core.courts.CourtApplication) obj;

    return java.util.Objects.equals(this.allegationOrComplaintEndDate, that.allegationOrComplaintEndDate) &&
    java.util.Objects.equals(this.allegationOrComplaintStartDate, that.allegationOrComplaintStartDate) &&
    java.util.Objects.equals(this.applicant, that.applicant) &&
    java.util.Objects.equals(this.applicationDecisionSoughtByDate, that.applicationDecisionSoughtByDate) &&
    java.util.Objects.equals(this.applicationExternalCreatorType, that.applicationExternalCreatorType) &&
    java.util.Objects.equals(this.applicationParticulars, that.applicationParticulars) &&
    java.util.Objects.equals(this.applicationReceivedDate, that.applicationReceivedDate) &&
    java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
    java.util.Objects.equals(this.applicationResultCodeForLaa, that.applicationResultCodeForLaa) &&
    java.util.Objects.equals(this.applicationStatus, that.applicationStatus) &&
    java.util.Objects.equals(this.commissionerOfOath, that.commissionerOfOath) &&
    java.util.Objects.equals(this.convictionDate, that.convictionDate) &&
    java.util.Objects.equals(this.courtApplicationCases, that.courtApplicationCases) &&
    java.util.Objects.equals(this.courtApplicationPayment, that.courtApplicationPayment) &&
    java.util.Objects.equals(this.courtCivilApplication, that.courtCivilApplication) &&
    java.util.Objects.equals(this.courtOrder, that.courtOrder) &&
    java.util.Objects.equals(this.defendantASN, that.defendantASN) &&
    java.util.Objects.equals(this.futureSummonsHearing, that.futureSummonsHearing) &&
    java.util.Objects.equals(this.hasSummonsSupplied, that.hasSummonsSupplied) &&
    java.util.Objects.equals(this.hearingIdToBeVacated, that.hearingIdToBeVacated) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isGroupCaseApplication, that.isGroupCaseApplication) &&
    java.util.Objects.equals(this.judicialResults, that.judicialResults) &&
    java.util.Objects.equals(this.laaApplnReference, that.laaApplnReference) &&
    java.util.Objects.equals(this.outOfTimeReasons, that.outOfTimeReasons) &&
    java.util.Objects.equals(this.parentApplicationId, that.parentApplicationId) &&
    java.util.Objects.equals(this.plea, that.plea) &&
    java.util.Objects.equals(this.proceedingsConcluded, that.proceedingsConcluded) &&
    java.util.Objects.equals(this.removalReason, that.removalReason) &&
    java.util.Objects.equals(this.respondents, that.respondents) &&
    java.util.Objects.equals(this.subject, that.subject) &&
    java.util.Objects.equals(this.thirdParties, that.thirdParties) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.verdict, that.verdict);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allegationOrComplaintEndDate, allegationOrComplaintStartDate, applicant, applicationDecisionSoughtByDate, applicationExternalCreatorType, applicationParticulars, applicationReceivedDate, applicationReference, applicationResultCodeForLaa, applicationStatus, commissionerOfOath, convictionDate, courtApplicationCases, courtApplicationPayment, courtCivilApplication, courtOrder, defendantASN, futureSummonsHearing, hasSummonsSupplied, hearingIdToBeVacated, id, isGroupCaseApplication, judicialResults, laaApplnReference, outOfTimeReasons, parentApplicationId, plea, proceedingsConcluded, removalReason, respondents, subject, thirdParties, type, verdict);}

  public static class Builder {
    private String allegationOrComplaintEndDate;

    private String allegationOrComplaintStartDate;

    private CourtApplicationParty applicant;

    private String applicationDecisionSoughtByDate;

    private ApplicationExternalCreatorType applicationExternalCreatorType;

    private String applicationParticulars;

    private String applicationReceivedDate;

    private String applicationReference;

    private String applicationResultCodeForLaa;

    private ApplicationStatus applicationStatus;

    private Boolean commissionerOfOath;

    private String convictionDate;

    private List<CourtApplicationCase> courtApplicationCases;

    private CourtApplicationPayment courtApplicationPayment;

    private CourtCivilApplication courtCivilApplication;

    private CourtOrder courtOrder;

    private String defendantASN;

    private FutureSummonsHearing futureSummonsHearing;

    private Boolean hasSummonsSupplied;

    private UUID hearingIdToBeVacated;

    private UUID id;

    private Boolean isGroupCaseApplication;

    private List<JudicialResult> judicialResults;

    private LaaReference laaApplnReference;

    private String outOfTimeReasons;

    private UUID parentApplicationId;

    private Plea plea;

    private Boolean proceedingsConcluded;

    private String removalReason;

    private List<CourtApplicationParty> respondents;

    private CourtApplicationParty subject;

    private List<CourtApplicationParty> thirdParties;

    private CourtApplicationType type;

    private Verdict verdict;

    public Builder withAllegationOrComplaintEndDate(final String allegationOrComplaintEndDate) {
      this.allegationOrComplaintEndDate = allegationOrComplaintEndDate;
      return this;
    }

    public Builder withAllegationOrComplaintStartDate(final String allegationOrComplaintStartDate) {
      this.allegationOrComplaintStartDate = allegationOrComplaintStartDate;
      return this;
    }

    public Builder withApplicant(final CourtApplicationParty applicant) {
      this.applicant = applicant;
      return this;
    }

    public Builder withApplicationDecisionSoughtByDate(final String applicationDecisionSoughtByDate) {
      this.applicationDecisionSoughtByDate = applicationDecisionSoughtByDate;
      return this;
    }

    public Builder withApplicationExternalCreatorType(final ApplicationExternalCreatorType applicationExternalCreatorType) {
      this.applicationExternalCreatorType = applicationExternalCreatorType;
      return this;
    }

    public Builder withApplicationParticulars(final String applicationParticulars) {
      this.applicationParticulars = applicationParticulars;
      return this;
    }

    public Builder withApplicationReceivedDate(final String applicationReceivedDate) {
      this.applicationReceivedDate = applicationReceivedDate;
      return this;
    }

    public Builder withApplicationReference(final String applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public Builder withApplicationResultCodeForLaa(final String applicationResultCodeForLaa) {
      this.applicationResultCodeForLaa = applicationResultCodeForLaa;
      return this;
    }

    public Builder withApplicationStatus(final ApplicationStatus applicationStatus) {
      this.applicationStatus = applicationStatus;
      return this;
    }

    public Builder withCommissionerOfOath(final Boolean commissionerOfOath) {
      this.commissionerOfOath = commissionerOfOath;
      return this;
    }

    public Builder withConvictionDate(final String convictionDate) {
      this.convictionDate = convictionDate;
      return this;
    }

    public Builder withCourtApplicationCases(final List<CourtApplicationCase> courtApplicationCases) {
      this.courtApplicationCases = courtApplicationCases;
      return this;
    }

    public Builder withCourtApplicationPayment(final CourtApplicationPayment courtApplicationPayment) {
      this.courtApplicationPayment = courtApplicationPayment;
      return this;
    }

    public Builder withCourtCivilApplication(final CourtCivilApplication courtCivilApplication) {
      this.courtCivilApplication = courtCivilApplication;
      return this;
    }

    public Builder withCourtOrder(final CourtOrder courtOrder) {
      this.courtOrder = courtOrder;
      return this;
    }

    public Builder withDefendantASN(final String defendantASN) {
      this.defendantASN = defendantASN;
      return this;
    }

    public Builder withFutureSummonsHearing(final FutureSummonsHearing futureSummonsHearing) {
      this.futureSummonsHearing = futureSummonsHearing;
      return this;
    }

    public Builder withHasSummonsSupplied(final Boolean hasSummonsSupplied) {
      this.hasSummonsSupplied = hasSummonsSupplied;
      return this;
    }

    public Builder withHearingIdToBeVacated(final UUID hearingIdToBeVacated) {
      this.hearingIdToBeVacated = hearingIdToBeVacated;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIsGroupCaseApplication(final Boolean isGroupCaseApplication) {
      this.isGroupCaseApplication = isGroupCaseApplication;
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

    public Builder withOutOfTimeReasons(final String outOfTimeReasons) {
      this.outOfTimeReasons = outOfTimeReasons;
      return this;
    }

    public Builder withParentApplicationId(final UUID parentApplicationId) {
      this.parentApplicationId = parentApplicationId;
      return this;
    }

    public Builder withPlea(final Plea plea) {
      this.plea = plea;
      return this;
    }

    public Builder withProceedingsConcluded(final Boolean proceedingsConcluded) {
      this.proceedingsConcluded = proceedingsConcluded;
      return this;
    }

    public Builder withRemovalReason(final String removalReason) {
      this.removalReason = removalReason;
      return this;
    }

    public Builder withRespondents(final List<CourtApplicationParty> respondents) {
      this.respondents = respondents;
      return this;
    }

    public Builder withSubject(final CourtApplicationParty subject) {
      this.subject = subject;
      return this;
    }

    public Builder withThirdParties(final List<CourtApplicationParty> thirdParties) {
      this.thirdParties = thirdParties;
      return this;
    }

    public Builder withType(final CourtApplicationType type) {
      this.type = type;
      return this;
    }

    public Builder withVerdict(final Verdict verdict) {
      this.verdict = verdict;
      return this;
    }

    public Builder withValuesFrom(final CourtApplication courtApplication) {
      this.allegationOrComplaintEndDate = courtApplication.getAllegationOrComplaintEndDate();
      this.allegationOrComplaintStartDate = courtApplication.getAllegationOrComplaintStartDate();
      this.applicant = courtApplication.getApplicant();
      this.applicationDecisionSoughtByDate = courtApplication.getApplicationDecisionSoughtByDate();
      this.applicationExternalCreatorType = courtApplication.getApplicationExternalCreatorType();
      this.applicationParticulars = courtApplication.getApplicationParticulars();
      this.applicationReceivedDate = courtApplication.getApplicationReceivedDate();
      this.applicationReference = courtApplication.getApplicationReference();
      this.applicationResultCodeForLaa = courtApplication.getApplicationResultCodeForLaa();
      this.applicationStatus = courtApplication.getApplicationStatus();
      this.commissionerOfOath = courtApplication.getCommissionerOfOath();
      this.convictionDate = courtApplication.getConvictionDate();
      this.courtApplicationCases = courtApplication.getCourtApplicationCases();
      this.courtApplicationPayment = courtApplication.getCourtApplicationPayment();
      this.courtCivilApplication = courtApplication.getCourtCivilApplication();
      this.courtOrder = courtApplication.getCourtOrder();
      this.defendantASN = courtApplication.getDefendantASN();
      this.futureSummonsHearing = courtApplication.getFutureSummonsHearing();
      this.hasSummonsSupplied = courtApplication.getHasSummonsSupplied();
      this.hearingIdToBeVacated = courtApplication.getHearingIdToBeVacated();
      this.id = courtApplication.getId();
      this.isGroupCaseApplication = courtApplication.getIsGroupCaseApplication();
      this.judicialResults = courtApplication.getJudicialResults();
      this.laaApplnReference = courtApplication.getLaaApplnReference();
      this.outOfTimeReasons = courtApplication.getOutOfTimeReasons();
      this.parentApplicationId = courtApplication.getParentApplicationId();
      this.plea = courtApplication.getPlea();
      this.proceedingsConcluded = courtApplication.getProceedingsConcluded();
      this.removalReason = courtApplication.getRemovalReason();
      this.respondents = courtApplication.getRespondents();
      this.subject = courtApplication.getSubject();
      this.thirdParties = courtApplication.getThirdParties();
      this.type = courtApplication.getType();
      this.verdict = courtApplication.getVerdict();
      return this;
    }

    public CourtApplication build() {
      return new uk.gov.justice.core.courts.CourtApplication(allegationOrComplaintEndDate, allegationOrComplaintStartDate, applicant, applicationDecisionSoughtByDate, applicationExternalCreatorType, applicationParticulars, applicationReceivedDate, applicationReference, applicationResultCodeForLaa, applicationStatus, commissionerOfOath, convictionDate, courtApplicationCases, courtApplicationPayment, courtCivilApplication, courtOrder, defendantASN, futureSummonsHearing, hasSummonsSupplied, hearingIdToBeVacated, id, isGroupCaseApplication, judicialResults, laaApplnReference, outOfTimeReasons, parentApplicationId, plea, proceedingsConcluded, removalReason, respondents, subject, thirdParties, type, verdict);
    }
  }
}

package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class ProsecutionCase implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Boolean appealProceedingsPending;

  private final Boolean breachProceedingsPending;

  private final List<Marker> caseMarkers;

  private final String caseStatus;

  private final List<CivilFees> civilFees;

  private final String classOfCase;

  private final String committalDate;

  private final String cpsOrganisation;

  private final UUID cpsOrganisationId;

  private final String dateOfSendingCase;

  private final List<Defendant> defendants;

  private final UUID groupId;

  private final UUID id;

  private final InitiationCode initiationCode;

  private final Boolean isCivil;

  private final Boolean isCpsOrgVerifyError;

  private final Boolean isGroupMaster;

  private final Boolean isGroupMember;

  private final Boolean isRetrial;

  private final MigrationSourceSystem migrationSourceSystem;

  private final String originatingOrganisation;

  private final PoliceOfficerInCase policeOfficerInCase;

  private final ProsecutionCaseIdentifier prosecutionCaseIdentifier;

  private final Prosecutor prosecutor;

  private final String receiptType;

  private final CourtCentre receivingCourt;

  private final String relatedUrn;

  private final String removalReason;

  private final Boolean retrialIndicator;

  private final CourtCentre sendingCourt;

  private final String statementOfFacts;

  private final String statementOfFactsWelsh;

  private final String summonsCode;

  private final String trialReceiptType;

  @JsonCreator
  public ProsecutionCase(final Boolean appealProceedingsPending, final Boolean breachProceedingsPending, final List<Marker> caseMarkers, final String caseStatus, final List<CivilFees> civilFees, final String classOfCase, final String committalDate, final String cpsOrganisation, final UUID cpsOrganisationId, final String dateOfSendingCase, final List<Defendant> defendants, final UUID groupId, final UUID id, final InitiationCode initiationCode, final Boolean isCivil, final Boolean isCpsOrgVerifyError, final Boolean isGroupMaster, final Boolean isGroupMember, final Boolean isRetrial, final MigrationSourceSystem migrationSourceSystem, final String originatingOrganisation, final PoliceOfficerInCase policeOfficerInCase, final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final Prosecutor prosecutor, final String receiptType, final CourtCentre receivingCourt, final String relatedUrn, final String removalReason, final Boolean retrialIndicator, final CourtCentre sendingCourt, final String statementOfFacts, final String statementOfFactsWelsh, final String summonsCode, final String trialReceiptType) {
    this.appealProceedingsPending = appealProceedingsPending;
    this.breachProceedingsPending = breachProceedingsPending;
    this.caseMarkers = caseMarkers;
    this.caseStatus = caseStatus;
    this.civilFees = civilFees;
    this.classOfCase = classOfCase;
    this.committalDate = committalDate;
    this.cpsOrganisation = cpsOrganisation;
    this.cpsOrganisationId = cpsOrganisationId;
    this.dateOfSendingCase = dateOfSendingCase;
    this.defendants = defendants;
    this.groupId = groupId;
    this.id = id;
    this.initiationCode = initiationCode;
    this.isCivil = isCivil;
    this.isCpsOrgVerifyError = isCpsOrgVerifyError;
    this.isGroupMaster = isGroupMaster;
    this.isGroupMember = isGroupMember;
    this.isRetrial = isRetrial;
    this.migrationSourceSystem = migrationSourceSystem;
    this.originatingOrganisation = originatingOrganisation;
    this.policeOfficerInCase = policeOfficerInCase;
    this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
    this.prosecutor = prosecutor;
    this.receiptType = receiptType;
    this.receivingCourt = receivingCourt;
    this.relatedUrn = relatedUrn;
    this.removalReason = removalReason;
    this.retrialIndicator = retrialIndicator;
    this.sendingCourt = sendingCourt;
    this.statementOfFacts = statementOfFacts;
    this.statementOfFactsWelsh = statementOfFactsWelsh;
    this.summonsCode = summonsCode;
    this.trialReceiptType = trialReceiptType;
  }

  public Boolean getAppealProceedingsPending() {
    return appealProceedingsPending;
  }

  public Boolean getBreachProceedingsPending() {
    return breachProceedingsPending;
  }

  public List<Marker> getCaseMarkers() {
    return caseMarkers;
  }

  public String getCaseStatus() {
    return caseStatus;
  }

  public List<CivilFees> getCivilFees() {
    return civilFees;
  }

  public String getClassOfCase() {
    return classOfCase;
  }

  public String getCommittalDate() {
    return committalDate;
  }

  public String getCpsOrganisation() {
    return cpsOrganisation;
  }

  public UUID getCpsOrganisationId() {
    return cpsOrganisationId;
  }

  public String getDateOfSendingCase() {
    return dateOfSendingCase;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public UUID getId() {
    return id;
  }

  public InitiationCode getInitiationCode() {
    return initiationCode;
  }

  public Boolean getIsCivil() {
    return isCivil;
  }

  public Boolean getIsCpsOrgVerifyError() {
    return isCpsOrgVerifyError;
  }

  public Boolean getIsGroupMaster() {
    return isGroupMaster;
  }

  public Boolean getIsGroupMember() {
    return isGroupMember;
  }

  public Boolean getIsRetrial() {
    return isRetrial;
  }

  public MigrationSourceSystem getMigrationSourceSystem() {
    return migrationSourceSystem;
  }

  public String getOriginatingOrganisation() {
    return originatingOrganisation;
  }

  public PoliceOfficerInCase getPoliceOfficerInCase() {
    return policeOfficerInCase;
  }

  public ProsecutionCaseIdentifier getProsecutionCaseIdentifier() {
    return prosecutionCaseIdentifier;
  }

  public Prosecutor getProsecutor() {
    return prosecutor;
  }

  public String getReceiptType() {
    return receiptType;
  }

  public CourtCentre getReceivingCourt() {
    return receivingCourt;
  }

  public String getRelatedUrn() {
    return relatedUrn;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public Boolean getRetrialIndicator() {
    return retrialIndicator;
  }

  public CourtCentre getSendingCourt() {
    return sendingCourt;
  }

  public String getStatementOfFacts() {
    return statementOfFacts;
  }

  public String getStatementOfFactsWelsh() {
    return statementOfFactsWelsh;
  }

  public String getSummonsCode() {
    return summonsCode;
  }

  public String getTrialReceiptType() {
    return trialReceiptType;
  }

  public static Builder prosecutionCase() {
    return new uk.gov.justice.core.courts.ProsecutionCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ProsecutionCase that = (uk.gov.justice.core.courts.ProsecutionCase) obj;

    return java.util.Objects.equals(this.appealProceedingsPending, that.appealProceedingsPending) &&
    java.util.Objects.equals(this.breachProceedingsPending, that.breachProceedingsPending) &&
    java.util.Objects.equals(this.caseMarkers, that.caseMarkers) &&
    java.util.Objects.equals(this.caseStatus, that.caseStatus) &&
    java.util.Objects.equals(this.civilFees, that.civilFees) &&
    java.util.Objects.equals(this.classOfCase, that.classOfCase) &&
    java.util.Objects.equals(this.committalDate, that.committalDate) &&
    java.util.Objects.equals(this.cpsOrganisation, that.cpsOrganisation) &&
    java.util.Objects.equals(this.cpsOrganisationId, that.cpsOrganisationId) &&
    java.util.Objects.equals(this.dateOfSendingCase, that.dateOfSendingCase) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.groupId, that.groupId) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.initiationCode, that.initiationCode) &&
    java.util.Objects.equals(this.isCivil, that.isCivil) &&
    java.util.Objects.equals(this.isCpsOrgVerifyError, that.isCpsOrgVerifyError) &&
    java.util.Objects.equals(this.isGroupMaster, that.isGroupMaster) &&
    java.util.Objects.equals(this.isGroupMember, that.isGroupMember) &&
    java.util.Objects.equals(this.isRetrial, that.isRetrial) &&
    java.util.Objects.equals(this.migrationSourceSystem, that.migrationSourceSystem) &&
    java.util.Objects.equals(this.originatingOrganisation, that.originatingOrganisation) &&
    java.util.Objects.equals(this.policeOfficerInCase, that.policeOfficerInCase) &&
    java.util.Objects.equals(this.prosecutionCaseIdentifier, that.prosecutionCaseIdentifier) &&
    java.util.Objects.equals(this.prosecutor, that.prosecutor) &&
    java.util.Objects.equals(this.receiptType, that.receiptType) &&
    java.util.Objects.equals(this.receivingCourt, that.receivingCourt) &&
    java.util.Objects.equals(this.relatedUrn, that.relatedUrn) &&
    java.util.Objects.equals(this.removalReason, that.removalReason) &&
    java.util.Objects.equals(this.retrialIndicator, that.retrialIndicator) &&
    java.util.Objects.equals(this.sendingCourt, that.sendingCourt) &&
    java.util.Objects.equals(this.statementOfFacts, that.statementOfFacts) &&
    java.util.Objects.equals(this.statementOfFactsWelsh, that.statementOfFactsWelsh) &&
    java.util.Objects.equals(this.summonsCode, that.summonsCode) &&
    java.util.Objects.equals(this.trialReceiptType, that.trialReceiptType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(appealProceedingsPending, breachProceedingsPending, caseMarkers, caseStatus, civilFees, classOfCase, committalDate, cpsOrganisation, cpsOrganisationId, dateOfSendingCase, defendants, groupId, id, initiationCode, isCivil, isCpsOrgVerifyError, isGroupMaster, isGroupMember, isRetrial, migrationSourceSystem, originatingOrganisation, policeOfficerInCase, prosecutionCaseIdentifier, prosecutor, receiptType, receivingCourt, relatedUrn, removalReason, retrialIndicator, sendingCourt, statementOfFacts, statementOfFactsWelsh, summonsCode, trialReceiptType);}

  public static class Builder {
    private Boolean appealProceedingsPending;

    private Boolean breachProceedingsPending;

    private List<Marker> caseMarkers;

    private String caseStatus;

    private List<CivilFees> civilFees;

    private String classOfCase;

    private String committalDate;

    private String cpsOrganisation;

    private UUID cpsOrganisationId;

    private String dateOfSendingCase;

    private List<Defendant> defendants;

    private UUID groupId;

    private UUID id;

    private InitiationCode initiationCode;

    private Boolean isCivil;

    private Boolean isCpsOrgVerifyError;

    private Boolean isGroupMaster;

    private Boolean isGroupMember;

    private Boolean isRetrial;

    private MigrationSourceSystem migrationSourceSystem;

    private String originatingOrganisation;

    private PoliceOfficerInCase policeOfficerInCase;

    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    private Prosecutor prosecutor;

    private String receiptType;

    private CourtCentre receivingCourt;

    private String relatedUrn;

    private String removalReason;

    private Boolean retrialIndicator;

    private CourtCentre sendingCourt;

    private String statementOfFacts;

    private String statementOfFactsWelsh;

    private String summonsCode;

    private String trialReceiptType;

    public Builder withAppealProceedingsPending(final Boolean appealProceedingsPending) {
      this.appealProceedingsPending = appealProceedingsPending;
      return this;
    }

    public Builder withBreachProceedingsPending(final Boolean breachProceedingsPending) {
      this.breachProceedingsPending = breachProceedingsPending;
      return this;
    }

    public Builder withCaseMarkers(final List<Marker> caseMarkers) {
      this.caseMarkers = caseMarkers;
      return this;
    }

    public Builder withCaseStatus(final String caseStatus) {
      this.caseStatus = caseStatus;
      return this;
    }

    public Builder withCivilFees(final List<CivilFees> civilFees) {
      this.civilFees = civilFees;
      return this;
    }

    public Builder withClassOfCase(final String classOfCase) {
      this.classOfCase = classOfCase;
      return this;
    }

    public Builder withCommittalDate(final String committalDate) {
      this.committalDate = committalDate;
      return this;
    }

    public Builder withCpsOrganisation(final String cpsOrganisation) {
      this.cpsOrganisation = cpsOrganisation;
      return this;
    }

    public Builder withCpsOrganisationId(final UUID cpsOrganisationId) {
      this.cpsOrganisationId = cpsOrganisationId;
      return this;
    }

    public Builder withDateOfSendingCase(final String dateOfSendingCase) {
      this.dateOfSendingCase = dateOfSendingCase;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withGroupId(final UUID groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withInitiationCode(final InitiationCode initiationCode) {
      this.initiationCode = initiationCode;
      return this;
    }

    public Builder withIsCivil(final Boolean isCivil) {
      this.isCivil = isCivil;
      return this;
    }

    public Builder withIsCpsOrgVerifyError(final Boolean isCpsOrgVerifyError) {
      this.isCpsOrgVerifyError = isCpsOrgVerifyError;
      return this;
    }

    public Builder withIsGroupMaster(final Boolean isGroupMaster) {
      this.isGroupMaster = isGroupMaster;
      return this;
    }

    public Builder withIsGroupMember(final Boolean isGroupMember) {
      this.isGroupMember = isGroupMember;
      return this;
    }

    public Builder withIsRetrial(final Boolean isRetrial) {
      this.isRetrial = isRetrial;
      return this;
    }

    public Builder withMigrationSourceSystem(final MigrationSourceSystem migrationSourceSystem) {
      this.migrationSourceSystem = migrationSourceSystem;
      return this;
    }

    public Builder withOriginatingOrganisation(final String originatingOrganisation) {
      this.originatingOrganisation = originatingOrganisation;
      return this;
    }

    public Builder withPoliceOfficerInCase(final PoliceOfficerInCase policeOfficerInCase) {
      this.policeOfficerInCase = policeOfficerInCase;
      return this;
    }

    public Builder withProsecutionCaseIdentifier(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
      this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
      return this;
    }

    public Builder withProsecutor(final Prosecutor prosecutor) {
      this.prosecutor = prosecutor;
      return this;
    }

    public Builder withReceiptType(final String receiptType) {
      this.receiptType = receiptType;
      return this;
    }

    public Builder withReceivingCourt(final CourtCentre receivingCourt) {
      this.receivingCourt = receivingCourt;
      return this;
    }

    public Builder withRelatedUrn(final String relatedUrn) {
      this.relatedUrn = relatedUrn;
      return this;
    }

    public Builder withRemovalReason(final String removalReason) {
      this.removalReason = removalReason;
      return this;
    }

    public Builder withRetrialIndicator(final Boolean retrialIndicator) {
      this.retrialIndicator = retrialIndicator;
      return this;
    }

    public Builder withSendingCourt(final CourtCentre sendingCourt) {
      this.sendingCourt = sendingCourt;
      return this;
    }

    public Builder withStatementOfFacts(final String statementOfFacts) {
      this.statementOfFacts = statementOfFacts;
      return this;
    }

    public Builder withStatementOfFactsWelsh(final String statementOfFactsWelsh) {
      this.statementOfFactsWelsh = statementOfFactsWelsh;
      return this;
    }

    public Builder withSummonsCode(final String summonsCode) {
      this.summonsCode = summonsCode;
      return this;
    }

    public Builder withTrialReceiptType(final String trialReceiptType) {
      this.trialReceiptType = trialReceiptType;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCase prosecutionCase) {
      this.appealProceedingsPending = prosecutionCase.getAppealProceedingsPending();
      this.breachProceedingsPending = prosecutionCase.getBreachProceedingsPending();
      this.caseMarkers = prosecutionCase.getCaseMarkers();
      this.caseStatus = prosecutionCase.getCaseStatus();
      this.civilFees = prosecutionCase.getCivilFees();
      this.classOfCase = prosecutionCase.getClassOfCase();
      this.committalDate = prosecutionCase.getCommittalDate();
      this.cpsOrganisation = prosecutionCase.getCpsOrganisation();
      this.cpsOrganisationId = prosecutionCase.getCpsOrganisationId();
      this.dateOfSendingCase = prosecutionCase.getDateOfSendingCase();
      this.defendants = prosecutionCase.getDefendants();
      this.groupId = prosecutionCase.getGroupId();
      this.id = prosecutionCase.getId();
      this.initiationCode = prosecutionCase.getInitiationCode();
      this.isCivil = prosecutionCase.getIsCivil();
      this.isCpsOrgVerifyError = prosecutionCase.getIsCpsOrgVerifyError();
      this.isGroupMaster = prosecutionCase.getIsGroupMaster();
      this.isGroupMember = prosecutionCase.getIsGroupMember();
      this.isRetrial = prosecutionCase.getIsRetrial();
      this.migrationSourceSystem = prosecutionCase.getMigrationSourceSystem();
      this.originatingOrganisation = prosecutionCase.getOriginatingOrganisation();
      this.policeOfficerInCase = prosecutionCase.getPoliceOfficerInCase();
      this.prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
      this.prosecutor = prosecutionCase.getProsecutor();
      this.receiptType = prosecutionCase.getReceiptType();
      this.receivingCourt = prosecutionCase.getReceivingCourt();
      this.relatedUrn = prosecutionCase.getRelatedUrn();
      this.removalReason = prosecutionCase.getRemovalReason();
      this.retrialIndicator = prosecutionCase.getRetrialIndicator();
      this.sendingCourt = prosecutionCase.getSendingCourt();
      this.statementOfFacts = prosecutionCase.getStatementOfFacts();
      this.statementOfFactsWelsh = prosecutionCase.getStatementOfFactsWelsh();
      this.summonsCode = prosecutionCase.getSummonsCode();
      this.trialReceiptType = prosecutionCase.getTrialReceiptType();
      return this;
    }

    public ProsecutionCase build() {
      return new uk.gov.justice.core.courts.ProsecutionCase(appealProceedingsPending, breachProceedingsPending, caseMarkers, caseStatus, civilFees, classOfCase, committalDate, cpsOrganisation, cpsOrganisationId, dateOfSendingCase, defendants, groupId, id, initiationCode, isCivil, isCpsOrgVerifyError, isGroupMaster, isGroupMember, isRetrial, migrationSourceSystem, originatingOrganisation, policeOfficerInCase, prosecutionCaseIdentifier, prosecutor, receiptType, receivingCourt, relatedUrn, removalReason, retrialIndicator, sendingCourt, statementOfFacts, statementOfFactsWelsh, summonsCode, trialReceiptType);
    }
  }
}

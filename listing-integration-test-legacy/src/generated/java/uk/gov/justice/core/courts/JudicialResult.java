package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class JudicialResult implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Boolean alwaysPublished;

  private final String amendmentDate;

  private final String amendmentReason;

  private final UUID amendmentReasonId;

  private final String approvedDate;

  private final Boolean canBeSubjectOfBreach;

  private final Boolean canBeSubjectOfVariation;

  private final Boolean canExtendActiveOrder;

  private final JudicialResultCategory category;

  private final String cjsCode;

  private final Boolean committedToCC;

  private final DelegatedPowers courtClerk;

  private final Boolean d20;

  private final DelegatedPowers delegatedPowers;

  private final Integer drivingTestStipulation;

  private final JudicialResultPromptDurationElement durationElement;

  private final String dvlaCode;

  private final Boolean excludedFromResults;

  private final DelegatedPowers fourEyesApproval;

  private final Boolean isAdjournmentResult;

  private final Boolean isAvailableForCourtExtract;

  private final Boolean isConvictedResult;

  private final Boolean isDeemedServed;

  private final Boolean isDeleted;

  private final Boolean isFinancialResult;

  private final Boolean isNewAmendment;

  private final Boolean isUnscheduled;

  private final UUID judicialResultId;

  private final List<JudicialResultPrompt> judicialResultPrompts;

  private final UUID judicialResultTypeId;

  private final String label;

  private final String lastSharedDateTime;

  private final String level;

  private final Boolean lifeDuration;

  private final NextHearing nextHearing;

  private final UUID offenceId;

  private final String orderedDate;

  private final UUID orderedHearingId;

  private final UUID parentJudicialResultId;

  private final UUID parentJudicialResultTypeId;

  private final String pointsDisqualificationCode;

  private final String policeSubjectLineTitle;

  private final String postHearingCustodyStatus;

  private final Boolean preserveActiveOrder;

  private final Boolean publishedAsAPrompt;

  private final Boolean publishedForNows;

  private final String qualifier;

  private final BigDecimal rank;

  private final String resultDefinitionGroup;

  private final String resultText;

  private final String resultWording;

  private final Boolean rollUpPrompts;

  private final UUID rootJudicialResultId;

  private final UUID rootJudicialResultTypeId;

  private final List<SecondaryCJSCode> secondaryCJSCodes;

  private final Boolean sentToCC;

  private final Boolean terminatesOffenceProceedings;

  private final Boolean urgent;

  private final List<String> usergroups;

  private final String welshLabel;

  private final String welshResultWording;

  @JsonCreator
  public JudicialResult(final Boolean alwaysPublished, final String amendmentDate, final String amendmentReason, final UUID amendmentReasonId, final String approvedDate, final Boolean canBeSubjectOfBreach, final Boolean canBeSubjectOfVariation, final Boolean canExtendActiveOrder, final JudicialResultCategory category, final String cjsCode, final Boolean committedToCC, final DelegatedPowers courtClerk, final Boolean d20, final DelegatedPowers delegatedPowers, final Integer drivingTestStipulation, final JudicialResultPromptDurationElement durationElement, final String dvlaCode, final Boolean excludedFromResults, final DelegatedPowers fourEyesApproval, final Boolean isAdjournmentResult, final Boolean isAvailableForCourtExtract, final Boolean isConvictedResult, final Boolean isDeemedServed, final Boolean isDeleted, final Boolean isFinancialResult, final Boolean isNewAmendment, final Boolean isUnscheduled, final UUID judicialResultId, final List<JudicialResultPrompt> judicialResultPrompts, final UUID judicialResultTypeId, final String label, final String lastSharedDateTime, final String level, final Boolean lifeDuration, final NextHearing nextHearing, final UUID offenceId, final String orderedDate, final UUID orderedHearingId, final UUID parentJudicialResultId, final UUID parentJudicialResultTypeId, final String pointsDisqualificationCode, final String policeSubjectLineTitle, final String postHearingCustodyStatus, final Boolean preserveActiveOrder, final Boolean publishedAsAPrompt, final Boolean publishedForNows, final String qualifier, final BigDecimal rank, final String resultDefinitionGroup, final String resultText, final String resultWording, final Boolean rollUpPrompts, final UUID rootJudicialResultId, final UUID rootJudicialResultTypeId, final List<SecondaryCJSCode> secondaryCJSCodes, final Boolean sentToCC, final Boolean terminatesOffenceProceedings, final Boolean urgent, final List<String> usergroups, final String welshLabel, final String welshResultWording) {
    this.alwaysPublished = alwaysPublished;
    this.amendmentDate = amendmentDate;
    this.amendmentReason = amendmentReason;
    this.amendmentReasonId = amendmentReasonId;
    this.approvedDate = approvedDate;
    this.canBeSubjectOfBreach = canBeSubjectOfBreach;
    this.canBeSubjectOfVariation = canBeSubjectOfVariation;
    this.canExtendActiveOrder = canExtendActiveOrder;
    this.category = category;
    this.cjsCode = cjsCode;
    this.committedToCC = committedToCC;
    this.courtClerk = courtClerk;
    this.d20 = d20;
    this.delegatedPowers = delegatedPowers;
    this.drivingTestStipulation = drivingTestStipulation;
    this.durationElement = durationElement;
    this.dvlaCode = dvlaCode;
    this.excludedFromResults = excludedFromResults;
    this.fourEyesApproval = fourEyesApproval;
    this.isAdjournmentResult = isAdjournmentResult;
    this.isAvailableForCourtExtract = isAvailableForCourtExtract;
    this.isConvictedResult = isConvictedResult;
    this.isDeemedServed = isDeemedServed;
    this.isDeleted = isDeleted;
    this.isFinancialResult = isFinancialResult;
    this.isNewAmendment = isNewAmendment;
    this.isUnscheduled = isUnscheduled;
    this.judicialResultId = judicialResultId;
    this.judicialResultPrompts = judicialResultPrompts;
    this.judicialResultTypeId = judicialResultTypeId;
    this.label = label;
    this.lastSharedDateTime = lastSharedDateTime;
    this.level = level;
    this.lifeDuration = lifeDuration;
    this.nextHearing = nextHearing;
    this.offenceId = offenceId;
    this.orderedDate = orderedDate;
    this.orderedHearingId = orderedHearingId;
    this.parentJudicialResultId = parentJudicialResultId;
    this.parentJudicialResultTypeId = parentJudicialResultTypeId;
    this.pointsDisqualificationCode = pointsDisqualificationCode;
    this.policeSubjectLineTitle = policeSubjectLineTitle;
    this.postHearingCustodyStatus = postHearingCustodyStatus;
    this.preserveActiveOrder = preserveActiveOrder;
    this.publishedAsAPrompt = publishedAsAPrompt;
    this.publishedForNows = publishedForNows;
    this.qualifier = qualifier;
    this.rank = rank;
    this.resultDefinitionGroup = resultDefinitionGroup;
    this.resultText = resultText;
    this.resultWording = resultWording;
    this.rollUpPrompts = rollUpPrompts;
    this.rootJudicialResultId = rootJudicialResultId;
    this.rootJudicialResultTypeId = rootJudicialResultTypeId;
    this.secondaryCJSCodes = secondaryCJSCodes;
    this.sentToCC = sentToCC;
    this.terminatesOffenceProceedings = terminatesOffenceProceedings;
    this.urgent = urgent;
    this.usergroups = usergroups;
    this.welshLabel = welshLabel;
    this.welshResultWording = welshResultWording;
  }

  public Boolean getAlwaysPublished() {
    return alwaysPublished;
  }

  public String getAmendmentDate() {
    return amendmentDate;
  }

  public String getAmendmentReason() {
    return amendmentReason;
  }

  public UUID getAmendmentReasonId() {
    return amendmentReasonId;
  }

  public String getApprovedDate() {
    return approvedDate;
  }

  public Boolean getCanBeSubjectOfBreach() {
    return canBeSubjectOfBreach;
  }

  public Boolean getCanBeSubjectOfVariation() {
    return canBeSubjectOfVariation;
  }

  public Boolean getCanExtendActiveOrder() {
    return canExtendActiveOrder;
  }

  public JudicialResultCategory getCategory() {
    return category;
  }

  public String getCjsCode() {
    return cjsCode;
  }

  public Boolean getCommittedToCC() {
    return committedToCC;
  }

  public DelegatedPowers getCourtClerk() {
    return courtClerk;
  }

  public Boolean getD20() {
    return d20;
  }

  public DelegatedPowers getDelegatedPowers() {
    return delegatedPowers;
  }

  public Integer getDrivingTestStipulation() {
    return drivingTestStipulation;
  }

  public JudicialResultPromptDurationElement getDurationElement() {
    return durationElement;
  }

  public String getDvlaCode() {
    return dvlaCode;
  }

  public Boolean getExcludedFromResults() {
    return excludedFromResults;
  }

  public DelegatedPowers getFourEyesApproval() {
    return fourEyesApproval;
  }

  public Boolean getIsAdjournmentResult() {
    return isAdjournmentResult;
  }

  public Boolean getIsAvailableForCourtExtract() {
    return isAvailableForCourtExtract;
  }

  public Boolean getIsConvictedResult() {
    return isConvictedResult;
  }

  public Boolean getIsDeemedServed() {
    return isDeemedServed;
  }

  public Boolean getIsDeleted() {
    return isDeleted;
  }

  public Boolean getIsFinancialResult() {
    return isFinancialResult;
  }

  public Boolean getIsNewAmendment() {
    return isNewAmendment;
  }

  public Boolean getIsUnscheduled() {
    return isUnscheduled;
  }

  public UUID getJudicialResultId() {
    return judicialResultId;
  }

  public List<JudicialResultPrompt> getJudicialResultPrompts() {
    return judicialResultPrompts;
  }

  public UUID getJudicialResultTypeId() {
    return judicialResultTypeId;
  }

  public String getLabel() {
    return label;
  }

  public String getLastSharedDateTime() {
    return lastSharedDateTime;
  }

  public String getLevel() {
    return level;
  }

  public Boolean getLifeDuration() {
    return lifeDuration;
  }

  public NextHearing getNextHearing() {
    return nextHearing;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public String getOrderedDate() {
    return orderedDate;
  }

  public UUID getOrderedHearingId() {
    return orderedHearingId;
  }

  public UUID getParentJudicialResultId() {
    return parentJudicialResultId;
  }

  public UUID getParentJudicialResultTypeId() {
    return parentJudicialResultTypeId;
  }

  public String getPointsDisqualificationCode() {
    return pointsDisqualificationCode;
  }

  public String getPoliceSubjectLineTitle() {
    return policeSubjectLineTitle;
  }

  public String getPostHearingCustodyStatus() {
    return postHearingCustodyStatus;
  }

  public Boolean getPreserveActiveOrder() {
    return preserveActiveOrder;
  }

  public Boolean getPublishedAsAPrompt() {
    return publishedAsAPrompt;
  }

  public Boolean getPublishedForNows() {
    return publishedForNows;
  }

  public String getQualifier() {
    return qualifier;
  }

  public BigDecimal getRank() {
    return rank;
  }

  public String getResultDefinitionGroup() {
    return resultDefinitionGroup;
  }

  public String getResultText() {
    return resultText;
  }

  public String getResultWording() {
    return resultWording;
  }

  public Boolean getRollUpPrompts() {
    return rollUpPrompts;
  }

  public UUID getRootJudicialResultId() {
    return rootJudicialResultId;
  }

  public UUID getRootJudicialResultTypeId() {
    return rootJudicialResultTypeId;
  }

  public List<SecondaryCJSCode> getSecondaryCJSCodes() {
    return secondaryCJSCodes;
  }

  public Boolean getSentToCC() {
    return sentToCC;
  }

  public Boolean getTerminatesOffenceProceedings() {
    return terminatesOffenceProceedings;
  }

  public Boolean getUrgent() {
    return urgent;
  }

  public List<String> getUsergroups() {
    return usergroups;
  }

  public String getWelshLabel() {
    return welshLabel;
  }

  public String getWelshResultWording() {
    return welshResultWording;
  }

  public static Builder judicialResult() {
    return new uk.gov.justice.core.courts.JudicialResult.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.JudicialResult that = (uk.gov.justice.core.courts.JudicialResult) obj;

    return java.util.Objects.equals(this.alwaysPublished, that.alwaysPublished) &&
    java.util.Objects.equals(this.amendmentDate, that.amendmentDate) &&
    java.util.Objects.equals(this.amendmentReason, that.amendmentReason) &&
    java.util.Objects.equals(this.amendmentReasonId, that.amendmentReasonId) &&
    java.util.Objects.equals(this.approvedDate, that.approvedDate) &&
    java.util.Objects.equals(this.canBeSubjectOfBreach, that.canBeSubjectOfBreach) &&
    java.util.Objects.equals(this.canBeSubjectOfVariation, that.canBeSubjectOfVariation) &&
    java.util.Objects.equals(this.canExtendActiveOrder, that.canExtendActiveOrder) &&
    java.util.Objects.equals(this.category, that.category) &&
    java.util.Objects.equals(this.cjsCode, that.cjsCode) &&
    java.util.Objects.equals(this.committedToCC, that.committedToCC) &&
    java.util.Objects.equals(this.courtClerk, that.courtClerk) &&
    java.util.Objects.equals(this.d20, that.d20) &&
    java.util.Objects.equals(this.delegatedPowers, that.delegatedPowers) &&
    java.util.Objects.equals(this.drivingTestStipulation, that.drivingTestStipulation) &&
    java.util.Objects.equals(this.durationElement, that.durationElement) &&
    java.util.Objects.equals(this.dvlaCode, that.dvlaCode) &&
    java.util.Objects.equals(this.excludedFromResults, that.excludedFromResults) &&
    java.util.Objects.equals(this.fourEyesApproval, that.fourEyesApproval) &&
    java.util.Objects.equals(this.isAdjournmentResult, that.isAdjournmentResult) &&
    java.util.Objects.equals(this.isAvailableForCourtExtract, that.isAvailableForCourtExtract) &&
    java.util.Objects.equals(this.isConvictedResult, that.isConvictedResult) &&
    java.util.Objects.equals(this.isDeemedServed, that.isDeemedServed) &&
    java.util.Objects.equals(this.isDeleted, that.isDeleted) &&
    java.util.Objects.equals(this.isFinancialResult, that.isFinancialResult) &&
    java.util.Objects.equals(this.isNewAmendment, that.isNewAmendment) &&
    java.util.Objects.equals(this.isUnscheduled, that.isUnscheduled) &&
    java.util.Objects.equals(this.judicialResultId, that.judicialResultId) &&
    java.util.Objects.equals(this.judicialResultPrompts, that.judicialResultPrompts) &&
    java.util.Objects.equals(this.judicialResultTypeId, that.judicialResultTypeId) &&
    java.util.Objects.equals(this.label, that.label) &&
    java.util.Objects.equals(this.lastSharedDateTime, that.lastSharedDateTime) &&
    java.util.Objects.equals(this.level, that.level) &&
    java.util.Objects.equals(this.lifeDuration, that.lifeDuration) &&
    java.util.Objects.equals(this.nextHearing, that.nextHearing) &&
    java.util.Objects.equals(this.offenceId, that.offenceId) &&
    java.util.Objects.equals(this.orderedDate, that.orderedDate) &&
    java.util.Objects.equals(this.orderedHearingId, that.orderedHearingId) &&
    java.util.Objects.equals(this.parentJudicialResultId, that.parentJudicialResultId) &&
    java.util.Objects.equals(this.parentJudicialResultTypeId, that.parentJudicialResultTypeId) &&
    java.util.Objects.equals(this.pointsDisqualificationCode, that.pointsDisqualificationCode) &&
    java.util.Objects.equals(this.policeSubjectLineTitle, that.policeSubjectLineTitle) &&
    java.util.Objects.equals(this.postHearingCustodyStatus, that.postHearingCustodyStatus) &&
    java.util.Objects.equals(this.preserveActiveOrder, that.preserveActiveOrder) &&
    java.util.Objects.equals(this.publishedAsAPrompt, that.publishedAsAPrompt) &&
    java.util.Objects.equals(this.publishedForNows, that.publishedForNows) &&
    java.util.Objects.equals(this.qualifier, that.qualifier) &&
    java.util.Objects.equals(this.rank, that.rank) &&
    java.util.Objects.equals(this.resultDefinitionGroup, that.resultDefinitionGroup) &&
    java.util.Objects.equals(this.resultText, that.resultText) &&
    java.util.Objects.equals(this.resultWording, that.resultWording) &&
    java.util.Objects.equals(this.rollUpPrompts, that.rollUpPrompts) &&
    java.util.Objects.equals(this.rootJudicialResultId, that.rootJudicialResultId) &&
    java.util.Objects.equals(this.rootJudicialResultTypeId, that.rootJudicialResultTypeId) &&
    java.util.Objects.equals(this.secondaryCJSCodes, that.secondaryCJSCodes) &&
    java.util.Objects.equals(this.sentToCC, that.sentToCC) &&
    java.util.Objects.equals(this.terminatesOffenceProceedings, that.terminatesOffenceProceedings) &&
    java.util.Objects.equals(this.urgent, that.urgent) &&
    java.util.Objects.equals(this.usergroups, that.usergroups) &&
    java.util.Objects.equals(this.welshLabel, that.welshLabel) &&
    java.util.Objects.equals(this.welshResultWording, that.welshResultWording);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(alwaysPublished, amendmentDate, amendmentReason, amendmentReasonId, approvedDate, canBeSubjectOfBreach, canBeSubjectOfVariation, canExtendActiveOrder, category, cjsCode, committedToCC, courtClerk, d20, delegatedPowers, drivingTestStipulation, durationElement, dvlaCode, excludedFromResults, fourEyesApproval, isAdjournmentResult, isAvailableForCourtExtract, isConvictedResult, isDeemedServed, isDeleted, isFinancialResult, isNewAmendment, isUnscheduled, judicialResultId, judicialResultPrompts, judicialResultTypeId, label, lastSharedDateTime, level, lifeDuration, nextHearing, offenceId, orderedDate, orderedHearingId, parentJudicialResultId, parentJudicialResultTypeId, pointsDisqualificationCode, policeSubjectLineTitle, postHearingCustodyStatus, preserveActiveOrder, publishedAsAPrompt, publishedForNows, qualifier, rank, resultDefinitionGroup, resultText, resultWording, rollUpPrompts, rootJudicialResultId, rootJudicialResultTypeId, secondaryCJSCodes, sentToCC, terminatesOffenceProceedings, urgent, usergroups, welshLabel, welshResultWording);}

  public static class Builder {
    private Boolean alwaysPublished;

    private String amendmentDate;

    private String amendmentReason;

    private UUID amendmentReasonId;

    private String approvedDate;

    private Boolean canBeSubjectOfBreach;

    private Boolean canBeSubjectOfVariation;

    private Boolean canExtendActiveOrder;

    private JudicialResultCategory category;

    private String cjsCode;

    private Boolean committedToCC;

    private DelegatedPowers courtClerk;

    private Boolean d20;

    private DelegatedPowers delegatedPowers;

    private Integer drivingTestStipulation;

    private JudicialResultPromptDurationElement durationElement;

    private String dvlaCode;

    private Boolean excludedFromResults;

    private DelegatedPowers fourEyesApproval;

    private Boolean isAdjournmentResult;

    private Boolean isAvailableForCourtExtract;

    private Boolean isConvictedResult;

    private Boolean isDeemedServed;

    private Boolean isDeleted;

    private Boolean isFinancialResult;

    private Boolean isNewAmendment;

    private Boolean isUnscheduled;

    private UUID judicialResultId;

    private List<JudicialResultPrompt> judicialResultPrompts;

    private UUID judicialResultTypeId;

    private String label;

    private String lastSharedDateTime;

    private String level;

    private Boolean lifeDuration;

    private NextHearing nextHearing;

    private UUID offenceId;

    private String orderedDate;

    private UUID orderedHearingId;

    private UUID parentJudicialResultId;

    private UUID parentJudicialResultTypeId;

    private String pointsDisqualificationCode;

    private String policeSubjectLineTitle;

    private String postHearingCustodyStatus;

    private Boolean preserveActiveOrder;

    private Boolean publishedAsAPrompt;

    private Boolean publishedForNows;

    private String qualifier;

    private BigDecimal rank;

    private String resultDefinitionGroup;

    private String resultText;

    private String resultWording;

    private Boolean rollUpPrompts;

    private UUID rootJudicialResultId;

    private UUID rootJudicialResultTypeId;

    private List<SecondaryCJSCode> secondaryCJSCodes;

    private Boolean sentToCC;

    private Boolean terminatesOffenceProceedings;

    private Boolean urgent;

    private List<String> usergroups;

    private String welshLabel;

    private String welshResultWording;

    public Builder withAlwaysPublished(final Boolean alwaysPublished) {
      this.alwaysPublished = alwaysPublished;
      return this;
    }

    public Builder withAmendmentDate(final String amendmentDate) {
      this.amendmentDate = amendmentDate;
      return this;
    }

    public Builder withAmendmentReason(final String amendmentReason) {
      this.amendmentReason = amendmentReason;
      return this;
    }

    public Builder withAmendmentReasonId(final UUID amendmentReasonId) {
      this.amendmentReasonId = amendmentReasonId;
      return this;
    }

    public Builder withApprovedDate(final String approvedDate) {
      this.approvedDate = approvedDate;
      return this;
    }

    public Builder withCanBeSubjectOfBreach(final Boolean canBeSubjectOfBreach) {
      this.canBeSubjectOfBreach = canBeSubjectOfBreach;
      return this;
    }

    public Builder withCanBeSubjectOfVariation(final Boolean canBeSubjectOfVariation) {
      this.canBeSubjectOfVariation = canBeSubjectOfVariation;
      return this;
    }

    public Builder withCanExtendActiveOrder(final Boolean canExtendActiveOrder) {
      this.canExtendActiveOrder = canExtendActiveOrder;
      return this;
    }

    public Builder withCategory(final JudicialResultCategory category) {
      this.category = category;
      return this;
    }

    public Builder withCjsCode(final String cjsCode) {
      this.cjsCode = cjsCode;
      return this;
    }

    public Builder withCommittedToCC(final Boolean committedToCC) {
      this.committedToCC = committedToCC;
      return this;
    }

    public Builder withCourtClerk(final DelegatedPowers courtClerk) {
      this.courtClerk = courtClerk;
      return this;
    }

    public Builder withD20(final Boolean d20) {
      this.d20 = d20;
      return this;
    }

    public Builder withDelegatedPowers(final DelegatedPowers delegatedPowers) {
      this.delegatedPowers = delegatedPowers;
      return this;
    }

    public Builder withDrivingTestStipulation(final Integer drivingTestStipulation) {
      this.drivingTestStipulation = drivingTestStipulation;
      return this;
    }

    public Builder withDurationElement(final JudicialResultPromptDurationElement durationElement) {
      this.durationElement = durationElement;
      return this;
    }

    public Builder withDvlaCode(final String dvlaCode) {
      this.dvlaCode = dvlaCode;
      return this;
    }

    public Builder withExcludedFromResults(final Boolean excludedFromResults) {
      this.excludedFromResults = excludedFromResults;
      return this;
    }

    public Builder withFourEyesApproval(final DelegatedPowers fourEyesApproval) {
      this.fourEyesApproval = fourEyesApproval;
      return this;
    }

    public Builder withIsAdjournmentResult(final Boolean isAdjournmentResult) {
      this.isAdjournmentResult = isAdjournmentResult;
      return this;
    }

    public Builder withIsAvailableForCourtExtract(final Boolean isAvailableForCourtExtract) {
      this.isAvailableForCourtExtract = isAvailableForCourtExtract;
      return this;
    }

    public Builder withIsConvictedResult(final Boolean isConvictedResult) {
      this.isConvictedResult = isConvictedResult;
      return this;
    }

    public Builder withIsDeemedServed(final Boolean isDeemedServed) {
      this.isDeemedServed = isDeemedServed;
      return this;
    }

    public Builder withIsDeleted(final Boolean isDeleted) {
      this.isDeleted = isDeleted;
      return this;
    }

    public Builder withIsFinancialResult(final Boolean isFinancialResult) {
      this.isFinancialResult = isFinancialResult;
      return this;
    }

    public Builder withIsNewAmendment(final Boolean isNewAmendment) {
      this.isNewAmendment = isNewAmendment;
      return this;
    }

    public Builder withIsUnscheduled(final Boolean isUnscheduled) {
      this.isUnscheduled = isUnscheduled;
      return this;
    }

    public Builder withJudicialResultId(final UUID judicialResultId) {
      this.judicialResultId = judicialResultId;
      return this;
    }

    public Builder withJudicialResultPrompts(final List<JudicialResultPrompt> judicialResultPrompts) {
      this.judicialResultPrompts = judicialResultPrompts;
      return this;
    }

    public Builder withJudicialResultTypeId(final UUID judicialResultTypeId) {
      this.judicialResultTypeId = judicialResultTypeId;
      return this;
    }

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withLastSharedDateTime(final String lastSharedDateTime) {
      this.lastSharedDateTime = lastSharedDateTime;
      return this;
    }

    public Builder withLevel(final String level) {
      this.level = level;
      return this;
    }

    public Builder withLifeDuration(final Boolean lifeDuration) {
      this.lifeDuration = lifeDuration;
      return this;
    }

    public Builder withNextHearing(final NextHearing nextHearing) {
      this.nextHearing = nextHearing;
      return this;
    }

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withOrderedDate(final String orderedDate) {
      this.orderedDate = orderedDate;
      return this;
    }

    public Builder withOrderedHearingId(final UUID orderedHearingId) {
      this.orderedHearingId = orderedHearingId;
      return this;
    }

    public Builder withParentJudicialResultId(final UUID parentJudicialResultId) {
      this.parentJudicialResultId = parentJudicialResultId;
      return this;
    }

    public Builder withParentJudicialResultTypeId(final UUID parentJudicialResultTypeId) {
      this.parentJudicialResultTypeId = parentJudicialResultTypeId;
      return this;
    }

    public Builder withPointsDisqualificationCode(final String pointsDisqualificationCode) {
      this.pointsDisqualificationCode = pointsDisqualificationCode;
      return this;
    }

    public Builder withPoliceSubjectLineTitle(final String policeSubjectLineTitle) {
      this.policeSubjectLineTitle = policeSubjectLineTitle;
      return this;
    }

    public Builder withPostHearingCustodyStatus(final String postHearingCustodyStatus) {
      this.postHearingCustodyStatus = postHearingCustodyStatus;
      return this;
    }

    public Builder withPreserveActiveOrder(final Boolean preserveActiveOrder) {
      this.preserveActiveOrder = preserveActiveOrder;
      return this;
    }

    public Builder withPublishedAsAPrompt(final Boolean publishedAsAPrompt) {
      this.publishedAsAPrompt = publishedAsAPrompt;
      return this;
    }

    public Builder withPublishedForNows(final Boolean publishedForNows) {
      this.publishedForNows = publishedForNows;
      return this;
    }

    public Builder withQualifier(final String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    public Builder withRank(final BigDecimal rank) {
      this.rank = rank;
      return this;
    }

    public Builder withResultDefinitionGroup(final String resultDefinitionGroup) {
      this.resultDefinitionGroup = resultDefinitionGroup;
      return this;
    }

    public Builder withResultText(final String resultText) {
      this.resultText = resultText;
      return this;
    }

    public Builder withResultWording(final String resultWording) {
      this.resultWording = resultWording;
      return this;
    }

    public Builder withRollUpPrompts(final Boolean rollUpPrompts) {
      this.rollUpPrompts = rollUpPrompts;
      return this;
    }

    public Builder withRootJudicialResultId(final UUID rootJudicialResultId) {
      this.rootJudicialResultId = rootJudicialResultId;
      return this;
    }

    public Builder withRootJudicialResultTypeId(final UUID rootJudicialResultTypeId) {
      this.rootJudicialResultTypeId = rootJudicialResultTypeId;
      return this;
    }

    public Builder withSecondaryCJSCodes(final List<SecondaryCJSCode> secondaryCJSCodes) {
      this.secondaryCJSCodes = secondaryCJSCodes;
      return this;
    }

    public Builder withSentToCC(final Boolean sentToCC) {
      this.sentToCC = sentToCC;
      return this;
    }

    public Builder withTerminatesOffenceProceedings(final Boolean terminatesOffenceProceedings) {
      this.terminatesOffenceProceedings = terminatesOffenceProceedings;
      return this;
    }

    public Builder withUrgent(final Boolean urgent) {
      this.urgent = urgent;
      return this;
    }

    public Builder withUsergroups(final List<String> usergroups) {
      this.usergroups = usergroups;
      return this;
    }

    public Builder withWelshLabel(final String welshLabel) {
      this.welshLabel = welshLabel;
      return this;
    }

    public Builder withWelshResultWording(final String welshResultWording) {
      this.welshResultWording = welshResultWording;
      return this;
    }

    public Builder withValuesFrom(final JudicialResult judicialResult) {
      this.alwaysPublished = judicialResult.getAlwaysPublished();
      this.amendmentDate = judicialResult.getAmendmentDate();
      this.amendmentReason = judicialResult.getAmendmentReason();
      this.amendmentReasonId = judicialResult.getAmendmentReasonId();
      this.approvedDate = judicialResult.getApprovedDate();
      this.canBeSubjectOfBreach = judicialResult.getCanBeSubjectOfBreach();
      this.canBeSubjectOfVariation = judicialResult.getCanBeSubjectOfVariation();
      this.canExtendActiveOrder = judicialResult.getCanExtendActiveOrder();
      this.category = judicialResult.getCategory();
      this.cjsCode = judicialResult.getCjsCode();
      this.committedToCC = judicialResult.getCommittedToCC();
      this.courtClerk = judicialResult.getCourtClerk();
      this.d20 = judicialResult.getD20();
      this.delegatedPowers = judicialResult.getDelegatedPowers();
      this.drivingTestStipulation = judicialResult.getDrivingTestStipulation();
      this.durationElement = judicialResult.getDurationElement();
      this.dvlaCode = judicialResult.getDvlaCode();
      this.excludedFromResults = judicialResult.getExcludedFromResults();
      this.fourEyesApproval = judicialResult.getFourEyesApproval();
      this.isAdjournmentResult = judicialResult.getIsAdjournmentResult();
      this.isAvailableForCourtExtract = judicialResult.getIsAvailableForCourtExtract();
      this.isConvictedResult = judicialResult.getIsConvictedResult();
      this.isDeemedServed = judicialResult.getIsDeemedServed();
      this.isDeleted = judicialResult.getIsDeleted();
      this.isFinancialResult = judicialResult.getIsFinancialResult();
      this.isNewAmendment = judicialResult.getIsNewAmendment();
      this.isUnscheduled = judicialResult.getIsUnscheduled();
      this.judicialResultId = judicialResult.getJudicialResultId();
      this.judicialResultPrompts = judicialResult.getJudicialResultPrompts();
      this.judicialResultTypeId = judicialResult.getJudicialResultTypeId();
      this.label = judicialResult.getLabel();
      this.lastSharedDateTime = judicialResult.getLastSharedDateTime();
      this.level = judicialResult.getLevel();
      this.lifeDuration = judicialResult.getLifeDuration();
      this.nextHearing = judicialResult.getNextHearing();
      this.offenceId = judicialResult.getOffenceId();
      this.orderedDate = judicialResult.getOrderedDate();
      this.orderedHearingId = judicialResult.getOrderedHearingId();
      this.parentJudicialResultId = judicialResult.getParentJudicialResultId();
      this.parentJudicialResultTypeId = judicialResult.getParentJudicialResultTypeId();
      this.pointsDisqualificationCode = judicialResult.getPointsDisqualificationCode();
      this.policeSubjectLineTitle = judicialResult.getPoliceSubjectLineTitle();
      this.postHearingCustodyStatus = judicialResult.getPostHearingCustodyStatus();
      this.preserveActiveOrder = judicialResult.getPreserveActiveOrder();
      this.publishedAsAPrompt = judicialResult.getPublishedAsAPrompt();
      this.publishedForNows = judicialResult.getPublishedForNows();
      this.qualifier = judicialResult.getQualifier();
      this.rank = judicialResult.getRank();
      this.resultDefinitionGroup = judicialResult.getResultDefinitionGroup();
      this.resultText = judicialResult.getResultText();
      this.resultWording = judicialResult.getResultWording();
      this.rollUpPrompts = judicialResult.getRollUpPrompts();
      this.rootJudicialResultId = judicialResult.getRootJudicialResultId();
      this.rootJudicialResultTypeId = judicialResult.getRootJudicialResultTypeId();
      this.secondaryCJSCodes = judicialResult.getSecondaryCJSCodes();
      this.sentToCC = judicialResult.getSentToCC();
      this.terminatesOffenceProceedings = judicialResult.getTerminatesOffenceProceedings();
      this.urgent = judicialResult.getUrgent();
      this.usergroups = judicialResult.getUsergroups();
      this.welshLabel = judicialResult.getWelshLabel();
      this.welshResultWording = judicialResult.getWelshResultWording();
      return this;
    }

    public JudicialResult build() {
      return new uk.gov.justice.core.courts.JudicialResult(alwaysPublished, amendmentDate, amendmentReason, amendmentReasonId, approvedDate, canBeSubjectOfBreach, canBeSubjectOfVariation, canExtendActiveOrder, category, cjsCode, committedToCC, courtClerk, d20, delegatedPowers, drivingTestStipulation, durationElement, dvlaCode, excludedFromResults, fourEyesApproval, isAdjournmentResult, isAvailableForCourtExtract, isConvictedResult, isDeemedServed, isDeleted, isFinancialResult, isNewAmendment, isUnscheduled, judicialResultId, judicialResultPrompts, judicialResultTypeId, label, lastSharedDateTime, level, lifeDuration, nextHearing, offenceId, orderedDate, orderedHearingId, parentJudicialResultId, parentJudicialResultTypeId, pointsDisqualificationCode, policeSubjectLineTitle, postHearingCustodyStatus, preserveActiveOrder, publishedAsAPrompt, publishedForNows, qualifier, rank, resultDefinitionGroup, resultText, resultWording, rollUpPrompts, rootJudicialResultId, rootJudicialResultTypeId, secondaryCJSCodes, sentToCC, terminatesOffenceProceedings, urgent, usergroups, welshLabel, welshResultWording);
    }
  }
}

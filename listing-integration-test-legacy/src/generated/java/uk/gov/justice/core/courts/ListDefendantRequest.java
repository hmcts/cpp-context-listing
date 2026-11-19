package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class ListDefendantRequest implements Serializable {
  private static final long serialVersionUID = -2013694293470485741L;

  private final String datesToAvoid;

  private final UUID defendantId;

  private final List<UUID> defendantOffences;

  private final HearingLanguage hearingLanguageNeeds;

  private final UUID prosecutionCaseId;

  private final ReferralReason referralReason;

  private final SummonsApprovedOutcome summonsApprovedOutcome;

  private final SummonsType summonsRequired;

  @JsonCreator
  public ListDefendantRequest(final String datesToAvoid, final UUID defendantId, final List<UUID> defendantOffences, final HearingLanguage hearingLanguageNeeds, final UUID prosecutionCaseId, final ReferralReason referralReason, final SummonsApprovedOutcome summonsApprovedOutcome, final SummonsType summonsRequired) {
    this.datesToAvoid = datesToAvoid;
    this.defendantId = defendantId;
    this.defendantOffences = defendantOffences;
    this.hearingLanguageNeeds = hearingLanguageNeeds;
    this.prosecutionCaseId = prosecutionCaseId;
    this.referralReason = referralReason;
    this.summonsApprovedOutcome = summonsApprovedOutcome;
    this.summonsRequired = summonsRequired;
  }

  public String getDatesToAvoid() {
    return datesToAvoid;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<UUID> getDefendantOffences() {
    return defendantOffences;
  }

  public HearingLanguage getHearingLanguageNeeds() {
    return hearingLanguageNeeds;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public ReferralReason getReferralReason() {
    return referralReason;
  }

  public SummonsApprovedOutcome getSummonsApprovedOutcome() {
    return summonsApprovedOutcome;
  }

  public SummonsType getSummonsRequired() {
    return summonsRequired;
  }

  public static Builder listDefendantRequest() {
    return new uk.gov.justice.core.courts.ListDefendantRequest.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ListDefendantRequest that = (uk.gov.justice.core.courts.ListDefendantRequest) obj;

    return java.util.Objects.equals(this.datesToAvoid, that.datesToAvoid) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.defendantOffences, that.defendantOffences) &&
    java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.referralReason, that.referralReason) &&
    java.util.Objects.equals(this.summonsApprovedOutcome, that.summonsApprovedOutcome) &&
    java.util.Objects.equals(this.summonsRequired, that.summonsRequired);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(datesToAvoid, defendantId, defendantOffences, hearingLanguageNeeds, prosecutionCaseId, referralReason, summonsApprovedOutcome, summonsRequired);}

  public static class Builder {
    private String datesToAvoid;

    private UUID defendantId;

    private List<UUID> defendantOffences;

    private HearingLanguage hearingLanguageNeeds;

    private UUID prosecutionCaseId;

    private ReferralReason referralReason;

    private SummonsApprovedOutcome summonsApprovedOutcome;

    private SummonsType summonsRequired;

    public Builder withDatesToAvoid(final String datesToAvoid) {
      this.datesToAvoid = datesToAvoid;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withDefendantOffences(final List<UUID> defendantOffences) {
      this.defendantOffences = defendantOffences;
      return this;
    }

    public Builder withHearingLanguageNeeds(final HearingLanguage hearingLanguageNeeds) {
      this.hearingLanguageNeeds = hearingLanguageNeeds;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withReferralReason(final ReferralReason referralReason) {
      this.referralReason = referralReason;
      return this;
    }

    public Builder withSummonsApprovedOutcome(final SummonsApprovedOutcome summonsApprovedOutcome) {
      this.summonsApprovedOutcome = summonsApprovedOutcome;
      return this;
    }

    public Builder withSummonsRequired(final SummonsType summonsRequired) {
      this.summonsRequired = summonsRequired;
      return this;
    }

    public Builder withValuesFrom(final ListDefendantRequest listDefendantRequest) {
      this.datesToAvoid = listDefendantRequest.getDatesToAvoid();
      this.defendantId = listDefendantRequest.getDefendantId();
      this.defendantOffences = listDefendantRequest.getDefendantOffences();
      this.hearingLanguageNeeds = listDefendantRequest.getHearingLanguageNeeds();
      this.prosecutionCaseId = listDefendantRequest.getProsecutionCaseId();
      this.referralReason = listDefendantRequest.getReferralReason();
      this.summonsApprovedOutcome = listDefendantRequest.getSummonsApprovedOutcome();
      this.summonsRequired = listDefendantRequest.getSummonsRequired();
      return this;
    }

    public ListDefendantRequest build() {
      return new uk.gov.justice.core.courts.ListDefendantRequest(datesToAvoid, defendantId, defendantOffences, hearingLanguageNeeds, prosecutionCaseId, referralReason, summonsApprovedOutcome, summonsRequired);
    }
  }
}

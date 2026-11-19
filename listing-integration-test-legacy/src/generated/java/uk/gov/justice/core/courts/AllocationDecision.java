package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class AllocationDecision implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String allocationDecisionDate;

  private final CourtIndicatedSentence courtIndicatedSentence;

  private final String motReasonCode;

  private final String motReasonDescription;

  private final UUID motReasonId;

  private final UUID offenceId;

  private final UUID originatingHearingId;

  private final Integer sequenceNumber;

  @JsonCreator
  public AllocationDecision(final String allocationDecisionDate, final CourtIndicatedSentence courtIndicatedSentence, final String motReasonCode, final String motReasonDescription, final UUID motReasonId, final UUID offenceId, final UUID originatingHearingId, final Integer sequenceNumber) {
    this.allocationDecisionDate = allocationDecisionDate;
    this.courtIndicatedSentence = courtIndicatedSentence;
    this.motReasonCode = motReasonCode;
    this.motReasonDescription = motReasonDescription;
    this.motReasonId = motReasonId;
    this.offenceId = offenceId;
    this.originatingHearingId = originatingHearingId;
    this.sequenceNumber = sequenceNumber;
  }

  public String getAllocationDecisionDate() {
    return allocationDecisionDate;
  }

  public CourtIndicatedSentence getCourtIndicatedSentence() {
    return courtIndicatedSentence;
  }

  public String getMotReasonCode() {
    return motReasonCode;
  }

  public String getMotReasonDescription() {
    return motReasonDescription;
  }

  public UUID getMotReasonId() {
    return motReasonId;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public UUID getOriginatingHearingId() {
    return originatingHearingId;
  }

  public Integer getSequenceNumber() {
    return sequenceNumber;
  }

  public static Builder allocationDecision() {
    return new uk.gov.justice.core.courts.AllocationDecision.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.AllocationDecision that = (uk.gov.justice.core.courts.AllocationDecision) obj;

    return java.util.Objects.equals(this.allocationDecisionDate, that.allocationDecisionDate) &&
    java.util.Objects.equals(this.courtIndicatedSentence, that.courtIndicatedSentence) &&
    java.util.Objects.equals(this.motReasonCode, that.motReasonCode) &&
    java.util.Objects.equals(this.motReasonDescription, that.motReasonDescription) &&
    java.util.Objects.equals(this.motReasonId, that.motReasonId) &&
    java.util.Objects.equals(this.offenceId, that.offenceId) &&
    java.util.Objects.equals(this.originatingHearingId, that.originatingHearingId) &&
    java.util.Objects.equals(this.sequenceNumber, that.sequenceNumber);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocationDecisionDate, courtIndicatedSentence, motReasonCode, motReasonDescription, motReasonId, offenceId, originatingHearingId, sequenceNumber);}

  public static class Builder {
    private String allocationDecisionDate;

    private CourtIndicatedSentence courtIndicatedSentence;

    private String motReasonCode;

    private String motReasonDescription;

    private UUID motReasonId;

    private UUID offenceId;

    private UUID originatingHearingId;

    private Integer sequenceNumber;

    public Builder withAllocationDecisionDate(final String allocationDecisionDate) {
      this.allocationDecisionDate = allocationDecisionDate;
      return this;
    }

    public Builder withCourtIndicatedSentence(final CourtIndicatedSentence courtIndicatedSentence) {
      this.courtIndicatedSentence = courtIndicatedSentence;
      return this;
    }

    public Builder withMotReasonCode(final String motReasonCode) {
      this.motReasonCode = motReasonCode;
      return this;
    }

    public Builder withMotReasonDescription(final String motReasonDescription) {
      this.motReasonDescription = motReasonDescription;
      return this;
    }

    public Builder withMotReasonId(final UUID motReasonId) {
      this.motReasonId = motReasonId;
      return this;
    }

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withOriginatingHearingId(final UUID originatingHearingId) {
      this.originatingHearingId = originatingHearingId;
      return this;
    }

    public Builder withSequenceNumber(final Integer sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
      return this;
    }

    public Builder withValuesFrom(final AllocationDecision allocationDecision) {
      this.allocationDecisionDate = allocationDecision.getAllocationDecisionDate();
      this.courtIndicatedSentence = allocationDecision.getCourtIndicatedSentence();
      this.motReasonCode = allocationDecision.getMotReasonCode();
      this.motReasonDescription = allocationDecision.getMotReasonDescription();
      this.motReasonId = allocationDecision.getMotReasonId();
      this.offenceId = allocationDecision.getOffenceId();
      this.originatingHearingId = allocationDecision.getOriginatingHearingId();
      this.sequenceNumber = allocationDecision.getSequenceNumber();
      return this;
    }

    public AllocationDecision build() {
      return new uk.gov.justice.core.courts.AllocationDecision(allocationDecisionDate, courtIndicatedSentence, motReasonCode, motReasonDescription, motReasonId, offenceId, originatingHearingId, sequenceNumber);
    }
  }
}

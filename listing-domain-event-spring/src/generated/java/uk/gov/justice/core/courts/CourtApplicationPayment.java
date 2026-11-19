package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CourtApplicationPayment implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final FeeStatus contestedFeeStatus;

  private final String contestedPaymentReference;

  private final FeeStatus feeStatus;

  private final Boolean isFeeExempt;

  private final Boolean isFeePaid;

  private final Boolean isFeeUndertakingAttached;

  private final String paymentReference;

  @JsonCreator
  public CourtApplicationPayment(final FeeStatus contestedFeeStatus, final String contestedPaymentReference, final FeeStatus feeStatus, final Boolean isFeeExempt, final Boolean isFeePaid, final Boolean isFeeUndertakingAttached, final String paymentReference) {
    this.contestedFeeStatus = contestedFeeStatus;
    this.contestedPaymentReference = contestedPaymentReference;
    this.feeStatus = feeStatus;
    this.isFeeExempt = isFeeExempt;
    this.isFeePaid = isFeePaid;
    this.isFeeUndertakingAttached = isFeeUndertakingAttached;
    this.paymentReference = paymentReference;
  }

  public FeeStatus getContestedFeeStatus() {
    return contestedFeeStatus;
  }

  public String getContestedPaymentReference() {
    return contestedPaymentReference;
  }

  public FeeStatus getFeeStatus() {
    return feeStatus;
  }

  public Boolean getIsFeeExempt() {
    return isFeeExempt;
  }

  public Boolean getIsFeePaid() {
    return isFeePaid;
  }

  public Boolean getIsFeeUndertakingAttached() {
    return isFeeUndertakingAttached;
  }

  public String getPaymentReference() {
    return paymentReference;
  }

  public static Builder courtApplicationPayment() {
    return new uk.gov.justice.core.courts.CourtApplicationPayment.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplicationPayment that = (uk.gov.justice.core.courts.CourtApplicationPayment) obj;

    return java.util.Objects.equals(this.contestedFeeStatus, that.contestedFeeStatus) &&
    java.util.Objects.equals(this.contestedPaymentReference, that.contestedPaymentReference) &&
    java.util.Objects.equals(this.feeStatus, that.feeStatus) &&
    java.util.Objects.equals(this.isFeeExempt, that.isFeeExempt) &&
    java.util.Objects.equals(this.isFeePaid, that.isFeePaid) &&
    java.util.Objects.equals(this.isFeeUndertakingAttached, that.isFeeUndertakingAttached) &&
    java.util.Objects.equals(this.paymentReference, that.paymentReference);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(contestedFeeStatus, contestedPaymentReference, feeStatus, isFeeExempt, isFeePaid, isFeeUndertakingAttached, paymentReference);}

  public static class Builder {
    private FeeStatus contestedFeeStatus;

    private String contestedPaymentReference;

    private FeeStatus feeStatus;

    private Boolean isFeeExempt;

    private Boolean isFeePaid;

    private Boolean isFeeUndertakingAttached;

    private String paymentReference;

    public Builder withContestedFeeStatus(final FeeStatus contestedFeeStatus) {
      this.contestedFeeStatus = contestedFeeStatus;
      return this;
    }

    public Builder withContestedPaymentReference(final String contestedPaymentReference) {
      this.contestedPaymentReference = contestedPaymentReference;
      return this;
    }

    public Builder withFeeStatus(final FeeStatus feeStatus) {
      this.feeStatus = feeStatus;
      return this;
    }

    public Builder withIsFeeExempt(final Boolean isFeeExempt) {
      this.isFeeExempt = isFeeExempt;
      return this;
    }

    public Builder withIsFeePaid(final Boolean isFeePaid) {
      this.isFeePaid = isFeePaid;
      return this;
    }

    public Builder withIsFeeUndertakingAttached(final Boolean isFeeUndertakingAttached) {
      this.isFeeUndertakingAttached = isFeeUndertakingAttached;
      return this;
    }

    public Builder withPaymentReference(final String paymentReference) {
      this.paymentReference = paymentReference;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationPayment courtApplicationPayment) {
      this.contestedFeeStatus = courtApplicationPayment.getContestedFeeStatus();
      this.contestedPaymentReference = courtApplicationPayment.getContestedPaymentReference();
      this.feeStatus = courtApplicationPayment.getFeeStatus();
      this.isFeeExempt = courtApplicationPayment.getIsFeeExempt();
      this.isFeePaid = courtApplicationPayment.getIsFeePaid();
      this.isFeeUndertakingAttached = courtApplicationPayment.getIsFeeUndertakingAttached();
      this.paymentReference = courtApplicationPayment.getPaymentReference();
      return this;
    }

    public CourtApplicationPayment build() {
      return new uk.gov.justice.core.courts.CourtApplicationPayment(contestedFeeStatus, contestedPaymentReference, feeStatus, isFeeExempt, isFeePaid, isFeeUndertakingAttached, paymentReference);
    }
  }
}

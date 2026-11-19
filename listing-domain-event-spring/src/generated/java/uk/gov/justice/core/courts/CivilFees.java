package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CivilFees implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final UUID feeId;

  private final FeeStatus feeStatus;

  private final FeeType feeType;

  private final String paymentReference;

  @JsonCreator
  public CivilFees(final UUID feeId, final FeeStatus feeStatus, final FeeType feeType, final String paymentReference) {
    this.feeId = feeId;
    this.feeStatus = feeStatus;
    this.feeType = feeType;
    this.paymentReference = paymentReference;
  }

  public UUID getFeeId() {
    return feeId;
  }

  public FeeStatus getFeeStatus() {
    return feeStatus;
  }

  public FeeType getFeeType() {
    return feeType;
  }

  public String getPaymentReference() {
    return paymentReference;
  }

  public static Builder civilFees() {
    return new uk.gov.justice.core.courts.CivilFees.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CivilFees that = (uk.gov.justice.core.courts.CivilFees) obj;

    return java.util.Objects.equals(this.feeId, that.feeId) &&
    java.util.Objects.equals(this.feeStatus, that.feeStatus) &&
    java.util.Objects.equals(this.feeType, that.feeType) &&
    java.util.Objects.equals(this.paymentReference, that.paymentReference);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(feeId, feeStatus, feeType, paymentReference);}

  public static class Builder {
    private UUID feeId;

    private FeeStatus feeStatus;

    private FeeType feeType;

    private String paymentReference;

    public Builder withFeeId(final UUID feeId) {
      this.feeId = feeId;
      return this;
    }

    public Builder withFeeStatus(final FeeStatus feeStatus) {
      this.feeStatus = feeStatus;
      return this;
    }

    public Builder withFeeType(final FeeType feeType) {
      this.feeType = feeType;
      return this;
    }

    public Builder withPaymentReference(final String paymentReference) {
      this.paymentReference = paymentReference;
      return this;
    }

    public Builder withValuesFrom(final CivilFees civilFees) {
      this.feeId = civilFees.getFeeId();
      this.feeStatus = civilFees.getFeeStatus();
      this.feeType = civilFees.getFeeType();
      this.paymentReference = civilFees.getPaymentReference();
      return this;
    }

    public CivilFees build() {
      return new uk.gov.justice.core.courts.CivilFees(feeId, feeStatus, feeType, paymentReference);
    }
  }
}

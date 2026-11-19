package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class LaaReference implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String applicationReference;

  private final String effectiveEndDate;

  private final String effectiveStartDate;

  private final String laaContractNumber;

  private final String offenceLevelStatus;

  private final String statusCode;

  private final String statusDate;

  private final String statusDescription;

  private final UUID statusId;

  @JsonCreator
  public LaaReference(final String applicationReference, final String effectiveEndDate, final String effectiveStartDate, final String laaContractNumber, final String offenceLevelStatus, final String statusCode, final String statusDate, final String statusDescription, final UUID statusId) {
    this.applicationReference = applicationReference;
    this.effectiveEndDate = effectiveEndDate;
    this.effectiveStartDate = effectiveStartDate;
    this.laaContractNumber = laaContractNumber;
    this.offenceLevelStatus = offenceLevelStatus;
    this.statusCode = statusCode;
    this.statusDate = statusDate;
    this.statusDescription = statusDescription;
    this.statusId = statusId;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public String getEffectiveEndDate() {
    return effectiveEndDate;
  }

  public String getEffectiveStartDate() {
    return effectiveStartDate;
  }

  public String getLaaContractNumber() {
    return laaContractNumber;
  }

  public String getOffenceLevelStatus() {
    return offenceLevelStatus;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public String getStatusDate() {
    return statusDate;
  }

  public String getStatusDescription() {
    return statusDescription;
  }

  public UUID getStatusId() {
    return statusId;
  }

  public static Builder laaReference() {
    return new uk.gov.justice.core.courts.LaaReference.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.LaaReference that = (uk.gov.justice.core.courts.LaaReference) obj;

    return java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
    java.util.Objects.equals(this.effectiveEndDate, that.effectiveEndDate) &&
    java.util.Objects.equals(this.effectiveStartDate, that.effectiveStartDate) &&
    java.util.Objects.equals(this.laaContractNumber, that.laaContractNumber) &&
    java.util.Objects.equals(this.offenceLevelStatus, that.offenceLevelStatus) &&
    java.util.Objects.equals(this.statusCode, that.statusCode) &&
    java.util.Objects.equals(this.statusDate, that.statusDate) &&
    java.util.Objects.equals(this.statusDescription, that.statusDescription) &&
    java.util.Objects.equals(this.statusId, that.statusId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationReference, effectiveEndDate, effectiveStartDate, laaContractNumber, offenceLevelStatus, statusCode, statusDate, statusDescription, statusId);}

  public static class Builder {
    private String applicationReference;

    private String effectiveEndDate;

    private String effectiveStartDate;

    private String laaContractNumber;

    private String offenceLevelStatus;

    private String statusCode;

    private String statusDate;

    private String statusDescription;

    private UUID statusId;

    public Builder withApplicationReference(final String applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public Builder withEffectiveEndDate(final String effectiveEndDate) {
      this.effectiveEndDate = effectiveEndDate;
      return this;
    }

    public Builder withEffectiveStartDate(final String effectiveStartDate) {
      this.effectiveStartDate = effectiveStartDate;
      return this;
    }

    public Builder withLaaContractNumber(final String laaContractNumber) {
      this.laaContractNumber = laaContractNumber;
      return this;
    }

    public Builder withOffenceLevelStatus(final String offenceLevelStatus) {
      this.offenceLevelStatus = offenceLevelStatus;
      return this;
    }

    public Builder withStatusCode(final String statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public Builder withStatusDate(final String statusDate) {
      this.statusDate = statusDate;
      return this;
    }

    public Builder withStatusDescription(final String statusDescription) {
      this.statusDescription = statusDescription;
      return this;
    }

    public Builder withStatusId(final UUID statusId) {
      this.statusId = statusId;
      return this;
    }

    public Builder withValuesFrom(final LaaReference laaReference) {
      this.applicationReference = laaReference.getApplicationReference();
      this.effectiveEndDate = laaReference.getEffectiveEndDate();
      this.effectiveStartDate = laaReference.getEffectiveStartDate();
      this.laaContractNumber = laaReference.getLaaContractNumber();
      this.offenceLevelStatus = laaReference.getOffenceLevelStatus();
      this.statusCode = laaReference.getStatusCode();
      this.statusDate = laaReference.getStatusDate();
      this.statusDescription = laaReference.getStatusDescription();
      this.statusId = laaReference.getStatusId();
      return this;
    }

    public LaaReference build() {
      return new uk.gov.justice.core.courts.LaaReference(applicationReference, effectiveEndDate, effectiveStartDate, laaContractNumber, offenceLevelStatus, statusCode, statusDate, statusDescription, statusId);
    }
  }
}

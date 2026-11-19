package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class VacatedTrialUpdated implements Serializable {
  private static final long serialVersionUID = 9187708479486303135L;

  private final Boolean allocated;

  private final UUID hearingId;

  private final Boolean isVacated;

  private final UUID vacatedTrialReasonId;

  @JsonCreator
  public VacatedTrialUpdated(final Boolean allocated, final UUID hearingId, final Boolean isVacated, final UUID vacatedTrialReasonId) {
    this.allocated = allocated;
    this.hearingId = hearingId;
    this.isVacated = isVacated;
    this.vacatedTrialReasonId = vacatedTrialReasonId;
  }

  public Boolean getAllocated() {
    return allocated;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Boolean getIsVacated() {
    return isVacated;
  }

  public UUID getVacatedTrialReasonId() {
    return vacatedTrialReasonId;
  }

  public static Builder vacatedTrialUpdated() {
    return new uk.gov.justice.listing.courts.VacatedTrialUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.VacatedTrialUpdated that = (uk.gov.justice.listing.courts.VacatedTrialUpdated) obj;

    return java.util.Objects.equals(this.allocated, that.allocated) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.isVacated, that.isVacated) &&
    java.util.Objects.equals(this.vacatedTrialReasonId, that.vacatedTrialReasonId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocated, hearingId, isVacated, vacatedTrialReasonId);}

  public static class Builder {
    private Boolean allocated;

    private UUID hearingId;

    private Boolean isVacated;

    private UUID vacatedTrialReasonId;

    public Builder withAllocated(final Boolean allocated) {
      this.allocated = allocated;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withIsVacated(final Boolean isVacated) {
      this.isVacated = isVacated;
      return this;
    }

    public Builder withVacatedTrialReasonId(final UUID vacatedTrialReasonId) {
      this.vacatedTrialReasonId = vacatedTrialReasonId;
      return this;
    }

    public Builder withValuesFrom(final VacatedTrialUpdated vacatedTrialUpdated) {
      this.allocated = vacatedTrialUpdated.getAllocated();
      this.hearingId = vacatedTrialUpdated.getHearingId();
      this.isVacated = vacatedTrialUpdated.getIsVacated();
      this.vacatedTrialReasonId = vacatedTrialUpdated.getVacatedTrialReasonId();
      return this;
    }

    public VacatedTrialUpdated build() {
      return new uk.gov.justice.listing.courts.VacatedTrialUpdated(allocated, hearingId, isVacated, vacatedTrialReasonId);
    }
  }
}

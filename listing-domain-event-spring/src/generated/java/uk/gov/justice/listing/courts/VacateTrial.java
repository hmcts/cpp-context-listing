package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class VacateTrial implements Serializable {
  private static final long serialVersionUID = -2641330522603143091L;

  private final UUID hearingId;

  private final UUID vacatedTrialReasonId;

  @JsonCreator
  public VacateTrial(final UUID hearingId, final UUID vacatedTrialReasonId) {
    this.hearingId = hearingId;
    this.vacatedTrialReasonId = vacatedTrialReasonId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getVacatedTrialReasonId() {
    return vacatedTrialReasonId;
  }

  public static Builder vacateTrial() {
    return new uk.gov.justice.listing.courts.VacateTrial.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.VacateTrial that = (uk.gov.justice.listing.courts.VacateTrial) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.vacatedTrialReasonId, that.vacatedTrialReasonId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, vacatedTrialReasonId);}

  public static class Builder {
    private UUID hearingId;

    private UUID vacatedTrialReasonId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withVacatedTrialReasonId(final UUID vacatedTrialReasonId) {
      this.vacatedTrialReasonId = vacatedTrialReasonId;
      return this;
    }

    public Builder withValuesFrom(final VacateTrial vacateTrial) {
      this.hearingId = vacateTrial.getHearingId();
      this.vacatedTrialReasonId = vacateTrial.getVacatedTrialReasonId();
      return this;
    }

    public VacateTrial build() {
      return new uk.gov.justice.listing.courts.VacateTrial(hearingId, vacatedTrialReasonId);
    }
  }
}

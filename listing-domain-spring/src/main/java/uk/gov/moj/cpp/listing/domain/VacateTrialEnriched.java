package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VacateTrialEnriched implements Serializable {
  private static final long serialVersionUID = 128561577783403590L;

  private final UUID hearingId;

  private final UUID vacatedTrialReasonId;

  @JsonCreator
  public VacateTrialEnriched(final UUID hearingId, final UUID vacatedTrialReasonId) {
    this.hearingId = hearingId;
    this.vacatedTrialReasonId = vacatedTrialReasonId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getVacatedTrialReasonId() {
    return vacatedTrialReasonId;
  }

  public static Builder vacateTrialEnriched() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final VacateTrialEnriched that = (VacateTrialEnriched) obj;

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

    public Builder withValuesFrom(final VacateTrialEnriched vacateTrialEnriched) {
      this.hearingId = vacateTrialEnriched.getHearingId();
      this.vacatedTrialReasonId = vacateTrialEnriched.getVacatedTrialReasonId();
      return this;
    }

    public VacateTrialEnriched build() {
      return new VacateTrialEnriched(hearingId, vacatedTrialReasonId);
    }
  }
}

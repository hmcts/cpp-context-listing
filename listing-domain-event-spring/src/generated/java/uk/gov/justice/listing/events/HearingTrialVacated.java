package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class HearingTrialVacated implements Serializable {
  private static final long serialVersionUID = -3306960108182816990L;

  private final UUID hearingId;

  private final UUID vacatedTrialReasonId;

  @JsonCreator
  public HearingTrialVacated(final UUID hearingId, final UUID vacatedTrialReasonId) {
    this.hearingId = hearingId;
    this.vacatedTrialReasonId = vacatedTrialReasonId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getVacatedTrialReasonId() {
    return vacatedTrialReasonId;
  }

  public static Builder hearingTrialVacated() {
    return new uk.gov.justice.listing.events.HearingTrialVacated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingTrialVacated that = (uk.gov.justice.listing.events.HearingTrialVacated) obj;

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

    public Builder withValuesFrom(final HearingTrialVacated hearingTrialVacated) {
      this.hearingId = hearingTrialVacated.getHearingId();
      this.vacatedTrialReasonId = hearingTrialVacated.getVacatedTrialReasonId();
      return this;
    }

    public HearingTrialVacated build() {
      return new uk.gov.justice.listing.events.HearingTrialVacated(hearingId, vacatedTrialReasonId);
    }
  }
}

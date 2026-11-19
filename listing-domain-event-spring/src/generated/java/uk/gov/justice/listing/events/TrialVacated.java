package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.trial-vacated")
public class TrialVacated implements Serializable {
  private static final long serialVersionUID = -2676218729556438670L;

  private final Boolean allocated;

  private final UUID hearingId;

  private final UUID vacatedTrialReasonId;

  @JsonCreator
  public TrialVacated(final Boolean allocated, final UUID hearingId, final UUID vacatedTrialReasonId) {
    this.allocated = allocated;
    this.hearingId = hearingId;
    this.vacatedTrialReasonId = vacatedTrialReasonId;
  }

  public Boolean getAllocated() {
    return allocated;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getVacatedTrialReasonId() {
    return vacatedTrialReasonId;
  }

  public static Builder trialVacated() {
    return new uk.gov.justice.listing.events.TrialVacated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.TrialVacated that = (uk.gov.justice.listing.events.TrialVacated) obj;

    return java.util.Objects.equals(this.allocated, that.allocated) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.vacatedTrialReasonId, that.vacatedTrialReasonId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocated, hearingId, vacatedTrialReasonId);}

  public static class Builder {
    private Boolean allocated;

    private UUID hearingId;

    private UUID vacatedTrialReasonId;

    public Builder withAllocated(final Boolean allocated) {
      this.allocated = allocated;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withVacatedTrialReasonId(final UUID vacatedTrialReasonId) {
      this.vacatedTrialReasonId = vacatedTrialReasonId;
      return this;
    }

    public Builder withValuesFrom(final TrialVacated trialVacated) {
      this.allocated = trialVacated.getAllocated();
      this.hearingId = trialVacated.getHearingId();
      this.vacatedTrialReasonId = trialVacated.getVacatedTrialReasonId();
      return this;
    }

    public TrialVacated build() {
      return new uk.gov.justice.listing.events.TrialVacated(allocated, hearingId, vacatedTrialReasonId);
    }
  }
}

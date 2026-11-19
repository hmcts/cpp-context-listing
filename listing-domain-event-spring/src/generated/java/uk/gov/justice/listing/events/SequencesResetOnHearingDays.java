package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class SequencesResetOnHearingDays implements Serializable {
  private static final long serialVersionUID = 3103048205003737929L;

  private final UUID hearingId;

  @JsonCreator
  public SequencesResetOnHearingDays(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder sequencesResetOnHearingDays() {
    return new uk.gov.justice.listing.events.SequencesResetOnHearingDays.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.SequencesResetOnHearingDays that = (uk.gov.justice.listing.events.SequencesResetOnHearingDays) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId);}

  public static class Builder {
    private UUID hearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final SequencesResetOnHearingDays sequencesResetOnHearingDays) {
      this.hearingId = sequencesResetOnHearingDays.getHearingId();
      return this;
    }

    public SequencesResetOnHearingDays build() {
      return new uk.gov.justice.listing.events.SequencesResetOnHearingDays(hearingId);
    }
  }
}

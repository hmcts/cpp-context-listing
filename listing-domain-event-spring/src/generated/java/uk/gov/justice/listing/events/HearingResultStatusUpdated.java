package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-result-status-updated")
public class HearingResultStatusUpdated implements Serializable {
  private static final long serialVersionUID = 9020253045483487127L;

  private final UUID hearingId;

  @JsonCreator
  public HearingResultStatusUpdated(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingResultStatusUpdated() {
    return new uk.gov.justice.listing.events.HearingResultStatusUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingResultStatusUpdated that = (uk.gov.justice.listing.events.HearingResultStatusUpdated) obj;

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

    public Builder withValuesFrom(final HearingResultStatusUpdated hearingResultStatusUpdated) {
      this.hearingId = hearingResultStatusUpdated.getHearingId();
      return this;
    }

    public HearingResultStatusUpdated build() {
      return new uk.gov.justice.listing.events.HearingResultStatusUpdated(hearingId);
    }
  }
}

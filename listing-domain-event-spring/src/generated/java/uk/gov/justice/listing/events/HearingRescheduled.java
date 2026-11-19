package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-rescheduled")
public class HearingRescheduled implements Serializable {
  private static final long serialVersionUID = 555579255203204214L;

  private final Boolean allocated;

  private final UUID hearingId;

  @JsonCreator
  public HearingRescheduled(final Boolean allocated, final UUID hearingId) {
    this.allocated = allocated;
    this.hearingId = hearingId;
  }

  public Boolean getAllocated() {
    return allocated;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingRescheduled() {
    return new uk.gov.justice.listing.events.HearingRescheduled.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingRescheduled that = (uk.gov.justice.listing.events.HearingRescheduled) obj;

    return java.util.Objects.equals(this.allocated, that.allocated) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocated, hearingId);}

  public static class Builder {
    private Boolean allocated;

    private UUID hearingId;

    public Builder withAllocated(final Boolean allocated) {
      this.allocated = allocated;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final HearingRescheduled hearingRescheduled) {
      this.allocated = hearingRescheduled.getAllocated();
      this.hearingId = hearingRescheduled.getHearingId();
      return this;
    }

    public HearingRescheduled build() {
      return new uk.gov.justice.listing.events.HearingRescheduled(allocated, hearingId);
    }
  }
}

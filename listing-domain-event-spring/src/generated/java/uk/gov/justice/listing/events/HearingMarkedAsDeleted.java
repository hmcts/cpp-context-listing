package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-marked-as-deleted")
public class HearingMarkedAsDeleted implements Serializable {
  private static final long serialVersionUID = 3635797117544827167L;

  private final UUID hearingIdToDelete;

  @JsonCreator
  public HearingMarkedAsDeleted(final UUID hearingIdToDelete) {
    this.hearingIdToDelete = hearingIdToDelete;
  }

  public UUID getHearingIdToDelete() {
    return hearingIdToDelete;
  }

  public static Builder hearingMarkedAsDeleted() {
    return new uk.gov.justice.listing.events.HearingMarkedAsDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingMarkedAsDeleted that = (uk.gov.justice.listing.events.HearingMarkedAsDeleted) obj;

    return java.util.Objects.equals(this.hearingIdToDelete, that.hearingIdToDelete);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIdToDelete);}

  public static class Builder {
    private UUID hearingIdToDelete;

    public Builder withHearingIdToDelete(final UUID hearingIdToDelete) {
      this.hearingIdToDelete = hearingIdToDelete;
      return this;
    }

    public Builder withValuesFrom(final HearingMarkedAsDeleted hearingMarkedAsDeleted) {
      this.hearingIdToDelete = hearingMarkedAsDeleted.getHearingIdToDelete();
      return this;
    }

    public HearingMarkedAsDeleted build() {
      return new uk.gov.justice.listing.events.HearingMarkedAsDeleted(hearingIdToDelete);
    }
  }
}

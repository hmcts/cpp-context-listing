package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.court-room-removed-from-hearing")
public class CourtRoomRemovedFromHearing implements Serializable {
  private static final long serialVersionUID = -1167100763507710048L;

  private final UUID hearingId;

  @JsonCreator
  public CourtRoomRemovedFromHearing(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder courtRoomRemovedFromHearing() {
    return new uk.gov.justice.listing.events.CourtRoomRemovedFromHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtRoomRemovedFromHearing that = (uk.gov.justice.listing.events.CourtRoomRemovedFromHearing) obj;

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

    public Builder withValuesFrom(final CourtRoomRemovedFromHearing courtRoomRemovedFromHearing) {
      this.hearingId = courtRoomRemovedFromHearing.getHearingId();
      return this;
    }

    public CourtRoomRemovedFromHearing build() {
      return new uk.gov.justice.listing.events.CourtRoomRemovedFromHearing(hearingId);
    }
  }
}

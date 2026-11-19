package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.court-room-changed-for-hearing")
public class CourtRoomChangedForHearing implements Serializable {
  private static final long serialVersionUID = 2994855725052046374L;

  private final UUID courtRoomId;

  private final UUID hearingId;

  private final String panel;

  @JsonCreator
  public CourtRoomChangedForHearing(final UUID courtRoomId, final UUID hearingId, final String panel) {
    this.courtRoomId = courtRoomId;
    this.hearingId = hearingId;
    this.panel = panel;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getPanel() {
    return panel;
  }

  public static Builder courtRoomChangedForHearing() {
    return new uk.gov.justice.listing.events.CourtRoomChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtRoomChangedForHearing that = (uk.gov.justice.listing.events.CourtRoomChangedForHearing) obj;

    return java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.panel, that.panel);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtRoomId, hearingId, panel);}

  public static class Builder {
    private UUID courtRoomId;

    private UUID hearingId;

    private String panel;

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withPanel(final String panel) {
      this.panel = panel;
      return this;
    }

    public Builder withValuesFrom(final CourtRoomChangedForHearing courtRoomChangedForHearing) {
      this.courtRoomId = courtRoomChangedForHearing.getCourtRoomId();
      this.hearingId = courtRoomChangedForHearing.getHearingId();
      this.panel = courtRoomChangedForHearing.getPanel();
      return this;
    }

    public CourtRoomChangedForHearing build() {
      return new uk.gov.justice.listing.events.CourtRoomChangedForHearing(courtRoomId, hearingId, panel);
    }
  }
}

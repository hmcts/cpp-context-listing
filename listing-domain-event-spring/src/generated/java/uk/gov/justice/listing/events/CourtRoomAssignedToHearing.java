package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CourtRoomAssignedToHearing implements Serializable {
  private static final long serialVersionUID = -3883354866738460100L;

  private final UUID courtRoomId;

  private final UUID hearingId;

  private final String panel;

  @JsonCreator
  public CourtRoomAssignedToHearing(final UUID courtRoomId, final UUID hearingId, final String panel) {
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

  public static Builder courtRoomAssignedToHearing() {
    return new uk.gov.justice.listing.events.CourtRoomAssignedToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtRoomAssignedToHearing that = (uk.gov.justice.listing.events.CourtRoomAssignedToHearing) obj;

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

    public Builder withValuesFrom(final CourtRoomAssignedToHearing courtRoomAssignedToHearing) {
      this.courtRoomId = courtRoomAssignedToHearing.getCourtRoomId();
      this.hearingId = courtRoomAssignedToHearing.getHearingId();
      this.panel = courtRoomAssignedToHearing.getPanel();
      return this;
    }

    public CourtRoomAssignedToHearing build() {
      return new uk.gov.justice.listing.events.CourtRoomAssignedToHearing(courtRoomId, hearingId, panel);
    }
  }
}

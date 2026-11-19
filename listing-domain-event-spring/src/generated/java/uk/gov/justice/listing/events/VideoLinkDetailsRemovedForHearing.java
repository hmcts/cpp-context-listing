package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.video-link-details-removed-for-hearing")
public class VideoLinkDetailsRemovedForHearing implements Serializable {
  private static final long serialVersionUID = -1321392514532161886L;

  private final UUID hearingId;

  @JsonCreator
  public VideoLinkDetailsRemovedForHearing(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder videoLinkDetailsRemovedForHearing() {
    return new uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing that = (uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing) obj;

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

    public Builder withValuesFrom(final VideoLinkDetailsRemovedForHearing videoLinkDetailsRemovedForHearing) {
      this.hearingId = videoLinkDetailsRemovedForHearing.getHearingId();
      return this;
    }

    public VideoLinkDetailsRemovedForHearing build() {
      return new uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing(hearingId);
    }
  }
}

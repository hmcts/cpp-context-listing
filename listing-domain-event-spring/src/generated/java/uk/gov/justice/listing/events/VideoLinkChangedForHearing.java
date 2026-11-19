package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.video-link-changed-for-hearing")
public class VideoLinkChangedForHearing implements Serializable {
  private static final long serialVersionUID = -5727045585841635461L;

  private final Boolean hasVideoLink;

  private final UUID hearingId;

  @JsonCreator
  public VideoLinkChangedForHearing(final Boolean hasVideoLink, final UUID hearingId) {
    this.hasVideoLink = hasVideoLink;
    this.hearingId = hearingId;
  }

  public Boolean getHasVideoLink() {
    return hasVideoLink;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder videoLinkChangedForHearing() {
    return new uk.gov.justice.listing.events.VideoLinkChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.VideoLinkChangedForHearing that = (uk.gov.justice.listing.events.VideoLinkChangedForHearing) obj;

    return java.util.Objects.equals(this.hasVideoLink, that.hasVideoLink) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hasVideoLink, hearingId);}

  public static class Builder {
    private Boolean hasVideoLink;

    private UUID hearingId;

    public Builder withHasVideoLink(final Boolean hasVideoLink) {
      this.hasVideoLink = hasVideoLink;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final VideoLinkChangedForHearing videoLinkChangedForHearing) {
      this.hasVideoLink = videoLinkChangedForHearing.getHasVideoLink();
      this.hearingId = videoLinkChangedForHearing.getHearingId();
      return this;
    }

    public VideoLinkChangedForHearing build() {
      return new uk.gov.justice.listing.events.VideoLinkChangedForHearing(hasVideoLink, hearingId);
    }
  }
}

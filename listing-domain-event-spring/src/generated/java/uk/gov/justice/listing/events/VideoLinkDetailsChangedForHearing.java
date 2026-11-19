package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.video-link-details-changed-for-hearing")
public class VideoLinkDetailsChangedForHearing implements Serializable {
  private static final long serialVersionUID = -2152980884229897667L;

  private final Boolean hasVideoLink;

  private final UUID hearingId;

  private final String videoLinkDetails;

  @JsonCreator
  public VideoLinkDetailsChangedForHearing(final Boolean hasVideoLink, final UUID hearingId, final String videoLinkDetails) {
    this.hasVideoLink = hasVideoLink;
    this.hearingId = hearingId;
    this.videoLinkDetails = videoLinkDetails;
  }

  public Boolean getHasVideoLink() {
    return hasVideoLink;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getVideoLinkDetails() {
    return videoLinkDetails;
  }

  public static Builder videoLinkDetailsChangedForHearing() {
    return new uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing that = (uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing) obj;

    return java.util.Objects.equals(this.hasVideoLink, that.hasVideoLink) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.videoLinkDetails, that.videoLinkDetails);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hasVideoLink, hearingId, videoLinkDetails);}

  public static class Builder {
    private Boolean hasVideoLink;

    private UUID hearingId;

    private String videoLinkDetails;

    public Builder withHasVideoLink(final Boolean hasVideoLink) {
      this.hasVideoLink = hasVideoLink;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withVideoLinkDetails(final String videoLinkDetails) {
      this.videoLinkDetails = videoLinkDetails;
      return this;
    }

    public Builder withValuesFrom(final VideoLinkDetailsChangedForHearing videoLinkDetailsChangedForHearing) {
      this.hasVideoLink = videoLinkDetailsChangedForHearing.getHasVideoLink();
      this.hearingId = videoLinkDetailsChangedForHearing.getHearingId();
      this.videoLinkDetails = videoLinkDetailsChangedForHearing.getVideoLinkDetails();
      return this;
    }

    public VideoLinkDetailsChangedForHearing build() {
      return new uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing(hasVideoLink, hearingId, videoLinkDetails);
    }
  }
}

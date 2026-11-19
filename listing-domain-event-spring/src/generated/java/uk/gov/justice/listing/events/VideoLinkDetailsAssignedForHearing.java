package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.video-link-details-assigned-for-hearing")
public class VideoLinkDetailsAssignedForHearing implements Serializable {
  private static final long serialVersionUID = -2033215375672306741L;

  private final Boolean hasVideoLink;

  private final UUID hearingId;

  private final String videoLinkDetails;

  @JsonCreator
  public VideoLinkDetailsAssignedForHearing(final Boolean hasVideoLink, final UUID hearingId, final String videoLinkDetails) {
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

  public static Builder videoLinkDetailsAssignedForHearing() {
    return new uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing that = (uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing) obj;

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

    public Builder withValuesFrom(final VideoLinkDetailsAssignedForHearing videoLinkDetailsAssignedForHearing) {
      this.hasVideoLink = videoLinkDetailsAssignedForHearing.getHasVideoLink();
      this.hearingId = videoLinkDetailsAssignedForHearing.getHearingId();
      this.videoLinkDetails = videoLinkDetailsAssignedForHearing.getVideoLinkDetails();
      return this;
    }

    public VideoLinkDetailsAssignedForHearing build() {
      return new uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing(hasVideoLink, hearingId, videoLinkDetails);
    }
  }
}

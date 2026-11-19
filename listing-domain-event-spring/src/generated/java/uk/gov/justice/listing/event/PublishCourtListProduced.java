package uk.gov.justice.listing.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.event.publish-court-list-produced")
public class PublishCourtListProduced implements Serializable {
  private static final long serialVersionUID = -4106170752706864868L;

  private final UUID courtCentreId;

  private final UUID courtListFileId;

  private final String courtListFileName;

  private final ZonedDateTime producedTime;

  private final UUID publishCourtListRequestId;

  private final PublishCourtListType publishCourtListType;

  private final String publishDate;

  private final PublishStatus publishStatus;

  private final Boolean weekCommencing;

  @JsonCreator
  public PublishCourtListProduced(final UUID courtCentreId, final UUID courtListFileId, final String courtListFileName, final ZonedDateTime producedTime, final UUID publishCourtListRequestId, final PublishCourtListType publishCourtListType, final String publishDate, final PublishStatus publishStatus, final Boolean weekCommencing) {
    this.courtCentreId = courtCentreId;
    this.courtListFileId = courtListFileId;
    this.courtListFileName = courtListFileName;
    this.producedTime = producedTime;
    this.publishCourtListRequestId = publishCourtListRequestId;
    this.publishCourtListType = publishCourtListType;
    this.publishDate = publishDate;
    this.publishStatus = publishStatus;
    this.weekCommencing = weekCommencing;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtListFileId() {
    return courtListFileId;
  }

  public String getCourtListFileName() {
    return courtListFileName;
  }

  public ZonedDateTime getProducedTime() {
    return producedTime;
  }

  public UUID getPublishCourtListRequestId() {
    return publishCourtListRequestId;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public String getPublishDate() {
    return publishDate;
  }

  public PublishStatus getPublishStatus() {
    return publishStatus;
  }

  public Boolean getWeekCommencing() {
    return weekCommencing;
  }

  public static Builder publishCourtListProduced() {
    return new uk.gov.justice.listing.event.PublishCourtListProduced.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.event.PublishCourtListProduced that = (uk.gov.justice.listing.event.PublishCourtListProduced) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListFileId, that.courtListFileId) &&
    java.util.Objects.equals(this.courtListFileName, that.courtListFileName) &&
    java.util.Objects.equals(this.producedTime, that.producedTime) &&
    java.util.Objects.equals(this.publishCourtListRequestId, that.publishCourtListRequestId) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.publishDate, that.publishDate) &&
    java.util.Objects.equals(this.publishStatus, that.publishStatus) &&
    java.util.Objects.equals(this.weekCommencing, that.weekCommencing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListFileId, courtListFileName, producedTime, publishCourtListRequestId, publishCourtListType, publishDate, publishStatus, weekCommencing);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtListFileId;

    private String courtListFileName;

    private ZonedDateTime producedTime;

    private UUID publishCourtListRequestId;

    private PublishCourtListType publishCourtListType;

    private String publishDate;

    private PublishStatus publishStatus;

    private Boolean weekCommencing;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtListFileId(final UUID courtListFileId) {
      this.courtListFileId = courtListFileId;
      return this;
    }

    public Builder withCourtListFileName(final String courtListFileName) {
      this.courtListFileName = courtListFileName;
      return this;
    }

    public Builder withProducedTime(final ZonedDateTime producedTime) {
      this.producedTime = producedTime;
      return this;
    }

    public Builder withPublishCourtListRequestId(final UUID publishCourtListRequestId) {
      this.publishCourtListRequestId = publishCourtListRequestId;
      return this;
    }

    public Builder withPublishCourtListType(final PublishCourtListType publishCourtListType) {
      this.publishCourtListType = publishCourtListType;
      return this;
    }

    public Builder withPublishDate(final String publishDate) {
      this.publishDate = publishDate;
      return this;
    }

    public Builder withPublishStatus(final PublishStatus publishStatus) {
      this.publishStatus = publishStatus;
      return this;
    }

    public Builder withWeekCommencing(final Boolean weekCommencing) {
      this.weekCommencing = weekCommencing;
      return this;
    }

    public Builder withValuesFrom(final PublishCourtListProduced publishCourtListProduced) {
      this.courtCentreId = publishCourtListProduced.getCourtCentreId();
      this.courtListFileId = publishCourtListProduced.getCourtListFileId();
      this.courtListFileName = publishCourtListProduced.getCourtListFileName();
      this.producedTime = publishCourtListProduced.getProducedTime();
      this.publishCourtListRequestId = publishCourtListProduced.getPublishCourtListRequestId();
      this.publishCourtListType = publishCourtListProduced.getPublishCourtListType();
      this.publishDate = publishCourtListProduced.getPublishDate();
      this.publishStatus = publishCourtListProduced.getPublishStatus();
      this.weekCommencing = publishCourtListProduced.getWeekCommencing();
      return this;
    }

    public PublishCourtListProduced build() {
      return new uk.gov.justice.listing.event.PublishCourtListProduced(courtCentreId, courtListFileId, courtListFileName, producedTime, publishCourtListRequestId, publishCourtListType, publishDate, publishStatus, weekCommencing);
    }
  }
}

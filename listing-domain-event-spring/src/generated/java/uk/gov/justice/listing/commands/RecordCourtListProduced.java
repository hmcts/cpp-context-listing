package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.UUID;

public class RecordCourtListProduced implements Serializable {
  private static final long serialVersionUID = -8675291505426295901L;

  private final UUID courtCentreId;

  private final UUID courtListFileId;

  private final String courtListFileName;

  private final ZonedDateTime producedTime;

  private final UUID publishCourtListRequestId;

  private final PublishCourtListType publishCourtListType;

  private final String publishDate;

  private final PublishStatus publishStatus;

  @JsonCreator
  public RecordCourtListProduced(final UUID courtCentreId, final UUID courtListFileId, final String courtListFileName, final ZonedDateTime producedTime, final UUID publishCourtListRequestId, final PublishCourtListType publishCourtListType, final String publishDate, final PublishStatus publishStatus) {
    this.courtCentreId = courtCentreId;
    this.courtListFileId = courtListFileId;
    this.courtListFileName = courtListFileName;
    this.producedTime = producedTime;
    this.publishCourtListRequestId = publishCourtListRequestId;
    this.publishCourtListType = publishCourtListType;
    this.publishDate = publishDate;
    this.publishStatus = publishStatus;
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

  public static Builder recordCourtListProduced() {
    return new uk.gov.justice.listing.commands.RecordCourtListProduced.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.RecordCourtListProduced that = (uk.gov.justice.listing.commands.RecordCourtListProduced) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListFileId, that.courtListFileId) &&
    java.util.Objects.equals(this.courtListFileName, that.courtListFileName) &&
    java.util.Objects.equals(this.producedTime, that.producedTime) &&
    java.util.Objects.equals(this.publishCourtListRequestId, that.publishCourtListRequestId) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.publishDate, that.publishDate) &&
    java.util.Objects.equals(this.publishStatus, that.publishStatus);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListFileId, courtListFileName, producedTime, publishCourtListRequestId, publishCourtListType, publishDate, publishStatus);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtListFileId;

    private String courtListFileName;

    private ZonedDateTime producedTime;

    private UUID publishCourtListRequestId;

    private PublishCourtListType publishCourtListType;

    private String publishDate;

    private PublishStatus publishStatus;

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

    public Builder withValuesFrom(final RecordCourtListProduced recordCourtListProduced) {
      this.courtCentreId = recordCourtListProduced.getCourtCentreId();
      this.courtListFileId = recordCourtListProduced.getCourtListFileId();
      this.courtListFileName = recordCourtListProduced.getCourtListFileName();
      this.producedTime = recordCourtListProduced.getProducedTime();
      this.publishCourtListRequestId = recordCourtListProduced.getPublishCourtListRequestId();
      this.publishCourtListType = recordCourtListProduced.getPublishCourtListType();
      this.publishDate = recordCourtListProduced.getPublishDate();
      this.publishStatus = recordCourtListProduced.getPublishStatus();
      return this;
    }

    public RecordCourtListProduced build() {
      return new uk.gov.justice.listing.commands.RecordCourtListProduced(courtCentreId, courtListFileId, courtListFileName, producedTime, publishCourtListRequestId, publishCourtListType, publishDate, publishStatus);
    }
  }
}

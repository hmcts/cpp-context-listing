package uk.gov.justice.listing.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishCourtListRequested implements Serializable {
  private static final long serialVersionUID = 733483537521242322L;

  private final UUID courtCentreId;

  private final String endDate;

  private final UUID publishCourtListRequestId;

  private final PublishCourtListType publishCourtListType;

  private final PublishStatus publishStatus;

  private final ZonedDateTime requestedTime;

  private final Boolean sendNotificationToParties;

  private final String startDate;

  private final Boolean weekCommencing;

  @JsonCreator
  public PublishCourtListRequested(final UUID courtCentreId, final String endDate, final UUID publishCourtListRequestId, final PublishCourtListType publishCourtListType, final PublishStatus publishStatus, final ZonedDateTime requestedTime, final Boolean sendNotificationToParties, final String startDate, final Boolean weekCommencing) {
    this.courtCentreId = courtCentreId;
    this.endDate = endDate;
    this.publishCourtListRequestId = publishCourtListRequestId;
    this.publishCourtListType = publishCourtListType;
    this.publishStatus = publishStatus;
    this.requestedTime = requestedTime;
    this.sendNotificationToParties = sendNotificationToParties;
    this.startDate = startDate;
    this.weekCommencing = weekCommencing;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public String getEndDate() {
    return endDate;
  }

  public UUID getPublishCourtListRequestId() {
    return publishCourtListRequestId;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public PublishStatus getPublishStatus() {
    return publishStatus;
  }

  public ZonedDateTime getRequestedTime() {
    return requestedTime;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public String getStartDate() {
    return startDate;
  }

  public Boolean getWeekCommencing() {
    return weekCommencing;
  }

  public static Builder publishCourtListRequested() {
    return new uk.gov.justice.listing.event.PublishCourtListRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.event.PublishCourtListRequested that = (uk.gov.justice.listing.event.PublishCourtListRequested) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.publishCourtListRequestId, that.publishCourtListRequestId) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.publishStatus, that.publishStatus) &&
    java.util.Objects.equals(this.requestedTime, that.requestedTime) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.weekCommencing, that.weekCommencing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, endDate, publishCourtListRequestId, publishCourtListType, publishStatus, requestedTime, sendNotificationToParties, startDate, weekCommencing);}

  public static class Builder {
    private UUID courtCentreId;

    private String endDate;

    private UUID publishCourtListRequestId;

    private PublishCourtListType publishCourtListType;

    private PublishStatus publishStatus;

    private ZonedDateTime requestedTime;

    private Boolean sendNotificationToParties;

    private String startDate;

    private Boolean weekCommencing;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
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

    public Builder withPublishStatus(final PublishStatus publishStatus) {
      this.publishStatus = publishStatus;
      return this;
    }

    public Builder withRequestedTime(final ZonedDateTime requestedTime) {
      this.requestedTime = requestedTime;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withWeekCommencing(final Boolean weekCommencing) {
      this.weekCommencing = weekCommencing;
      return this;
    }

    public Builder withValuesFrom(final PublishCourtListRequested publishCourtListRequested) {
      this.courtCentreId = publishCourtListRequested.getCourtCentreId();
      this.endDate = publishCourtListRequested.getEndDate();
      this.publishCourtListRequestId = publishCourtListRequested.getPublishCourtListRequestId();
      this.publishCourtListType = publishCourtListRequested.getPublishCourtListType();
      this.publishStatus = publishCourtListRequested.getPublishStatus();
      this.requestedTime = publishCourtListRequested.getRequestedTime();
      this.sendNotificationToParties = publishCourtListRequested.getSendNotificationToParties();
      this.startDate = publishCourtListRequested.getStartDate();
      this.weekCommencing = publishCourtListRequested.getWeekCommencing();
      return this;
    }

    public PublishCourtListRequested build() {
      return new uk.gov.justice.listing.event.PublishCourtListRequested(courtCentreId, endDate, publishCourtListRequestId, publishCourtListType, publishStatus, requestedTime, sendNotificationToParties, startDate, weekCommencing);
    }
  }
}

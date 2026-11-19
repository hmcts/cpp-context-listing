package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishCourtList implements Serializable {
  private static final long serialVersionUID = -1167182234824350765L;

  private final UUID courtCentreId;

  private final LocalDate endDate;

  private final PublishCourtListType publishCourtListType;

  private final ZonedDateTime requestedTime;

  private final Boolean sendNotificationToParties;

  private final LocalDate startDate;

  @JsonCreator
  public PublishCourtList(final UUID courtCentreId, final LocalDate endDate, final PublishCourtListType publishCourtListType, final ZonedDateTime requestedTime, final Boolean sendNotificationToParties, final LocalDate startDate) {
    this.courtCentreId = courtCentreId;
    this.endDate = endDate;
    this.publishCourtListType = publishCourtListType;
    this.requestedTime = requestedTime;
    this.sendNotificationToParties = sendNotificationToParties;
    this.startDate = startDate;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public ZonedDateTime getRequestedTime() {
    return requestedTime;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public static Builder publishCourtList() {
    return new uk.gov.justice.listing.commands.PublishCourtList.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.PublishCourtList that = (uk.gov.justice.listing.commands.PublishCourtList) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.requestedTime, that.requestedTime) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, endDate, publishCourtListType, requestedTime, sendNotificationToParties, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private LocalDate endDate;

    private PublishCourtListType publishCourtListType;

    private ZonedDateTime requestedTime;

    private Boolean sendNotificationToParties;

    private LocalDate startDate;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withEndDate(final LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withPublishCourtListType(final PublishCourtListType publishCourtListType) {
      this.publishCourtListType = publishCourtListType;
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

    public Builder withStartDate(final LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final PublishCourtList publishCourtList) {
      this.courtCentreId = publishCourtList.getCourtCentreId();
      this.endDate = publishCourtList.getEndDate();
      this.publishCourtListType = publishCourtList.getPublishCourtListType();
      this.requestedTime = publishCourtList.getRequestedTime();
      this.sendNotificationToParties = publishCourtList.getSendNotificationToParties();
      this.startDate = publishCourtList.getStartDate();
      return this;
    }

    public PublishCourtList build() {
      return new uk.gov.justice.listing.commands.PublishCourtList(courtCentreId, endDate, publishCourtListType, requestedTime, sendNotificationToParties, startDate);
    }
  }
}

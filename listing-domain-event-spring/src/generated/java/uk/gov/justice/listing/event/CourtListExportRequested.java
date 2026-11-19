package uk.gov.justice.listing.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CourtListExportRequested implements Serializable {
  private static final long serialVersionUID = -9181608089796795179L;

  private final UUID courtCentreId;

  private final UUID courtListId;

  private final String courtListJson;

  private final LocalDate endDate;

  private final PublishCourtListType publishCourtListType;

  private final ZonedDateTime requestedTime;

  private final Boolean sendNotificationToParties;

  private final LocalDate startDate;

  @JsonCreator
  public CourtListExportRequested(final UUID courtCentreId, final UUID courtListId, final String courtListJson, final LocalDate endDate, final PublishCourtListType publishCourtListType, final ZonedDateTime requestedTime, final Boolean sendNotificationToParties, final LocalDate startDate) {
    this.courtCentreId = courtCentreId;
    this.courtListId = courtListId;
    this.courtListJson = courtListJson;
    this.endDate = endDate;
    this.publishCourtListType = publishCourtListType;
    this.requestedTime = requestedTime;
    this.sendNotificationToParties = sendNotificationToParties;
    this.startDate = startDate;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtListId() {
    return courtListId;
  }

  public String getCourtListJson() {
    return courtListJson;
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

  public static Builder courtListExportRequested() {
    return new uk.gov.justice.listing.event.CourtListExportRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.event.CourtListExportRequested that = (uk.gov.justice.listing.event.CourtListExportRequested) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListId, that.courtListId) &&
    java.util.Objects.equals(this.courtListJson, that.courtListJson) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.requestedTime, that.requestedTime) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListId, courtListJson, endDate, publishCourtListType, requestedTime, sendNotificationToParties, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtListId;

    private String courtListJson;

    private LocalDate endDate;

    private PublishCourtListType publishCourtListType;

    private ZonedDateTime requestedTime;

    private Boolean sendNotificationToParties;

    private LocalDate startDate;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtListId(final UUID courtListId) {
      this.courtListId = courtListId;
      return this;
    }

    public Builder withCourtListJson(final String courtListJson) {
      this.courtListJson = courtListJson;
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

    public Builder withValuesFrom(final CourtListExportRequested courtListExportRequested) {
      this.courtCentreId = courtListExportRequested.getCourtCentreId();
      this.courtListId = courtListExportRequested.getCourtListId();
      this.courtListJson = courtListExportRequested.getCourtListJson();
      this.endDate = courtListExportRequested.getEndDate();
      this.publishCourtListType = courtListExportRequested.getPublishCourtListType();
      this.requestedTime = courtListExportRequested.getRequestedTime();
      this.sendNotificationToParties = courtListExportRequested.getSendNotificationToParties();
      this.startDate = courtListExportRequested.getStartDate();
      return this;
    }

    public CourtListExportRequested build() {
      return new uk.gov.justice.listing.event.CourtListExportRequested(courtCentreId, courtListId, courtListJson, endDate, publishCourtListType, requestedTime, sendNotificationToParties, startDate);
    }
  }
}

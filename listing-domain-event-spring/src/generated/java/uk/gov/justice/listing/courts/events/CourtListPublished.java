package uk.gov.justice.listing.courts.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class CourtListPublished implements Serializable {
  private static final long serialVersionUID = 3881700311939688169L;

  private final UUID courtCentreId;

  private final UUID courtListId;

  private final List<CourtLists> courtLists;

  private final LocalDate endDate;

  private final PublishCourtListType publishCourtListType;

  private final ZonedDateTime requestedTime;

  private final Boolean sendNotificationToParties;

  private final LocalDate startDate;

  private final Boolean weekCommencing;

  @JsonCreator
  public CourtListPublished(final UUID courtCentreId, final UUID courtListId, final List<CourtLists> courtLists, final LocalDate endDate, final PublishCourtListType publishCourtListType, final ZonedDateTime requestedTime, final Boolean sendNotificationToParties, final LocalDate startDate, final Boolean weekCommencing) {
    this.courtCentreId = courtCentreId;
    this.courtListId = courtListId;
    this.courtLists = courtLists;
    this.endDate = endDate;
    this.publishCourtListType = publishCourtListType;
    this.requestedTime = requestedTime;
    this.sendNotificationToParties = sendNotificationToParties;
    this.startDate = startDate;
    this.weekCommencing = weekCommencing;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtListId() {
    return courtListId;
  }

  public List<CourtLists> getCourtLists() {
    return courtLists;
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

  public Boolean getWeekCommencing() {
    return weekCommencing;
  }

  public static Builder courtListPublished() {
    return new uk.gov.justice.listing.courts.events.CourtListPublished.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.events.CourtListPublished that = (uk.gov.justice.listing.courts.events.CourtListPublished) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListId, that.courtListId) &&
    java.util.Objects.equals(this.courtLists, that.courtLists) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.requestedTime, that.requestedTime) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.weekCommencing, that.weekCommencing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListId, courtLists, endDate, publishCourtListType, requestedTime, sendNotificationToParties, startDate, weekCommencing);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtListId;

    private List<CourtLists> courtLists;

    private LocalDate endDate;

    private PublishCourtListType publishCourtListType;

    private ZonedDateTime requestedTime;

    private Boolean sendNotificationToParties;

    private LocalDate startDate;

    private Boolean weekCommencing;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtListId(final UUID courtListId) {
      this.courtListId = courtListId;
      return this;
    }

    public Builder withCourtLists(final List<CourtLists> courtLists) {
      this.courtLists = courtLists;
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

    public Builder withWeekCommencing(final Boolean weekCommencing) {
      this.weekCommencing = weekCommencing;
      return this;
    }

    public Builder withValuesFrom(final CourtListPublished courtListPublished) {
      this.courtCentreId = courtListPublished.getCourtCentreId();
      this.courtListId = courtListPublished.getCourtListId();
      this.courtLists = courtListPublished.getCourtLists();
      this.endDate = courtListPublished.getEndDate();
      this.publishCourtListType = courtListPublished.getPublishCourtListType();
      this.requestedTime = courtListPublished.getRequestedTime();
      this.sendNotificationToParties = courtListPublished.getSendNotificationToParties();
      this.startDate = courtListPublished.getStartDate();
      this.weekCommencing = courtListPublished.getWeekCommencing();
      return this;
    }

    public CourtListPublished build() {
      return new uk.gov.justice.listing.courts.events.CourtListPublished(courtCentreId, courtListId, courtLists, endDate, publishCourtListType, requestedTime, sendNotificationToParties, startDate, weekCommencing);
    }
  }
}

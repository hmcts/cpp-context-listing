package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.UUID;

public class CourtListRequestExport implements Serializable {
  private static final long serialVersionUID = -5366019147330996365L;

  private final UUID courtCentreId;

  private final String courtListJson;

  private final LocalDate endDate;

  private final PublishCourtListType publishCourtListType;

  private final Boolean sendNotificationToParties;

  private final LocalDate startDate;

  @JsonCreator
  public CourtListRequestExport(final UUID courtCentreId, final String courtListJson, final LocalDate endDate, final PublishCourtListType publishCourtListType, final Boolean sendNotificationToParties, final LocalDate startDate) {
    this.courtCentreId = courtCentreId;
    this.courtListJson = courtListJson;
    this.endDate = endDate;
    this.publishCourtListType = publishCourtListType;
    this.sendNotificationToParties = sendNotificationToParties;
    this.startDate = startDate;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
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

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public static Builder courtListRequestExport() {
    return new uk.gov.justice.listing.commands.CourtListRequestExport.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.CourtListRequestExport that = (uk.gov.justice.listing.commands.CourtListRequestExport) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListJson, that.courtListJson) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListJson, endDate, publishCourtListType, sendNotificationToParties, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private String courtListJson;

    private LocalDate endDate;

    private PublishCourtListType publishCourtListType;

    private Boolean sendNotificationToParties;

    private LocalDate startDate;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
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

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withStartDate(final LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final CourtListRequestExport courtListRequestExport) {
      this.courtCentreId = courtListRequestExport.getCourtCentreId();
      this.courtListJson = courtListRequestExport.getCourtListJson();
      this.endDate = courtListRequestExport.getEndDate();
      this.publishCourtListType = courtListRequestExport.getPublishCourtListType();
      this.sendNotificationToParties = courtListRequestExport.getSendNotificationToParties();
      this.startDate = courtListRequestExport.getStartDate();
      return this;
    }

    public CourtListRequestExport build() {
      return new uk.gov.justice.listing.commands.CourtListRequestExport(courtCentreId, courtListJson, endDate, publishCourtListType, sendNotificationToParties, startDate);
    }
  }
}

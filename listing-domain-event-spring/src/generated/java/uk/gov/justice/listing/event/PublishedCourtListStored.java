package uk.gov.justice.listing.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishedCourtListStored implements Serializable {
  private static final long serialVersionUID = -7592548435928981206L;

  private final UUID courtCentreId;

  private final UUID courtListId;

  private final String courtListJson;

  private final ZonedDateTime lastUpdated;

  private final PublishCourtListType publishCourtListType;

  private final LocalDate startDate;

  @JsonCreator
  public PublishedCourtListStored(final UUID courtCentreId, final UUID courtListId, final String courtListJson, final ZonedDateTime lastUpdated, final PublishCourtListType publishCourtListType, final LocalDate startDate) {
    this.courtCentreId = courtCentreId;
    this.courtListId = courtListId;
    this.courtListJson = courtListJson;
    this.lastUpdated = lastUpdated;
    this.publishCourtListType = publishCourtListType;
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

  public ZonedDateTime getLastUpdated() {
    return lastUpdated;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public static Builder publishedCourtListStored() {
    return new uk.gov.justice.listing.event.PublishedCourtListStored.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.event.PublishedCourtListStored that = (uk.gov.justice.listing.event.PublishedCourtListStored) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListId, that.courtListId) &&
    java.util.Objects.equals(this.courtListJson, that.courtListJson) &&
    java.util.Objects.equals(this.lastUpdated, that.lastUpdated) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListId, courtListJson, lastUpdated, publishCourtListType, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtListId;

    private String courtListJson;

    private ZonedDateTime lastUpdated;

    private PublishCourtListType publishCourtListType;

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

    public Builder withLastUpdated(final ZonedDateTime lastUpdated) {
      this.lastUpdated = lastUpdated;
      return this;
    }

    public Builder withPublishCourtListType(final PublishCourtListType publishCourtListType) {
      this.publishCourtListType = publishCourtListType;
      return this;
    }

    public Builder withStartDate(final LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final PublishedCourtListStored publishedCourtListStored) {
      this.courtCentreId = publishedCourtListStored.getCourtCentreId();
      this.courtListId = publishedCourtListStored.getCourtListId();
      this.courtListJson = publishedCourtListStored.getCourtListJson();
      this.lastUpdated = publishedCourtListStored.getLastUpdated();
      this.publishCourtListType = publishedCourtListStored.getPublishCourtListType();
      this.startDate = publishedCourtListStored.getStartDate();
      return this;
    }

    public PublishedCourtListStored build() {
      return new uk.gov.justice.listing.event.PublishedCourtListStored(courtCentreId, courtListId, courtListJson, lastUpdated, publishCourtListType, startDate);
    }
  }
}

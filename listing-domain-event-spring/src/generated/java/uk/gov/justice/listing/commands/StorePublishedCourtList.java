package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.UUID;

public class StorePublishedCourtList implements Serializable {
  private static final long serialVersionUID = 5719028122575050159L;

  private final UUID courtCentreId;

  private final String courtListJson;

  private final UUID publishCourtListRequestId;

  private final PublishCourtListType publishCourtListType;

  private final LocalDate startDate;

  @JsonCreator
  public StorePublishedCourtList(final UUID courtCentreId, final String courtListJson, final UUID publishCourtListRequestId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {
    this.courtCentreId = courtCentreId;
    this.courtListJson = courtListJson;
    this.publishCourtListRequestId = publishCourtListRequestId;
    this.publishCourtListType = publishCourtListType;
    this.startDate = startDate;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public String getCourtListJson() {
    return courtListJson;
  }

  public UUID getPublishCourtListRequestId() {
    return publishCourtListRequestId;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public static Builder storePublishedCourtList() {
    return new uk.gov.justice.listing.commands.StorePublishedCourtList.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.StorePublishedCourtList that = (uk.gov.justice.listing.commands.StorePublishedCourtList) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListJson, that.courtListJson) &&
    java.util.Objects.equals(this.publishCourtListRequestId, that.publishCourtListRequestId) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListJson, publishCourtListRequestId, publishCourtListType, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private String courtListJson;

    private UUID publishCourtListRequestId;

    private PublishCourtListType publishCourtListType;

    private LocalDate startDate;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtListJson(final String courtListJson) {
      this.courtListJson = courtListJson;
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

    public Builder withStartDate(final LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final StorePublishedCourtList storePublishedCourtList) {
      this.courtCentreId = storePublishedCourtList.getCourtCentreId();
      this.courtListJson = storePublishedCourtList.getCourtListJson();
      this.publishCourtListRequestId = storePublishedCourtList.getPublishCourtListRequestId();
      this.publishCourtListType = storePublishedCourtList.getPublishCourtListType();
      this.startDate = storePublishedCourtList.getStartDate();
      return this;
    }

    public StorePublishedCourtList build() {
      return new uk.gov.justice.listing.commands.StorePublishedCourtList(courtCentreId, courtListJson, publishCourtListRequestId, publishCourtListType, startDate);
    }
  }
}

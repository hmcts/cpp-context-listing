package uk.gov.justice.listing.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishCourtListExportSuccessful implements Serializable {
  private static final long serialVersionUID = -7709635005228422299L;

  private final UUID courtCentreId;

  private final String courtListFileName;

  private final UUID courtListId;

  private final LocalDate endDate;

  private final ZonedDateTime exportedTime;

  private final PublishCourtListType publishCourtListType;

  private final LocalDate startDate;

  @JsonCreator
  public PublishCourtListExportSuccessful(final UUID courtCentreId, final String courtListFileName, final UUID courtListId, final LocalDate endDate, final ZonedDateTime exportedTime, final PublishCourtListType publishCourtListType, final LocalDate startDate) {
    this.courtCentreId = courtCentreId;
    this.courtListFileName = courtListFileName;
    this.courtListId = courtListId;
    this.endDate = endDate;
    this.exportedTime = exportedTime;
    this.publishCourtListType = publishCourtListType;
    this.startDate = startDate;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public String getCourtListFileName() {
    return courtListFileName;
  }

  public UUID getCourtListId() {
    return courtListId;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public ZonedDateTime getExportedTime() {
    return exportedTime;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public static Builder publishCourtListExportSuccessful() {
    return new uk.gov.justice.listing.event.PublishCourtListExportSuccessful.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.event.PublishCourtListExportSuccessful that = (uk.gov.justice.listing.event.PublishCourtListExportSuccessful) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListFileName, that.courtListFileName) &&
    java.util.Objects.equals(this.courtListId, that.courtListId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.exportedTime, that.exportedTime) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListFileName, courtListId, endDate, exportedTime, publishCourtListType, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private String courtListFileName;

    private UUID courtListId;

    private LocalDate endDate;

    private ZonedDateTime exportedTime;

    private PublishCourtListType publishCourtListType;

    private LocalDate startDate;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtListFileName(final String courtListFileName) {
      this.courtListFileName = courtListFileName;
      return this;
    }

    public Builder withCourtListId(final UUID courtListId) {
      this.courtListId = courtListId;
      return this;
    }

    public Builder withEndDate(final LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withExportedTime(final ZonedDateTime exportedTime) {
      this.exportedTime = exportedTime;
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

    public Builder withValuesFrom(final PublishCourtListExportSuccessful publishCourtListExportSuccessful) {
      this.courtCentreId = publishCourtListExportSuccessful.getCourtCentreId();
      this.courtListFileName = publishCourtListExportSuccessful.getCourtListFileName();
      this.courtListId = publishCourtListExportSuccessful.getCourtListId();
      this.endDate = publishCourtListExportSuccessful.getEndDate();
      this.exportedTime = publishCourtListExportSuccessful.getExportedTime();
      this.publishCourtListType = publishCourtListExportSuccessful.getPublishCourtListType();
      this.startDate = publishCourtListExportSuccessful.getStartDate();
      return this;
    }

    public PublishCourtListExportSuccessful build() {
      return new uk.gov.justice.listing.event.PublishCourtListExportSuccessful(courtCentreId, courtListFileName, courtListId, endDate, exportedTime, publishCourtListType, startDate);
    }
  }
}

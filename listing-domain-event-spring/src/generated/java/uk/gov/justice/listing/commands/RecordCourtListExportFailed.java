package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class RecordCourtListExportFailed implements Serializable {
  private static final long serialVersionUID = -8827914935964911299L;

  private final UUID courtCentreId;

  private final String courtListFileName;

  private final UUID courtListId;

  private final LocalDate endDate;

  private final String errorMessage;

  private final ZonedDateTime failedTime;

  private final PublishCourtListType publishCourtListType;

  private final LocalDate startDate;

  @JsonCreator
  public RecordCourtListExportFailed(final UUID courtCentreId, final String courtListFileName, final UUID courtListId, final LocalDate endDate, final String errorMessage, final ZonedDateTime failedTime, final PublishCourtListType publishCourtListType, final LocalDate startDate) {
    this.courtCentreId = courtCentreId;
    this.courtListFileName = courtListFileName;
    this.courtListId = courtListId;
    this.endDate = endDate;
    this.errorMessage = errorMessage;
    this.failedTime = failedTime;
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

  public String getErrorMessage() {
    return errorMessage;
  }

  public ZonedDateTime getFailedTime() {
    return failedTime;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public static Builder recordCourtListExportFailed() {
    return new uk.gov.justice.listing.commands.RecordCourtListExportFailed.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.RecordCourtListExportFailed that = (uk.gov.justice.listing.commands.RecordCourtListExportFailed) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListFileName, that.courtListFileName) &&
    java.util.Objects.equals(this.courtListId, that.courtListId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.errorMessage, that.errorMessage) &&
    java.util.Objects.equals(this.failedTime, that.failedTime) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListFileName, courtListId, endDate, errorMessage, failedTime, publishCourtListType, startDate);}

  public static class Builder {
    private UUID courtCentreId;

    private String courtListFileName;

    private UUID courtListId;

    private LocalDate endDate;

    private String errorMessage;

    private ZonedDateTime failedTime;

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

    public Builder withErrorMessage(final String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder withFailedTime(final ZonedDateTime failedTime) {
      this.failedTime = failedTime;
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

    public Builder withValuesFrom(final RecordCourtListExportFailed recordCourtListExportFailed) {
      this.courtCentreId = recordCourtListExportFailed.getCourtCentreId();
      this.courtListFileName = recordCourtListExportFailed.getCourtListFileName();
      this.courtListId = recordCourtListExportFailed.getCourtListId();
      this.endDate = recordCourtListExportFailed.getEndDate();
      this.errorMessage = recordCourtListExportFailed.getErrorMessage();
      this.failedTime = recordCourtListExportFailed.getFailedTime();
      this.publishCourtListType = recordCourtListExportFailed.getPublishCourtListType();
      this.startDate = recordCourtListExportFailed.getStartDate();
      return this;
    }

    public RecordCourtListExportFailed build() {
      return new uk.gov.justice.listing.commands.RecordCourtListExportFailed(courtCentreId, courtListFileName, courtListId, endDate, errorMessage, failedTime, publishCourtListType, startDate);
    }
  }
}

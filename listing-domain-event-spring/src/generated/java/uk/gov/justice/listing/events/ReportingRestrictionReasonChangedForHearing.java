package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class ReportingRestrictionReasonChangedForHearing implements Serializable {
  private static final long serialVersionUID = 3026544467778396182L;

  private final UUID hearingId;

  private final String reportingRestrictionReason;

  @JsonCreator
  public ReportingRestrictionReasonChangedForHearing(final UUID hearingId, final String reportingRestrictionReason) {
    this.hearingId = hearingId;
    this.reportingRestrictionReason = reportingRestrictionReason;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getReportingRestrictionReason() {
    return reportingRestrictionReason;
  }

  public static Builder reportingRestrictionReasonChangedForHearing() {
    return new uk.gov.justice.listing.events.ReportingRestrictionReasonChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ReportingRestrictionReasonChangedForHearing that = (uk.gov.justice.listing.events.ReportingRestrictionReasonChangedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.reportingRestrictionReason, that.reportingRestrictionReason);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, reportingRestrictionReason);}

  public static class Builder {
    private UUID hearingId;

    private String reportingRestrictionReason;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withReportingRestrictionReason(final String reportingRestrictionReason) {
      this.reportingRestrictionReason = reportingRestrictionReason;
      return this;
    }

    public Builder withValuesFrom(final ReportingRestrictionReasonChangedForHearing reportingRestrictionReasonChangedForHearing) {
      this.hearingId = reportingRestrictionReasonChangedForHearing.getHearingId();
      this.reportingRestrictionReason = reportingRestrictionReasonChangedForHearing.getReportingRestrictionReason();
      return this;
    }

    public ReportingRestrictionReasonChangedForHearing build() {
      return new uk.gov.justice.listing.events.ReportingRestrictionReasonChangedForHearing(hearingId, reportingRestrictionReason);
    }
  }
}

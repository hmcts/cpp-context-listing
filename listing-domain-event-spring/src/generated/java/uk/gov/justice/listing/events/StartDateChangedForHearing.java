package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class StartDateChangedForHearing implements Serializable {
  private static final long serialVersionUID = 7140292284738712581L;

  private final UUID hearingId;

  private final String startDate;

  @JsonCreator
  public StartDateChangedForHearing(final UUID hearingId, final String startDate) {
    this.hearingId = hearingId;
    this.startDate = startDate;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getStartDate() {
    return startDate;
  }

  public static Builder startDateChangedForHearing() {
    return new uk.gov.justice.listing.events.StartDateChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.StartDateChangedForHearing that = (uk.gov.justice.listing.events.StartDateChangedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, startDate);}

  public static class Builder {
    private UUID hearingId;

    private String startDate;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final StartDateChangedForHearing startDateChangedForHearing) {
      this.hearingId = startDateChangedForHearing.getHearingId();
      this.startDate = startDateChangedForHearing.getStartDate();
      return this;
    }

    public StartDateChangedForHearing build() {
      return new uk.gov.justice.listing.events.StartDateChangedForHearing(hearingId, startDate);
    }
  }
}

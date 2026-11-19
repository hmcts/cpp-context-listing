package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.end-date-changed-for-hearing")
public class EndDateChangedForHearing implements Serializable {
  private static final long serialVersionUID = -7499523219248298608L;

  private final String endDate;

  private final UUID hearingId;

  @JsonCreator
  public EndDateChangedForHearing(final String endDate, final UUID hearingId) {
    this.endDate = endDate;
    this.hearingId = hearingId;
  }

  public String getEndDate() {
    return endDate;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder endDateChangedForHearing() {
    return new uk.gov.justice.listing.events.EndDateChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.EndDateChangedForHearing that = (uk.gov.justice.listing.events.EndDateChangedForHearing) obj;

    return java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(endDate, hearingId);}

  public static class Builder {
    private String endDate;

    private UUID hearingId;

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final EndDateChangedForHearing endDateChangedForHearing) {
      this.endDate = endDateChangedForHearing.getEndDate();
      this.hearingId = endDateChangedForHearing.getHearingId();
      return this;
    }

    public EndDateChangedForHearing build() {
      return new uk.gov.justice.listing.events.EndDateChangedForHearing(endDate, hearingId);
    }
  }
}

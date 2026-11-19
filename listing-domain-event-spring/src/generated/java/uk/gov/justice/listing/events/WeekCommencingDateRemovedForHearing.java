package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.week-commencing-date-removed-for-hearing")
public class WeekCommencingDateRemovedForHearing implements Serializable {
  private static final long serialVersionUID = -9054059350605781110L;

  private final UUID hearingId;

  @JsonCreator
  public WeekCommencingDateRemovedForHearing(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder weekCommencingDateRemovedForHearing() {
    return new uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing that = (uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId);}

  public static class Builder {
    private UUID hearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final WeekCommencingDateRemovedForHearing weekCommencingDateRemovedForHearing) {
      this.hearingId = weekCommencingDateRemovedForHearing.getHearingId();
      return this;
    }

    public WeekCommencingDateRemovedForHearing build() {
      return new uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing(hearingId);
    }
  }
}

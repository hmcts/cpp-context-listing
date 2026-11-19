package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class EndDateRemovedFromHearing implements Serializable {
  private static final long serialVersionUID = -2222647120851812317L;

  private final UUID hearingId;

  @JsonCreator
  public EndDateRemovedFromHearing(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder endDateRemovedFromHearing() {
    return new uk.gov.justice.listing.events.EndDateRemovedFromHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.EndDateRemovedFromHearing that = (uk.gov.justice.listing.events.EndDateRemovedFromHearing) obj;

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

    public Builder withValuesFrom(final EndDateRemovedFromHearing endDateRemovedFromHearing) {
      this.hearingId = endDateRemovedFromHearing.getHearingId();
      return this;
    }

    public EndDateRemovedFromHearing build() {
      return new uk.gov.justice.listing.events.EndDateRemovedFromHearing(hearingId);
    }
  }
}

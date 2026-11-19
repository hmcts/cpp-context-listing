package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class EndDateRemovedForHearing implements Serializable {
  private static final long serialVersionUID = 1890188570126062872L;

  private final UUID hearingId;

  @JsonCreator
  public EndDateRemovedForHearing(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder endDateRemovedForHearing() {
    return new uk.gov.justice.listing.events.EndDateRemovedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.EndDateRemovedForHearing that = (uk.gov.justice.listing.events.EndDateRemovedForHearing) obj;

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

    public Builder withValuesFrom(final EndDateRemovedForHearing endDateRemovedForHearing) {
      this.hearingId = endDateRemovedForHearing.getHearingId();
      return this;
    }

    public EndDateRemovedForHearing build() {
      return new uk.gov.justice.listing.events.EndDateRemovedForHearing(hearingId);
    }
  }
}
